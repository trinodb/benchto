/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.model;

import java.util.List;

public class Benchmark
{
    private final String name;
    private final List<BenchmarkRun> runs;

    public Benchmark(String name, List<BenchmarkRun> runs)
    {
        this.name = name;
        this.runs = runs;
    }

    public String getName()
    {
        return name;
    }

    public List<BenchmarkRun> getRuns()
    {
        return runs;
    }
}
