/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.execution;

import com.facebook.presto.jdbc.PrestoResultSet;
import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.teradata.benchto.driver.execution.QueryExecutionResult.QueryExecutionResultBuilder;
import com.teradata.benchto.driver.loader.SqlStatementGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkState;

@Component
public class QueryExecutionDriver
{
    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutionDriver.class);
    private static final int LOGGED_ROWS = 10;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SqlStatementGenerator sqlStatementGenerator;

    public QueryExecutionResult execute(QueryExecution queryExecution)
            throws SQLException
    {
        QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResultBuilder(queryExecution)
                .startTimer();

        String sqlStatement = generateQuerySqlStatement(queryExecution);
        DataSource dataSource = applicationContext.getBean(queryExecution.getBenchmark().getDataSource(), DataSource.class);
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sqlStatement)
        ) {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            LOG.info("First {} rows for query: {}", LOGGED_ROWS, sqlStatement);

            int rowsCount = 0;
            while (resultSet.next()) {
                if (rowsCount++ < LOGGED_ROWS) {
                    logRow(rowsCount, resultSet);
                }
            }

            try {
                if (resultSet.isWrapperFor(PrestoResultSet.class)) {
                    PrestoResultSet prestoResultSet = resultSet.unwrap(PrestoResultSet.class);
                    queryExecutionResultBuilder.setPrestoQueryId(prestoResultSet.getQueryId());
                }
            }
            catch (AbstractMethodError | Exception e) {
                // this error is caught by the compiler, but some drivers (hsqldb, hive, ...?) sucks
                LOG.warn("Driver ({}) does not support isWrapperFor/unwrap method", connection.toString());
            }

            return queryExecutionResultBuilder
                    .setRowsCount(rowsCount)
                    .endTimer()
                    .build();
        }
    }

    private String generateQuerySqlStatement(QueryExecution queryExecution)
    {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put("execution_sequence_id", "" + queryExecution.getRun())
                .putAll(queryExecution.getBenchmark().getNonReservedKeywordVariables())
                .build();
        List<String> sqlQueries = sqlStatementGenerator.generateQuerySqlStatement(queryExecution.getQuery(), variables);
        checkState(sqlQueries.size() == 1);
        return sqlQueries.get(0);
    }

    private void logRow(int rowNumber, ResultSet resultSet)
            throws SQLException
    {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        StringJoiner joiner = new StringJoiner("; ", "[", "]");
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); ++i) {
            joiner.add(resultSetMetaData.getColumnName(i) + ": " + resultSet.getObject(i));
        }

        LOG.info("Row: " + rowNumber + ", column values: " + joiner.toString());
    }
}
