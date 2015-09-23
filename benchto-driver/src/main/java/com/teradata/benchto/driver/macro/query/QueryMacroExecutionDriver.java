/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.macro.query;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.Query;
import com.teradata.benchto.driver.loader.BenchmarkDescriptor;
import com.teradata.benchto.driver.loader.QueryLoader;
import com.teradata.benchto.driver.loader.SqlStatementGenerator;
import com.teradata.benchto.driver.macro.MacroExecutionDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Component
public class QueryMacroExecutionDriver
        implements MacroExecutionDriver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryMacroExecutionDriver.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QueryLoader queryLoader;

    @Autowired
    private SqlStatementGenerator sqlStatementGenerator;

    public boolean canExecuteBenchmarkMacro(String macroName)
    {
        return macroName.endsWith(".sql");
    }

    public void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark)
    {
        checkArgument(benchmark.isPresent(), "Benchmark is required to run query based macro");
        Query macroQuery = queryLoader.loadFromFile(macroName);

        List<String> sqlStatements = sqlStatementGenerator.generateQuerySqlStatement(macroQuery, benchmark.get().getNonReservedKeywordVariables());

        String dataSourceName = macroQuery.getProperty(BenchmarkDescriptor.DATA_SOURCE_KEY, benchmark.get().getDataSource());

        DataSource dataSource = applicationContext.getBean(dataSourceName, DataSource.class);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        for (String sqlStatement : sqlStatements) {
            LOGGER.info("Executing macro query: {}", sqlStatement);
            jdbcTemplate.execute(sqlStatement);
        }
    }
}
