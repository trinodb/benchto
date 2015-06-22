/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.suite;

import com.teradata.benchmark.driver.domain.BenchmarkResult;

import java.util.List;

public interface SuiteExecutionListener
{
    void suiteFinished(List<BenchmarkResult> queryResults);
}
