package com.teradata.benchmark.driver;

import java.util.List;

public class Benchmark
{
    private List<Query> queries;
    private int runs;

    public Benchmark(List<Query> queries, int runs)
    {
        this.queries = queries;
        this.runs = runs;
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
