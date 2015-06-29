/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.teradata.benchmark.driver.Benchmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.facebook.presto.jdbc.internal.guava.collect.Iterables.filter;
import static com.facebook.presto.jdbc.internal.guava.collect.Iterables.getOnlyElement;

@Component
public class MacroServiceImpl
        implements MacroService
{
    @Autowired
    private List<MacroExecutionDriver> macroExecutionDrivers;

    public void runBenchmarkMacro(String macroName, Benchmark benchmark)
    {
        MacroExecutionDriver macroExecutionDriver = getOnlyElement(filter(macroExecutionDrivers, service -> service.canExecuteBenchmarkMacro(macroName)));
        macroExecutionDriver.runBenchmarkMacro(macroName, benchmark);
    }
}
