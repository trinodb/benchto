/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.teradata.benchmark.driver.BenchmarkExecutionException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Component
public class ShellMacroService
        implements MacroService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellMacroService.class);

    private static final String SHELL = "bash";

    @Autowired
    private MacrosProperties macros;

    @Override
    public void runMacro(String macroName, Map<String, String> environment)
    {
        try {
            String macroCommand = getMacroCommand(macroName);
            ProcessBuilder processBuilder = new ProcessBuilder(SHELL, "-c", macroCommand);
            processBuilder.environment().putAll(environment);
            Process macroProcess = processBuilder.start();
            macroProcess.waitFor();
            LOGGER.debug("Executed macro: '{}'", macroCommand);
            if (macroProcess.exitValue() != 0) {
                LOGGER.error("Executed macro: '{}' failed with: '{}', out: '{}', err: '{}'",
                        macroCommand,
                        macroProcess.exitValue(),
                        IOUtils.toString(macroProcess.getInputStream()),
                        IOUtils.toString(macroProcess.getErrorStream())
                );
                throw new IllegalStateException(String.format("Macro %s exited with code %s", macroName, macroProcess.exitValue()));
            }

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
