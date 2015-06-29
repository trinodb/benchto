/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.teradata.benchmark.driver.Benchmark;

import java.util.List;

public interface MacroService
{
    default void runBenchmarkMacros(List<String> macroNames, Benchmark benchmark)
    {
        for (String macroName : macroNames) {
            runBenchmarkMacro(macroName, benchmark);
        }
    }

    void runBenchmarkMacro(String macroName, Benchmark benchmark);
}
