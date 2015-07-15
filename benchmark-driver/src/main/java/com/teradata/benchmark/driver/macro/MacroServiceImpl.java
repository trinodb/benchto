/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.teradata.benchmark.driver.Benchmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Component
public class MacroServiceImpl
        implements MacroService
{
    @Autowired
    private List<MacroExecutionDriver> macroExecutionDrivers;

    public void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark)
    {
        MacroExecutionDriver macroExecutionDriver = macroExecutionDrivers.stream()
                .filter(executionDriver -> executionDriver.canExecuteBenchmarkMacro(macroName))
                .collect(Collectors.collectingAndThen(toList(), matchingExecutionDrivers -> {
                    if (matchingExecutionDrivers.size() > 1) {
                        throw new IllegalStateException(format("More than one execution driver for macro %s - matching drivers %s", macroName, matchingExecutionDrivers));
                    }
                    else if (matchingExecutionDrivers.size() == 0) {
                        throw new IllegalStateException(format("No execution driver for macro %s", macroName));
                    }
                    return matchingExecutionDrivers.get(0);
                }));

        macroExecutionDriver.runBenchmarkMacro(macroName, benchmark);
    }
}
