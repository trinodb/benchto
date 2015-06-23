/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.domain;

import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;

import static com.google.common.base.MoreObjects.toStringHelper;

public class QueryExecution
{
    private final Benchmark benchmark;
    private final Query query;
    private final int run;
    private final BenchmarkStatusReporter statusReporter;

    public QueryExecution(Benchmark benchmark, Query query, int run, BenchmarkStatusReporter statusReporter)
    {
        this.benchmark = benchmark;
        this.query = query;
        this.run = run;
        this.statusReporter = statusReporter;
    }

    public Benchmark getBenchmark()
    {
        return benchmark;
    }

    public Query getQuery()
    {
        return query;
    }

    public int getRun()
    {
        return run;
    }

    public BenchmarkStatusReporter getStatusReporter()
    {
        return statusReporter;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("query", query)
                .add("run", run)
                .toString();
    }
}
