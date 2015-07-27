/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro.shell;

import com.google.common.collect.ImmutableMap;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.macro.MacroExecutionDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Executes macros using bash defined in application yaml file.
 */
@Component
public class ShellMacroExecutionDriver
        implements MacroExecutionDriver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellMacroExecutionDriver.class);

    private static final String SHELL = "bash";

    @Autowired
    private ShellMacrosProperties macros;

    public boolean canExecuteBenchmarkMacro(String macroName)
    {
        return macros.getMacros().containsKey(macroName);
    }

    @Override
    public void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark)
    {
        runBenchmarkMacro(macroName, ImmutableMap.of(), benchmark);
    }

    public void runBenchmarkMacro(String macroName, Map<String, String> environment, Optional<Benchmark> benchmark)
    {
        try {
            String macroCommand = getMacroCommand(macroName);
            ProcessBuilder processBuilder = new ProcessBuilder(SHELL, "-c", macroCommand);
            processBuilder.environment().putAll(environment);
            Process macroProcess = processBuilder.start();
            LOGGER.info("Executing macro: '{}'", macroCommand);
            printOutput(macroProcess);
            macroProcess.waitFor();
            checkState(macroProcess.exitValue() == 0, "Macro %s exited with code %s", macroName, macroProcess.exitValue());
        }
        catch (IOException | InterruptedException e) {
            throw new BenchmarkExecutionException("Could not execute macro " + macroName, e);
        }
    }

    private String getMacroCommand(String macroName)
    {
        checkArgument(macros.getMacros().containsKey(macroName), "Macro %s is not defined", macroName);
        return checkNotNull(macros.getMacros().get(macroName).getCommand(), "Macro %s has no command defined", macroName);
    }

    private void printOutput(Process process)
            throws IOException
    {
        String line;

        LOGGER.info("std output:");
        try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while ((line = input.readLine()) != null) {
                LOGGER.info(line);
            }
        }

        LOGGER.info("std error:");
        try (BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            while ((line = error.readLine()) != null) {
                LOGGER.error(line);
            }
        }
    }
}
