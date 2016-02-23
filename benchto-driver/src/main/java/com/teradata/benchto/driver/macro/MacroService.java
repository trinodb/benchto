/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.macro;

import com.teradata.benchto.driver.Benchmark;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface MacroService
{
    default void runBenchmarkMacros(List<String> macroNames)
    {
        runBenchmarkMacros(macroNames, Optional.empty(), Optional.<Connection>empty());
    }

    default void runBenchmarkMacros(List<String> macroNames, Benchmark benchmark)
    {
        runBenchmarkMacros(macroNames, Optional.of(benchmark), Optional.<Connection>empty());
    }

    default void runBenchmarkMacros(List<String> macroNames, Benchmark benchmark, Connection connection)
    {
        for (String macroName : macroNames) {
            runBenchmarkMacro(macroName, Optional.of(benchmark), Optional.of(connection));
        }
    }

    default void runBenchmarkMacros(List<String> macroNames, Optional<Benchmark> benchmark, Optional<Connection> connection)
    {
        for (String macroName : macroNames) {
            runBenchmarkMacro(macroName, benchmark, connection);
        }
    }

    void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark, Optional<Connection> connection);
}
