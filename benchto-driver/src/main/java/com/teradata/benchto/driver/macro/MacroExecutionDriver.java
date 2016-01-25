/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.macro;

import com.teradata.benchto.driver.Benchmark;

import java.sql.Connection;
import java.util.Optional;

public interface MacroExecutionDriver
{
    boolean canExecuteBenchmarkMacro(String macroName);

    void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark, Optional<Connection> connection);
}

