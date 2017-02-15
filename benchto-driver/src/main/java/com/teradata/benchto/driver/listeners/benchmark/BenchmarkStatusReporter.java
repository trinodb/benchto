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
package com.teradata.benchto.driver.listeners.benchmark;

import com.facebook.presto.jdbc.internal.guava.collect.Ordering;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import static com.google.common.collect.Queues.synchronizedQueue;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

@Component
public class BenchmarkStatusReporter
{
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkStatusReporter.class);

    @Autowired
    private List<BenchmarkExecutionListener> executionListeners;

    private Queue<Future<?>> pendingFutures = synchronizedQueue(new ArrayDeque<>());

    @PostConstruct
    public void sortExecutionListeners()
    {
        // HACK: listeners have to be sorted to provide tests determinism
        executionListeners = ImmutableList.copyOf(
                Ordering.<Ordered>from(OrderComparator.INSTANCE::compare)
                        .compound(Ordering.usingToString())
                        .sortedCopy(executionListeners));
    }

    public void processCompletedFutures()
    {
        synchronized (pendingFutures) {
            while (!pendingFutures.isEmpty() && pendingFutures.element().isDone()) {
                Future<?> doneFuture = pendingFutures.remove();
                try {
                    doneFuture.get();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted when retrieving result of an already done future", e);
                }
                catch (ExecutionException | CancellationException e) {
                    throw new RuntimeException("Listener failed with: " + e, e);
                }
            }
        }
    }

    public void awaitAllFutures(long timeout, TimeUnit unit)
    {
        processCompletedFutures();
        List<Future<?>> futures = drainFutures();

        LOG.info("Awaiting completion of {} futures", futures.size());

        Stopwatch stopwatch = Stopwatch.createStarted();
        for (Future<?> future : futures) {
            long remainingNanos = unit.toNanos(timeout) - stopwatch.elapsed(NANOSECONDS);
            remainingNanos = Math.max(remainingNanos, 0); // let Future.get handle timeout

            try {
                future.get(remainingNanos, NANOSECONDS);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted when retrieving result of an already done future", e);
            }
            catch (ExecutionException | TimeoutException | CancellationException e) {
                throw new RuntimeException("Failure when waiting for listener completion: " + e, e);
            }
        }
    }

    private List<Future<?>> drainFutures()
    {
        synchronized (pendingFutures) {
            List<Future<?>> futures = new ArrayList<>(pendingFutures);
            pendingFutures.clear();
            return futures;
        }
    }

    public void reportBenchmarkStarted(Benchmark benchmark)
    {
        fireListeners(BenchmarkExecutionListener::benchmarkStarted, benchmark);
    }

    public void reportBenchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        fireListeners(BenchmarkExecutionListener::benchmarkFinished, benchmarkExecutionResult);
    }

    public void reportExecutionStarted(QueryExecution queryExecution)
    {
        fireListeners(BenchmarkExecutionListener::executionStarted, queryExecution);
    }

    public void reportExecutionFinished(QueryExecutionResult queryExecutionResult)
    {
        fireListeners(BenchmarkExecutionListener::executionFinished, queryExecutionResult);
    }

    private <T> void fireListeners(BiFunction<BenchmarkExecutionListener, T, Future<?>> invoker, T argument)
    {
        List<Future<?>> futures = new ArrayList<>();
        for (BenchmarkExecutionListener listener : executionListeners) {
            futures.add(invoker.apply(listener, argument));
        }
        pendingFutures.addAll(futures);
    }
}
