/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.teradata.benchmark.driver.Benchmark;

public interface MacroExecutionDriver
{
    boolean canExecuteBenchmarkMacro(String macroName);

    void runBenchmarkMacro(String macroName, Benchmark benchmark);
}

