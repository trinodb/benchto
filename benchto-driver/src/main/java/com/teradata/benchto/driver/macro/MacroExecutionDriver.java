/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.macro;

import com.teradata.benchto.driver.Benchmark;

import java.util.Optional;

public interface MacroExecutionDriver
{
    boolean canExecuteBenchmarkMacro(String macroName);

    void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark);
}

