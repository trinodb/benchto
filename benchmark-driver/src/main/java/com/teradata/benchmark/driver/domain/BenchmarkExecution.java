/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.domain;

import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;

import static com.google.common.base.Preconditions.checkNotNull;

public class BenchmarkExecution
{
    private final Benchmark benchmark;
    private final BenchmarkStatusReporter statusReporter;
    private final int concurrency;
    private final int runs;

    public BenchmarkExecution(Benchmark benchmark, BenchmarkStatusReporter statusReporter, int concurrency, int runs)
    {
        this.benchmark = checkNotNull(benchmark);
        this.statusReporter = checkNotNull(statusReporter);
        this.concurrency = concurrency;
        this.runs = runs;
    }

    public Benchmark getBenchmark()
    {
        return benchmark;
    }

    public BenchmarkStatusReporter getStatusReporter()
    {
        return statusReporter;
    }

    public int getRuns()
    {
        return runs;
    }

    public int getConcurrency()
    {
        return concurrency;
    }
}
