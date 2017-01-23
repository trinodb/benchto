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
package com.teradata.benchto.driver.listeners;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.ExecutionSynchronizer;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import com.teradata.benchto.driver.graphite.GraphiteClient;
import com.teradata.benchto.driver.graphite.GraphiteClient.GraphiteEventRequest;
import com.teradata.benchto.driver.graphite.GraphiteClient.GraphiteEventRequest.GraphiteEventRequestBuilder;
import com.teradata.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.graphite", value = "event.reporting.enabled")
public class GraphiteEventExecutionListener
        implements BenchmarkExecutionListener
{
    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private ExecutionSynchronizer executionSynchronizer;

    @Autowired
    private GraphiteClient graphiteClient;

    @Override
    public int getOrder()
    {
        return 0;
    }

    @Override
    public void benchmarkStarted(Benchmark benchmark)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s started", benchmark.getUniqueName()))
                .tags("benchmark", "started", benchmark.getEnvironment())
                .build();

        taskExecutor.execute(() -> graphiteClient.storeEvent(request));
    }

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s ended", benchmarkExecutionResult.getBenchmark().getUniqueName()))
                .tags("benchmark", "ended", benchmarkExecutionResult.getEnvironment())
                .data(format("successful %b", benchmarkExecutionResult.isSuccessful()))
                .when(benchmarkExecutionResult.getUtcEnd())
                .build();

        taskExecutor.execute(() -> graphiteClient.storeEvent(request));

        executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmarkExecutionResult.getBenchmark());
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        if (execution.getBenchmark().isConcurrent()) {
            return;
        }

        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, query %s (%d) started", execution.getBenchmark().getUniqueName(), execution.getQueryName(), execution.getRun()))
                .tags("execution", "started", execution.getBenchmark().getEnvironment())
                .build();

        taskExecutor.execute(() -> graphiteClient.storeEvent(request));
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {
        if (executionResult.getBenchmark().isConcurrent()) {
            return;
        }

        QueryExecution queryExecution = executionResult.getQueryExecution();
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, query %s (%d) ended", queryExecution.getBenchmark().getUniqueName(), executionResult.getQueryName(), queryExecution.getRun()))
                .tags("execution", "ended", executionResult.getEnvironment())
                .data(format("duration: %d ms", executionResult.getQueryDuration().toMillis()))
                .when(executionResult.getUtcEnd())
                .build();

        taskExecutor.execute(() -> graphiteClient.storeEvent(request));

        executionSynchronizer.awaitAfterQueryExecutionAndBeforeResultReport(executionResult);
    }
}
