/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.facebook.presto.jdbc.PrestoResultSet;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import com.teradata.benchmark.driver.domain.QueryExecutionResult.QueryExecutionResultBuilder;
import com.teradata.benchmark.driver.listeners.BenchmarkStatusReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class QueryExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutor.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BenchmarkStatusReporter statusReporter;

    public QueryExecutionResult execute(QueryExecution queryExecution)
    {
        LOG.debug("Executing query {}: {}", queryExecution.getQuery().getName(), queryExecution.getQuery().getSql());

        statusReporter.reportExecutionStarted(queryExecution);

        QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResultBuilder(queryExecution)
                .startTimer();

        DataSource dataSource = applicationContext.getBean(queryExecution.getBenchmark().getDataSource(), DataSource.class);
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(queryExecution.getQuery().getSql())
        ) {
            int rowsCount = 0;
            while (resultSet.next()) {
                rowsCount++;
            }

            try {
                if (resultSet.isWrapperFor(PrestoResultSet.class)) {
                    PrestoResultSet prestoResultSet = resultSet.unwrap(PrestoResultSet.class);
                    queryExecutionResultBuilder.setPrestoQueryId(prestoResultSet.getQueryId());
                }
            }
            catch (AbstractMethodError e) {
                // this error is caught by the compiler, but some drivers (hsqldb?) sucks
                LOG.warn("Driver ({}) does not support isWrapperFor/unwrap method", connection.toString());
            }

            queryExecutionResultBuilder.setRowsCount(rowsCount);
        }
        catch (SQLException e) {
            queryExecutionResultBuilder.failed(e);
        }

        QueryExecutionResult result = queryExecutionResultBuilder
                .endTimer()
                .build();

        statusReporter.reportExecutionFinished(result);

        return result;
    }
}
