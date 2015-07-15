/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.teradata.benchmark.driver.Benchmark;

import java.util.List;
import java.util.Optional;

public interface MacroService
{
    default void runBenchmarkMacros(List<String> macroNames) {
        runBenchmarkMacros(macroNames, Optional.empty());
    }

    default void runBenchmarkMacros(List<String> macroNames, Optional<Benchmark> benchmark)
    {
        for (String macroName : macroNames) {
            runBenchmarkMacro(macroName, benchmark);
        }
    }

    void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark);
}
