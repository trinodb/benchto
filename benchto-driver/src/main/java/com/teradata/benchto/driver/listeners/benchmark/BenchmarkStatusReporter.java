/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.listeners.benchmark;

import com.facebook.presto.jdbc.internal.guava.collect.Ordering;
import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;

@Component
public class BenchmarkStatusReporter
{
    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private List<BenchmarkExecutionListener> executionListeners;

    @PostConstruct
    public void sortExecutionListeners()
    {
        // HACK: listeners have to be sorted to provide tests determinism
        executionListeners = ImmutableList.copyOf(Ordering.usingToString().sortedCopy(executionListeners));
    }

    public void reportBenchmarkStarted(Benchmark benchmark)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.benchmarkStarted(benchmark));
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
