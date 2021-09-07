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
package io.trino.benchto.driver.listeners;

import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.execution.BenchmarkExecutionResult;
import io.trino.benchto.driver.execution.ExecutionSynchronizer;
import io.trino.benchto.driver.execution.QueryExecution;
import io.trino.benchto.driver.execution.QueryExecutionResult;
import io.trino.benchto.driver.graphite.GraphiteClient;
import io.trino.benchto.driver.graphite.GraphiteClient.GraphiteEventRequest;
import io.trino.benchto.driver.graphite.GraphiteClient.GraphiteEventRequest.GraphiteEventRequestBuilder;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static java.lang.String.format;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.graphite", value = "event.reporting.enabled")
public class GraphiteEventExecutionListener
        implements BenchmarkExecutionListener
{
    @Autowired
    private AsyncTaskExecutor taskExecutor;

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
    public Future<?> benchmarkStarted(Benchmark benchmark)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s started", benchmark.getUniqueName()))
                .tags("benchmark", "started", benchmark.getEnvironment())
                .build();

        return taskExecutor.submit(() -> graphiteClient.storeEvent(request));
    }

    @Override
    public Future<?> benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s ended", benchmarkExecutionResult.getBenchmark().getUniqueName()))
                .tags("benchmark", "ended", benchmarkExecutionResult.getEnvironment())
                .data(format("successful %b", benchmarkExecutionResult.isSuccessful()))
                .when(benchmarkExecutionResult.getUtcEnd())
                .build();

        Future<?> future = taskExecutor.submit(() -> graphiteClient.storeEvent(request));

        executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmarkExecutionResult.getBenchmark());

        return future;
    }

    @Override
    public Future<?> executionStarted(QueryExecution execution)
    {
        if (execution.getBenchmark().isConcurrent()) {
            return CompletableFuture.completedFuture("");
        }

        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, query %s (%d) started", execution.getBenchmark().getUniqueName(), execution.getQueryName(), execution.getRun()))
                .tags("execution", "started", execution.getBenchmark().getEnvironment())
                .build();

        return taskExecutor.submit(() -> graphiteClient.storeEvent(request));
    }

    @Override
    public Future<?> executionFinished(QueryExecutionResult executionResult)
    {
        if (executionResult.getBenchmark().isConcurrent()) {
            return CompletableFuture.completedFuture("");
        }

        QueryExecution queryExecution = executionResult.getQueryExecution();
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, query %s (%d) ended", queryExecution.getBenchmark().getUniqueName(), executionResult.getQueryName(), queryExecution.getRun()))
                .tags("execution", "ended", executionResult.getEnvironment())
                .data(format("duration: %d ms", executionResult.getQueryDuration().toMillis()))
                .when(executionResult.getUtcEnd())
                .build();

        Future<?> future = taskExecutor.submit(() -> graphiteClient.storeEvent(request));

        executionSynchronizer.awaitAfterQueryExecutionAndBeforeResultReport(executionResult);

        return future;
    }

    @Override
    public Future<?> concurrencyTestExecutionFinished(List<QueryExecutionResult> executions)
    {
        return CompletableFuture.completedFuture("");
    }
}
