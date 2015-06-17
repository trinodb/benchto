/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchmark.driver.listeners;

import com.google.common.collect.Ordering;
import com.teradata.benchmark.driver.domain.Benchmark;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;

@Component
public class BenchmarkStatusReporter
{

    @Autowired
    private List<BenchmarkExecutionListener> executionListeners;

    @Autowired
    private StatusReportSynchronizer statusReportSynchronizer;

    @Qualifier("defaultTaskExecutor")
    @Autowired
    private TaskExecutor taskExecutor;

    @PostConstruct
    private void HACK_sortListenersToGuaranteeDeterminismInTests()
    {
        executionListeners = Ordering
                .natural()
                .usingToString()
                .sortedCopy(executionListeners);
    }

    public void reportBenchmarkStarted(Benchmark benchmark)
    {
        statusReportSynchronizer.synchronizeBenchmarkStart(benchmark);
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.benchmarkStarted(benchmark));
        }
    }

    public void reportBenchmarkFinished(BenchmarkResult result)
    {
        statusReportSynchronizer.synchronizeBenchmarkFinish(result);
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.benchmarkFinished(result));
        }
    }

    public void reportExecutionStarted(QueryExecution queryExecution)
    {
        statusReportSynchronizer.synchronizeExecutionStart(queryExecution);
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.executionStarted(queryExecution));
        }
    }

    public void reportExecutionFinished(QueryExecutionResult execution)
    {
        statusReportSynchronizer.synchronizeExecutionFinish(execution);
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.executionFinished(execution));
        }
    }

    public void reportSuiteFinished(List<BenchmarkResult> benchmarkResults)
    {
        statusReportSynchronizer.synchronizeSuiteFinish(benchmarkResults);
        for (BenchmarkExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.suiteFinished(benchmarkResults));
        }
    }
}
