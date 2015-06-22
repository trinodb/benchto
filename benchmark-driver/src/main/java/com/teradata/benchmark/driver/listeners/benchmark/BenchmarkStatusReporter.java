/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchmark.driver.listeners.benchmark;

import com.teradata.benchmark.driver.domain.Benchmark;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

public class BenchmarkStatusReporter
{
    private final TaskExecutor taskExecutor;

    private final List<BenchmarkExecutionListener> executionListeners;

    public BenchmarkStatusReporter(TaskExecutor taskExecutor, List<BenchmarkExecutionListener> executionListeners)
    {
        this.taskExecutor = taskExecutor;
        this.executionListeners = executionListeners;
    }

    public void reportBenchmarkStarted(Benchmark benchmark)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.benchmarkStarted(benchmark));
        }
    }

    public void reportBenchmarkFinished(BenchmarkResult result)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.benchmarkFinished(result));
        }
    }

    public void reportExecutionStarted(QueryExecution queryExecution)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.executionStarted(queryExecution));
        }
    }

    public void reportExecutionFinished(QueryExecutionResult execution)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.executionFinished(execution));
        }
    }
}
