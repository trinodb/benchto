/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.teradata.benchmark.driver.BenchmarkExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Component
public class ShellMacroService
        implements MacroService
{
    private static final String SHELL = "bash";

    @Autowired
    private MacrosProperties macros;

    @Override
    public void runMacro(String macroName, Map<String, String> environment)
    {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(SHELL, "-c", getMacroCommand(macroName));
            processBuilder.environment().putAll(environment);
            Process macroProcess = processBuilder.start();
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
}
