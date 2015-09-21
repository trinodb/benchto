/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.listeners.benchmark;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionResult;

public interface BenchmarkExecutionListener
{
    void benchmarkStarted(Benchmark benchmark);

    void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult);

    void executionStarted(QueryExecution queryExecution);

    void executionFinished(QueryExecutionResult execution);
}
