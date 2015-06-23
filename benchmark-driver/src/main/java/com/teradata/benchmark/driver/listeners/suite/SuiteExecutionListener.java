/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.suite;

import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;

import java.util.List;

public interface SuiteExecutionListener
{
    void suiteFinished(List<BenchmarkExecutionResult> queryResults);
}
