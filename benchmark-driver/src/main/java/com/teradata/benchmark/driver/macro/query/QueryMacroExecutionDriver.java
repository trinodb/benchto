/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.macro.query;

import com.google.common.collect.ImmutableMap;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.loader.QueryLoader;
import com.teradata.benchmark.driver.macro.MacroExecutionDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class QueryMacroExecutionDriver
        implements MacroExecutionDriver
{
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QueryLoader queryLoader;

    public boolean canExecuteBenchmarkMacro(String macroName)
    {
        return macroName.endsWith(".sql");
    }

    public void runBenchmarkMacro(String macroName, Benchmark benchmark)
    {
        Query query = queryLoader.loadFromFile(macroName, ImmutableMap.of());
        DataSource dataSource = applicationContext.getBean(benchmark.getDataSource(), DataSource.class);
        new JdbcTemplate(dataSource).execute(query.getSql());
    }
}
