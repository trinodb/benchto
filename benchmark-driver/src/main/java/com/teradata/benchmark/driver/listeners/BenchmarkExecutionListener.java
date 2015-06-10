/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.sql.QueryExecution;

import java.util.List;

public interface BenchmarkExecutionListener
{
    void benchmarkStarted(Query benchmarkQuery);

    void benchmarkFinished(BenchmarkResult benchmarkResult);

    void executionStarted(Query benchmarkQuery, int run);

    void executionFinished(Query benchmarkQuery, int run, QueryExecution queryExecution);

    void suiteFinished(List<BenchmarkResult> queryResults);
}
