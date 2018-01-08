/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestodb.benchto.driver.graphite;

import io.prestodb.benchto.driver.Measurable;
import io.prestodb.benchto.driver.execution.BenchmarkExecutionResult;
import io.prestodb.benchto.driver.execution.ExecutionSynchronizer;
import io.prestodb.benchto.driver.execution.QueryExecutionResult;
import io.prestodb.benchto.driver.listeners.measurements.PostExecutionMeasurementProvider;
import io.prestodb.benchto.driver.service.Measurement;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Service
@ConditionalOnProperty(prefix = "benchmark.feature.graphite", value = "metrics.collection.enabled")
public class GraphiteMetricsLoader
        implements PostExecutionMeasurementProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(GraphiteMetricsLoader.class);

    @Autowired
    private GraphiteClient graphiteClient;

    @Autowired
    private GraphiteProperties graphiteProperties;

    @Autowired
    private ExecutionSynchronizer executionSynchronizer;

    private Map<String, String> queryMetrics;

    @PostConstruct
    public void initQueryMetrics()
    {
        queryMetrics = newHashMap();
        graphiteProperties.getCpuGraphiteExpr().ifPresent(value -> queryMetrics.put("cpu", value));
        graphiteProperties.getMemoryGraphiteExpr().ifPresent(value -> queryMetrics.put("memory", value));
        graphiteProperties.getNetworkGraphiteExpr().ifPresent(value -> queryMetrics.put("network", value));
        graphiteProperties.getNetworkGraphiteExpr().ifPresent(value -> queryMetrics.put("network_total", format("integral(%s)", value)));

        checkState(!queryMetrics.isEmpty(), "No graphite metrics (graphite.metrics.*) provided for measurement collection");
    }

    @Override
    public CompletableFuture<List<Measurement>> loadMeasurements(Measurable measurable)
    {
        if (!shouldLoadGraphiteMetrics(measurable)) {
            return completedFuture(emptyList());
        }

        /*
         * Graphite can be queried with seconds resolution. It's `from` is exclusive (sic!, at least currently) but `until` is inclusive.
         * Subtracting graphite resolution from `until` we effectively ask for the buckets that are fully covered by the `measurable`.
         * This ignores first and last bucket (both partially covered), but gives most interesting statistics.
         */
        long fromEpochSecond = measurable.getUtcStart().toEpochSecond();
        ZonedDateTime to = measurable.getUtcEnd()
                .minus(graphiteProperties.getGraphiteResolutionSeconds(), ChronoUnit.SECONDS);
        long toEpochSecond = to.toEpochSecond();

        if (fromEpochSecond >= toEpochSecond) {
            // Empty range
            return completedFuture(emptyList());
        }

        return executionSynchronizer.execute(
                to.plus(graphiteProperties.getGraphiteMetricsDelay()).toInstant(),
                () -> doLoadMeasurements(fromEpochSecond, toEpochSecond));
    }

    private List<Measurement> doLoadMeasurements(long fromEpochSecond, long toEpochSecond)
    {
        LOG.debug("Loading metrics {} - from: {}, to: {}", queryMetrics, fromEpochSecond, toEpochSecond);

        Map<String, double[]> loadedMetrics = graphiteClient.loadMetrics(queryMetrics, fromEpochSecond, toEpochSecond);

        List<Measurement> measurements = newArrayList();

        if (graphiteProperties.getCpuGraphiteExpr().isPresent() && loadedMetrics.containsKey("cpu")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "cpu", "PERCENT");
        }

        if (graphiteProperties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("memory")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "memory", "PERCENT");
        }

        if (graphiteProperties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("network")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "network", "BYTES");
        }

        if (graphiteProperties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("network_total")) {
            double[] metricValues = loadedMetrics.get("network_total");
            if (metricValues.length > 0) {
                // last non zero measurement contains total over time
                double totalBytes = getLastValueGreaterThanZero(metricValues);
                measurements.add(Measurement.measurement("cluster-network_total", "BYTES", totalBytes));
            }
        }
        return measurements;
    }

    private boolean shouldLoadGraphiteMetrics(Measurable measurable)
    {
        if (!measurable.isSuccessful()) {
            return false;
        }
        if (measurable instanceof QueryExecutionResult && measurable.getBenchmark().isSerial()) {
            return true;
        }
        else if (measurable instanceof BenchmarkExecutionResult && measurable.getBenchmark().isConcurrent()) {
            return true;
        }
        return false;
    }

    private void addMeanMaxMeasurements(Map<String, double[]> loadedMetrics, List<Measurement> measurements, String metricName, String unit)
    {
        Optional<StatisticalSummary> statistics = getStats(loadedMetrics, metricName);
        if (statistics.isPresent()) {
            measurements.add(Measurement.measurement("cluster-" + metricName + "_max", unit, statistics.get().getMax()));
            measurements.add(Measurement.measurement("cluster-" + metricName + "_mean", unit, statistics.get().getMean()));
        }
    }

    private Optional<StatisticalSummary> getStats(Map<String, double[]> loadedMetrics, String metricName)
    {
        double[] metricValues = loadedMetrics.get(metricName);
        if (metricValues.length >= 2) {
            return Optional.of(new DescriptiveStatistics(metricValues));
        }
        else if (metricValues.length == 1) {
            double value = metricValues[0];
            return Optional.of(new StatisticalSummaryValues(value, 0, 1, value, value, value));
        }
        else {
            return Optional.empty();
        }
    }

    private double getLastValueGreaterThanZero(double[] metricValues)
    {
        for (int i = metricValues.length - 1; i >= 0; --i) {
            if (metricValues[i] > 0.0) {
                return metricValues[i];
            }
        }
        return 0;
    }
}
