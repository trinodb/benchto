/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.domain.Benchmark;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;

import java.util.List;

public interface BenchmarkExecutionListener
{
    void benchmarkStarted(Benchmark benchmark);

    void benchmarkFinished(BenchmarkResult benchmarkResult);

    void executionStarted(QueryExecution queryExecution);

    void executionFinished(QueryExecutionResult execution);

    void suiteFinished(List<BenchmarkResult> queryResults);
}
