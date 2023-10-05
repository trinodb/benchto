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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Future;

import static com.google.common.util.concurrent.Futures.immediateVoidFuture;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.profiler", name = "enabled", havingValue = "true")
public class QueryProfilerExecutionListener
        implements BenchmarkExecutionListener
{
    private static final Logger LOG = LoggerFactory.getLogger(QueryProfilerExecutionListener.class);

    @Autowired
    private List<QueryProfiler> profilers;

    @Value("${benchmark.feature.profiler.profiled-coordinator:#{null}}")
    @Nullable
    private String profiledCoordinator;

    @Value("${benchmark.feature.profiler.profiled-worker:#{null}}")
    @Nullable
    private String profiledWorker;

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
        LOG.info("Starting profilers... [benchmark=%s, query=%s]".formatted(execution.getBenchmark().getName(), execution.getQueryName()));
        if (profiledCoordinator != null) {
            for (QueryProfiler profiler : profilers) {
                try {
                    LOG.info("Starting profiler %s for coordinator [benchmark=%s, query=%s]".formatted(profiler.getClass().getSimpleName(), execution.getBenchmark().getName(), execution.getQueryName()));
                    profiler.start(profiledCoordinator, execution.getBenchmark().getName(), execution.getQueryName(), execution.getSequenceId());
                }
                catch (Exception e) {
                    LOG.error("Starting profiler %s for coordinator failed".formatted(profiler.getClass().getSimpleName()), e);
                }
            }
        }
        if (profiledWorker != null) {
            for (QueryProfiler profiler : profilers) {
                try {
                    LOG.info("Starting profiler %s for worker [benchmark=%s, query=%s]".formatted(profiler.getClass().getSimpleName(), execution.getBenchmark().getName(), execution.getQueryName()));
                    profiler.start(profiledWorker, execution.getBenchmark().getName(), execution.getQueryName(), execution.getSequenceId());
                }
                catch (Exception e) {
                    LOG.error("Starting profiler %s for worker failed".formatted(profiler.getClass().getSimpleName()), e);
                }
            }
        }
        return immediateVoidFuture();
    }

    @Override
    public synchronized Future<?> executionFinished(QueryExecutionResult result)
    {
        LOG.info("Stopping profilers... [benchmark=%s, query=%s]".formatted(result.getBenchmark().getName(), result.getQueryName()));
        if (profiledCoordinator != null) {
            for (QueryProfiler profiler : profilers) {
                try {
                    LOG.info("Stopping profiler %s for coordinator [benchmark=%s, query=%s]".formatted(profiler.toString(), result.getQueryExecution().getBenchmark().getName(), result.getQueryExecution().getQueryName()));
                    profiler.stop(profiledCoordinator, result.getBenchmark().getName(), result.getQueryName(), result.getQueryExecution().getSequenceId());
                }
                catch (Exception e) {
                    LOG.error("Stopping profiler %s for coordinator failed".formatted(profiler.toString()), e);
                }
            }
        }
        if (profiledWorker != null) {
            for (QueryProfiler profiler : profilers) {
                try {
                    LOG.info("Stopping profiler %s for worker [benchmark=%s, query=%s]".formatted(profiler.toString(), result.getQueryExecution().getBenchmark().getName(), result.getQueryExecution().getQueryName()));
                    profiler.stop(profiledWorker, result.getBenchmark().getName(), result.getQueryName(), result.getQueryExecution().getSequenceId());
                }
                catch (Exception e) {
                    LOG.error("Stopping profiler %s for worker failed".formatted(profiler.toString()), e);
                }
            }
        }
        return immediateVoidFuture();
    }

    @Override
    public Future<?> concurrencyTestExecutionFinished(List<QueryExecutionResult> executions)
    {
        return immediateVoidFuture();
    }
}
