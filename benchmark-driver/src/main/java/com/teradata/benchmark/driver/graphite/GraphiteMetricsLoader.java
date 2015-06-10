/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.graphite;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.service.Measurement;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.teradata.benchmark.driver.service.Measurement.measurement;
import static java.lang.String.format;

@Service
public class GraphiteMetricsLoader
{
    private static final Logger LOG = LoggerFactory.getLogger(GraphiteMetricsLoader.class);

    @Autowired
    private GraphiteClient graphiteClient;

    @Autowired
    private BenchmarkProperties properties;

    private Map<String, String> queryMetrics;

    @PostConstruct
    public void initQueryMetrics()
    {
        queryMetrics = newHashMap();
        properties.getCpuGraphiteExpr().ifPresent(value -> queryMetrics.put("cpu", value));
        properties.getMemoryGraphiteExpr().ifPresent(value -> queryMetrics.put("memory", value));
        properties.getNetworkGraphiteExpr().ifPresent(value -> queryMetrics.put("network", value));
        properties.getNetworkGraphiteExpr().ifPresent(value -> queryMetrics.put("network_total", format("integral(%s)", value)));
    }

    public List<Measurement> loadMetrics(ZonedDateTime from, ZonedDateTime to)
    {
        if (queryMetrics.isEmpty()) {
            return ImmutableList.of();
        }

        // TODO: ugly hack - we need to fix this
        if (Duration.between(from, to).toMinutes() < 5) {
            from = from.minusMinutes(5);
        }

        LOG.debug("Loading metrics {} - from: {}, to: {}", queryMetrics, from, to);

        Map<String, double[]> loadedMetrics = graphiteClient.loadMetrics(queryMetrics, from, to);

        List<Measurement> measurements = newArrayList();

        if (properties.getCpuGraphiteExpr().isPresent() && loadedMetrics.containsKey("cpu")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "cpu", "PERCENT");
        }

        if (properties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("memory")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "memory", "BYTES");
        }

        if (properties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("network")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "network", "BYTES");
        }

        if (properties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("network_total")) {
            double[] metricValues = loadedMetrics.get("network_total");
            if (metricValues.length > 0) {
                // last measurement contains total over time
                double totalBytes = metricValues[metricValues.length - 1];
                measurements.add(measurement("network_total", "BYTES", totalBytes));
            }
        }

        return measurements;
    }

    private void addMeanMaxMeasurements(Map<String, double[]> loadedMetrics, List<Measurement> measurements, String metricName, String unit)
    {
        Optional<StatisticalSummary> statistics = getStats(loadedMetrics, metricName);
        if (statistics.isPresent()) {
            measurements.add(measurement(metricName + "_max", unit, statistics.get().getMax()));
            measurements.add(measurement(metricName + "_mean", unit, statistics.get().getMean()));
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
}
