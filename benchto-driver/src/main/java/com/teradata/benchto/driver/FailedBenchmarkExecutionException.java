/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver;

import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;

import java.util.List;

public class FailedBenchmarkExecutionException
        extends BenchmarkExecutionException
{

    private final List<BenchmarkExecutionResult> failedBenchmarkResults;
    private final int benchmarksCount;

    public FailedBenchmarkExecutionException(List<BenchmarkExecutionResult> failedBenchmarkResults, int benchmarksCount)
    {
        super("" + failedBenchmarkResults.size() + " benchmarks failed");
        this.failedBenchmarkResults = failedBenchmarkResults;
        this.benchmarksCount = benchmarksCount;
    }

    public List<BenchmarkExecutionResult> getFailedBenchmarkResults()
    {
        return failedBenchmarkResults;
    }

    public int getBenchmarksCount()
    {
        return benchmarksCount;
    }
}
