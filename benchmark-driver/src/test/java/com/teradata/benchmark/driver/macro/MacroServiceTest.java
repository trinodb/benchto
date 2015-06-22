/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro;

import com.google.common.collect.ImmutableMap;
import com.teradata.benchmark.driver.IntegrationTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static org.assertj.core.api.Assertions.assertThat;

public class MacroServiceTest
        extends IntegrationTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private ShellMacroService macroService;

    @Test
    public void shouldExecuteMacro()
            throws IOException
    {
        String filename = "/tmp/" + UUID.randomUUID().toString();
        String suffix = System.getenv("USER");

        macroService.runMacro("create-file", ImmutableMap.of("FILENAME", filename));

        Path path = Paths.get(filename + suffix);
        assertThat(exists(path)).isTrue();
        delete(path);
    }

    @Test
    public void shouldFailWhenMacroFails()
    {
        expectedException.expectMessage("Macro error-macro exited with code 1");
        macroService.runMacro("error-macro");
    }

    @Test
    public void shouldFailNoCommandMacro()

    {
        expectedException.expectMessage("Macro no-command-macro has no command defined");
        macroService.runMacro("no-command-macro");
    }

    @Test
    public void shouldFailNoMacro()

    {
        expectedException.expectMessage("Macro non-existing-macro is not defined");
        macroService.runMacro("non-existing-macro");
    }
}
