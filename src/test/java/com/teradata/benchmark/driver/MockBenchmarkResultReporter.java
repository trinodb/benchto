/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.reporters.BenchmarkResultReporter;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class MockBenchmarkResultReporter
        implements BenchmarkResultReporter
{

    private BenchmarkResult capturedBenchmarkResult;
    private List<QueryExecution> capturedQueryExecutions = newArrayList();
    private List<BenchmarkQueryResult> capturedBenchmarkQueryResults = newArrayList();

    @Override
    public void reportQueryExecution(BenchmarkQuery benchmarkQuery, QueryExecution queryExecution)
    {
        capturedQueryExecutions.add(queryExecution);
    }

    @Override
    public void reportQueryResult(BenchmarkQueryResult benchmarkQueryResult)
    {
        capturedBenchmarkQueryResults.add(benchmarkQueryResult);
    }

    @Override
    public void reportBenchmarkResult(BenchmarkResult benchmarkResult)
    {
        capturedBenchmarkResult = benchmarkResult;
    }

    public BenchmarkResult capturedBenchmarkResult()
    {
        return capturedBenchmarkResult;
    }

    public List<QueryExecution> capturedQueryExecutions()
    {
        return capturedQueryExecutions;
    }

    public List<BenchmarkQueryResult> capturedBenchmarkQueryResults()
    {
        return capturedBenchmarkQueryResults;
    }
}
