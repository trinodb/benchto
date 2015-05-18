/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.reporters;

import com.teradata.benchmark.driver.BenchmarkQuery;
import com.teradata.benchmark.driver.BenchmarkQueryResult;
import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.sql.QueryExecution;

public interface BenchmarkResultReporter
{
    void reportQueryExecution(BenchmarkQuery benchmarkQuery, QueryExecution queryExecution);

    void reportQueryResult(BenchmarkQueryResult benchmarkQueryResult);

    void reportBenchmarkResult(BenchmarkResult benchmarkResult);
}
