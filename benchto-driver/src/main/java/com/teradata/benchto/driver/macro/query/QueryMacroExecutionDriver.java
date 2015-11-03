/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.macro.query;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.Query;
import com.teradata.benchto.driver.loader.QueryLoader;
import com.teradata.benchto.driver.loader.SqlStatementGenerator;
import com.teradata.benchto.driver.macro.MacroExecutionDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.teradata.benchto.driver.loader.BenchmarkDescriptor.DATA_SOURCE_KEY;

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

    @Override
    public void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmarkOptional, Optional<Connection> connectionOptional)
    {
        checkArgument(benchmarkOptional.isPresent(), "Benchmark is required to run query based macro");
        Benchmark benchmark = benchmarkOptional.get();
        Query macroQuery = queryLoader.loadFromFile(macroName);

        List<String> sqlStatements = sqlStatementGenerator.generateQuerySqlStatement(macroQuery, benchmark.getNonReservedKeywordVariables());

        try {
            if (connectionOptional.isPresent() && !macroQuery.getProperty(DATA_SOURCE_KEY).isPresent()) {
                runSqlStatements(connectionOptional.get(), sqlStatements);
            }
            else {
                String dataSourceName = macroQuery.getProperty(DATA_SOURCE_KEY, benchmark.getDataSource());
                try (Connection connection = getConnectionFor(dataSourceName)) {
                    runSqlStatements(connection, sqlStatements);
                }
            }
        }
        catch (SQLException e) {
            throw new BenchmarkExecutionException("Could not execute macro SQL queries for benchmark: " + benchmark, e);
        }
    }

    private void runSqlStatements(Connection connection, List<String> sqlStatements)
            throws SQLException
    {
        for (String sqlStatement : sqlStatements) {
            LOGGER.info("Executing macro query: {}", sqlStatement);
            try (Statement statement = connection.createStatement()) {
                statement.execute(sqlStatement);
            }
        }
    }

    private Connection getConnectionFor(String dataSourceName)
            throws SQLException
    {
        return applicationContext.getBean(dataSourceName, DataSource.class).getConnection();
    }
}
