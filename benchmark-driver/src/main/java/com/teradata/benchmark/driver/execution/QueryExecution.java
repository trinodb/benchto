/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class QueryExecution
{
    private final BenchmarkExecution benchmarkExecution;
    private final Query query;
    private final int run;

    public QueryExecution(BenchmarkExecution benchmarkExecution, Query query, int run)
    {
        this.benchmarkExecution = checkNotNull(benchmarkExecution);
        this.query = checkNotNull(query);
        this.run = run;
    }

    public BenchmarkExecution getBenchmarkExecution()
    {
        return benchmarkExecution;
    }

    public String getQueryName()
    {
        return query.getName();
    }

    public String getSql()
    {
        return query.getSql();
    }

    public int getRun()
    {
        return run;
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
