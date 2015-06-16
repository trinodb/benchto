/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Benchmark
{
    private final String name;
    private final List<Query> queries;
    private final int runs;
    private final int concurrency;

    public Benchmark(String name, List<Query> queries, int runs, int concurrency)
    {
        this.name = name;
        this.queries = ImmutableList.copyOf(checkNotNull(queries));
        this.runs = runs;
        this.concurrency = concurrency;
    }

    public String getName()
    {
        return name;
    }

    public List<Query> getQueries()
    {
        return queries;
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
