/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.benchmark;

import com.teradata.benchmark.driver.execution.BenchmarkExecution;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.execution.QueryExecution;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;

public interface BenchmarkExecutionListener
{
    void benchmarkStarted(BenchmarkExecution benchmarkExecution);

    void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult);

    void executionStarted(QueryExecution queryExecution);

    void executionFinished(QueryExecutionResult execution);
}
