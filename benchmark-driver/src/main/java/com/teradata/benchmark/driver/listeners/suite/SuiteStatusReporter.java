/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.suite;

import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SuiteStatusReporter
{
    @Autowired
    private List<SuiteExecutionListener> executionListeners;

    @Autowired
    private TaskExecutor taskExecutor;

    public void reportSuiteFinished(List<BenchmarkExecutionResult> benchmarkExecutionResults)
    {
        for (SuiteExecutionListener listener : executionListeners) {
            taskExecutor.execute(() -> listener.suiteFinished(benchmarkExecutionResults));
        }
    }
}
