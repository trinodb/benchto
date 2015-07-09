/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.benchmark;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.execution.QueryExecution;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;

public interface BenchmarkExecutionListener
{
    void benchmarkStarted(Benchmark benchmark);

    void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult);

    void executionStarted(QueryExecution queryExecution);

    void executionFinished(QueryExecutionResult execution);
}
