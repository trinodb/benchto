/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchmark.driver.listeners.benchmark;

import com.teradata.benchmark.driver.execution.BenchmarkExecution;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.execution.QueryExecution;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;
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

    public void reportBenchmarkStarted(BenchmarkExecution benchmarkExecution)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.benchmarkStarted(benchmarkExecution));
        }
    }

    public void reportBenchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.benchmarkFinished(benchmarkExecutionResult));
        }
    }

    public void reportExecutionStarted(QueryExecution queryExecution)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.executionStarted(queryExecution));
        }
    }

    public void reportExecutionFinished(QueryExecutionResult queryExecutionResult)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.executionFinished(queryExecutionResult));
        }
    }
}
