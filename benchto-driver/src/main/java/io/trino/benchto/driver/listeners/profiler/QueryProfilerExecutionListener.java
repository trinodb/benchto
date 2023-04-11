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
package io.trino.benchto.driver.listeners.profiler;

import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.execution.BenchmarkExecutionResult;
import io.trino.benchto.driver.execution.QueryExecution;
import io.trino.benchto.driver.execution.QueryExecutionResult;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;

import static com.google.common.util.concurrent.Futures.immediateVoidFuture;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.profiler", value = "enabled", havingValue = "true")
public class QueryProfilerExecutionListener
        implements BenchmarkExecutionListener
{
    private static final Logger LOG = LoggerFactory.getLogger(QueryProfilerExecutionListener.class);

    @Autowired
    private QueryProfiler profiler;
    @Autowired
    private ProfilerProperties profilerProperties;

    public int getOrder()
    {
        return -100;
    }

    @Override
    public synchronized Future<?> benchmarkStarted(Benchmark benchmark)
    {
        return immediateVoidFuture();
    }

    @Override
    public Future<?> benchmarkFinished(BenchmarkExecutionResult result)
    {
        return immediateVoidFuture();
    }

    @Override
    public synchronized Future<?> executionStarted(QueryExecution execution)
    {
        LOG.info("Starting profiler... [benchmark=%s, query=%s]".formatted(execution.getBenchmark().getName(), execution.getQueryName()));
        if (profilerProperties.getProfiledCoordinator() != null) {
            profiler.start(profilerProperties.getProfiledCoordinator(), execution.getBenchmark().getName(), execution.getQueryName(), execution.getSequenceId());
        }
        if (profilerProperties.getProfiledWorker() != null) {
            profiler.start(profilerProperties.getProfiledWorker(), execution.getBenchmark().getName(), execution.getQueryName(), execution.getSequenceId());
        }
        return immediateVoidFuture();
    }

    @Override
    public synchronized Future<?> executionFinished(QueryExecutionResult result)
    {
        LOG.info("Stopping profiler... [benchmark=%s, query=%s]".formatted(result.getBenchmark().getName(), result.getQueryName()));
        if (profilerProperties.getProfiledCoordinator() != null) {
            profiler.stop(profilerProperties.getProfiledCoordinator(), result.getBenchmark().getName(), result.getQueryName(), result.getQueryExecution().getSequenceId());
        }
        if (profilerProperties.getProfiledWorker() != null) {
            profiler.stop(profilerProperties.getProfiledWorker(), result.getBenchmark().getName(), result.getQueryName(), result.getQueryExecution().getSequenceId());
        }
        return immediateVoidFuture();
    }

    @Override
    public Future<?> concurrencyTestExecutionFinished(List<QueryExecutionResult> executions)
    {
        return immediateVoidFuture();
    }
}
