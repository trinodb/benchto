/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.sql.QueryExecution;
import com.teradata.benchmark.driver.sql.QueryExecutionResult;

import java.util.List;

public interface BenchmarkExecutionListener
{
    void benchmarkStarted(Benchmark benchmark);

    void benchmarkFinished(BenchmarkResult benchmarkResult);

    void executionStarted(QueryExecution queryExecution);

    void executionFinished(QueryExecutionResult execution);

    void suiteFinished(List<BenchmarkResult> queryResults);
}
