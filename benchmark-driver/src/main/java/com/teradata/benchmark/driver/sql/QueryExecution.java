/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.sql;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Query;

import static com.google.common.base.MoreObjects.toStringHelper;

public class QueryExecution
{
    private final Benchmark benchmark;
    private final Query query;
    private final int run;

    public QueryExecution(Benchmark benchmark, Query query, int run)
    {
        this.benchmark = benchmark;
        this.query = query;
        this.run = run;
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

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("query", query)
                .add("run", run)
                .toString();
    }
}
