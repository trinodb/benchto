/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver;

import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;

import java.util.List;

public class FailedBenchmarkExecutionException
        extends BenchmarkExecutionException
{

    private final List<BenchmarkExecutionResult> failedBenchmarkResults;

    public FailedBenchmarkExecutionException(List<BenchmarkExecutionResult> failedBenchmarkResults)
    {
        super("" + failedBenchmarkResults.size() + " benchmarks failed");
        this.failedBenchmarkResults = failedBenchmarkResults;
    }

    public List<BenchmarkExecutionResult> getFailedBenchmarkResults()
    {
        return failedBenchmarkResults;
    }
}
