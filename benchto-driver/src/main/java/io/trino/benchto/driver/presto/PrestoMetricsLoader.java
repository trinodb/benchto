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
package io.trino.benchto.driver.presto;

import io.trino.benchto.driver.Measurable;
import io.trino.benchto.driver.execution.QueryExecutionResult;
import io.trino.benchto.driver.listeners.measurements.PostExecutionMeasurementProvider;
import io.trino.benchto.driver.service.Measurement;
import io.trino.jdbc.QueryStats;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.trino.benchto.driver.service.Measurement.measurement;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.presto", value = "metrics.collection.enabled")
public class PrestoMetricsLoader
        implements PostExecutionMeasurementProvider
{
    @Override
    public CompletableFuture<List<Measurement>> loadMeasurements(Measurable measurable)
    {
        if (measurable instanceof QueryExecutionResult executionResult) {
            if (executionResult.getPrestoQueryStats().isPresent() && !executionResult.getBenchmark().isThroughputTest()) {
                return completedFuture(loadMetrics(executionResult.getPrestoQueryStats().get()));
            }
        }
        return completedFuture(emptyList());
    }

    private List<Measurement> loadMetrics(QueryStats queryStats)
    {
        List<Measurement> measurements = new ArrayList<>();
        measurements.add(measurement("planningTime", "MILLISECONDS", queryStats.getPlanningTimeMillis()));
        measurements.add(measurement("analysisTime", "MILLISECONDS", queryStats.getAnalysisTimeMillis()));
        measurements.add(measurement("totalCpuTime", "MILLISECONDS", queryStats.getCpuTimeMillis()));
        measurements.add(measurement("totalScheduledTime", "MILLISECONDS", queryStats.getWallTimeMillis()));
        measurements.add(measurement("queuedTime", "MILLISECONDS", queryStats.getQueuedTimeMillis()));
        measurements.add(measurement("elapsedTime", "MILLISECONDS", queryStats.getElapsedTimeMillis()));
        measurements.add(measurement("finishingTime", "MILLISECONDS", queryStats.getFinishingTimeMillis()));
        measurements.add(measurement("physicalInputReadTime", "MILLISECONDS", queryStats.getPhysicalInputTimeMillis()));
        measurements.add(measurement("rawInputDataSize", "BYTES", queryStats.getProcessedBytes()));
        measurements.add(measurement("physicalInputDataSize", "BYTES", queryStats.getPhysicalInputBytes()));
        measurements.add(measurement("physicalWrittenDataSize", "BYTES", queryStats.getPhysicalWrittenBytes()));
        measurements.add(measurement("internalNetworkInputDataSize", "BYTES", queryStats.getInternalNetworkInputBytes()));
        measurements.add(measurement("peakTotalMemoryReservation", "BYTES", queryStats.getPeakMemoryBytes()));
        return measurements;
    }
}
