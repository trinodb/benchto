package com.teradata.benchmark.driver;

import java.util.List;

public class Benchmark
{
    private final String name;
    private final List<Query> queries;
    private final int runs;

    public Benchmark(String name, List<Query> queries, int runs)
    {
        this.name = name;
        this.queries = queries;
        this.runs = runs;
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
}
