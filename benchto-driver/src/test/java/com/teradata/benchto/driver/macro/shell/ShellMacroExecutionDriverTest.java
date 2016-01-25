/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.macro.shell;

import com.google.common.collect.ImmutableMap;
import com.teradata.benchto.driver.IntegrationTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static org.assertj.core.api.Assertions.assertThat;

public class ShellMacroExecutionDriverTest
        extends IntegrationTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private ShellMacroExecutionDriver macroService;

    @Test
    public void shouldExecuteMacro()
            throws IOException
    {
        String filename = "/tmp/" + UUID.randomUUID().toString();
        String suffix = System.getenv("USER");

        macroService.runBenchmarkMacro("create-file", ImmutableMap.of("FILENAME", filename));

        Path path = Paths.get(filename + suffix);
        assertThat(exists(path)).isTrue();
        delete(path);
    }

    @Test
    public void shouldFailWhenMacroFails()
    {
        expectedException.expectMessage("Macro error-macro exited with code 1");
        macroService.runBenchmarkMacro("error-macro", Optional.empty(), Optional.empty());
    }

    @Test
    public void shouldFailNoCommandMacro()

    {
        expectedException.expectMessage("Macro no-command-macro has no command defined");
        macroService.runBenchmarkMacro("no-command-macro", Optional.empty(), Optional.empty());
    }

    @Test
    public void shouldFailNoMacro()

    {
        expectedException.expectMessage("Macro non-existing-macro is not defined");
        macroService.runBenchmarkMacro("non-existing-macro", Optional.empty(), Optional.empty());
    }
}
