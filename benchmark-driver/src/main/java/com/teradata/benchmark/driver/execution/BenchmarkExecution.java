/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;

import java.util.List;
import java.util.Map;

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

    public boolean isSerial()
    {
        return !isConcurrent();
    }

    public boolean isConcurrent()
    {
        return concurrency > 1;
    }

    public String getBenchmarkName()
    {
        return benchmark.getName();
    }

    public String getSequenceId()
    {
        return benchmark.getSequenceId();
    }

    public String getDataSource()
    {
        return benchmark.getDataSource();
    }

    public String getEnvironment()
    {
        return benchmark.getEnvironment();
    }

    public List<Query> getQueries()
    {
        return benchmark.getQueries();
    }

    public Map<String, String> getVariables()
    {
        return benchmark.getVariables();
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
