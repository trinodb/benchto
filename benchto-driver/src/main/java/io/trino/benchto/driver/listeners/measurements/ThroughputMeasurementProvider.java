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
package io.trino.benchto.driver.listeners.measurements;

import com.google.common.collect.ImmutableList;
import io.trino.benchto.driver.Measurable;
import io.trino.benchto.driver.execution.BenchmarkExecutionResult;
import io.trino.benchto.driver.service.Measurement;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Component
public class ThroughputMeasurementProvider
        implements PostExecutionMeasurementProvider
{
    @Override
    public CompletableFuture<List<Measurement>> loadMeasurements(Measurable measurable)
    {
        List<Measurement> measurements;
        if (measurable instanceof BenchmarkExecutionResult && measurable.getBenchmark().isConcurrent() && measurable.isSuccessful()) {
            measurements = ImmutableList.of(Measurement.measurement("throughput", "QUERY_PER_SECOND", calculateThroughput((BenchmarkExecutionResult) measurable)));
        }
        else {
            measurements = emptyList();
        }

        return completedFuture(measurements);
    }

    private double calculateThroughput(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        long durationInMillis = benchmarkExecutionResult.getQueryDuration().toMillis();
        return (double) benchmarkExecutionResult.getExecutions().size() / durationInMillis * 1000;
    }
}
