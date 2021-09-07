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
import io.trino.benchto.driver.execution.QueryExecution;
import io.trino.benchto.driver.execution.QueryExecutionResult;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Component
public class LoggingBenchmarkExecutionListener
        implements BenchmarkExecutionListener
{
    private static final Logger LOG = LoggerFactory.getLogger(LoggingBenchmarkExecutionListener.class);

    @Override
    public int getOrder()
    {
        return -100;
    }

    @Override
    public Future<?> benchmarkStarted(Benchmark benchmark)
    {
        LOG.info("Executing benchmark: {}", benchmark.getName());

        return CompletableFuture.completedFuture("");
    }

    @Override
    public Future<?> benchmarkFinished(BenchmarkExecutionResult result)
    {
        LOG.info("Finished benchmark: {}", result.getBenchmark().getName());

        return CompletableFuture.completedFuture("");
    }

    @Override
    public Future<?> executionStarted(QueryExecution execution)
    {
        LOG.info("Query started: {} ({}/{})", execution.getQueryName(), execution.getRun(), execution.getBenchmark().getRuns());

        return CompletableFuture.completedFuture("");
    }

    @Override
    public Future<?> executionFinished(QueryExecutionResult result)
    {
        if (result.isSuccessful()) {
            LOG.info("Query finished: {} ({}/{}), rows count: {}, duration: {}", result.getQueryName(), result.getQueryExecution().getRun(),
                    result.getBenchmark().getRuns(), result.getRowsCount(), result.getQueryDuration());
        }
        else {
            LOG.error("Query failed: {} ({}/{}), execution error: {}", result.getQueryName(), result.getQueryExecution().getRun(),
                    result.getBenchmark().getRuns(), result.getFailureCause().getMessage());
        }

        return CompletableFuture.completedFuture("");
    }

    @Override
    public Future<?> concurrencyTestExecutionFinished(List<QueryExecutionResult> executions)
    {
        LOG.info("Concurrency test queries finished, queries successful {}, queries failed {}",
                executions.stream().filter(QueryExecutionResult::isSuccessful).count(),
                executions.stream().filter(execution -> !execution.isSuccessful()).count());
        return CompletableFuture.completedFuture("");
    }
}
