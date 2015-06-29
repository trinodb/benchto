/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro.shell;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@Component
@ConfigurationProperties
public class ShellMacrosProperties
{
    private Map<String, ShellMacroProperties> macros = newHashMap();

    public Map<String, ShellMacroProperties> getMacros()
    {
        return macros;
    }
}
