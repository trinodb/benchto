/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.reporters;

import com.teradata.benchmark.driver.BenchmarkQuery;
import com.teradata.benchmark.driver.BenchmarkQueryResult;
import com.teradata.benchmark.driver.sql.QueryExecution;

import java.util.List;

public interface BenchmarkExecutionListener
{
    void benchmarkStarted(BenchmarkQuery benchmarkQuery);

    void benchmarkFinished(BenchmarkQueryResult benchmarkQueryResult);

    void executionStarted(BenchmarkQuery benchmarkQuery, int run);

    void executionFinished(BenchmarkQuery benchmarkQuery, int run, QueryExecution queryExecution);

    void suiteFinished(List<BenchmarkQueryResult> queryResults);
}
