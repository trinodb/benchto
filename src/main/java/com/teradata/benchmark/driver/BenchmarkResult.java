/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public class BenchmarkResult
{

    private final List<BenchmarkQueryResult> benchmarkResults;

    public BenchmarkResult(List<BenchmarkQueryResult> benchmarkResults)
    {
        this.benchmarkResults = benchmarkResults;
    }

    public List<BenchmarkQueryResult> queryResults()
    {
        return unmodifiableList(benchmarkResults);
    }

    public boolean containsFailedQueries()
    {
        return benchmarkResults.stream()
                .filter(result -> !result.isSuccessful())
                .findAny().isPresent();
    }
}
