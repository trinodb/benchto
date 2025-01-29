/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.benchto.driver.execution;

import io.trino.benchto.driver.execution.QueryExecutionResult.QueryExecutionResultBuilder;
import io.trino.jdbc.TrinoResultSet;
import io.trino.jdbc.TrinoStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static io.trino.benchto.driver.utils.QueryUtils.compareCount;
import static io.trino.benchto.driver.utils.QueryUtils.compareRows;
import static io.trino.benchto.driver.utils.QueryUtils.fetchRows;
import static io.trino.benchto.driver.utils.QueryUtils.isSelectQuery;

public class QueryExecutionDriver
{
    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutionDriver.class);

    public QueryExecutionResult execute(QueryExecution queryExecution, Connection connection, Optional<Path> resultFile)
            throws SQLException
    {
        QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResultBuilder(queryExecution)
                .startTimer();

        String sqlStatement = queryExecution.getStatement();

        if (isSelectQuery(sqlStatement)) {
            return executeSelectQuery(connection, queryExecutionResultBuilder, sqlStatement, resultFile);
        }
        else {
            return executeUpdateQuery(connection, queryExecutionResultBuilder, sqlStatement, resultFile);
        }
    }

    private QueryExecutionResult executeSelectQuery(
            Connection connection,
            QueryExecutionResultBuilder queryExecutionResultBuilder,
            String sqlStatement,
            Optional<Path> resultFile)
            throws SQLException
    {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sqlStatement)) {
            int rowsCount;
            if (resultFile.isPresent()) {
                // load results from file and compare
                rowsCount = compareRows(resultFile.get(), resultSet);
            }
            else {
                // ignore results
                rowsCount = fetchRows(sqlStatement, resultSet);
            }

            try {
                if (resultSet.isWrapperFor(TrinoResultSet.class)) {
                    TrinoResultSet trinoResultSet = resultSet.unwrap(TrinoResultSet.class);
                    queryExecutionResultBuilder.setPrestoQueryId(trinoResultSet.getQueryId());
                    queryExecutionResultBuilder.setPrestoQueryStats(trinoResultSet.getStats());
                }
            }
            catch (AbstractMethodError | Exception e) {
                // this error is caught by the compiler, but some drivers (hsqldb, hive, ...?) sucks
                LOG.warn("Driver ({}) does not support isWrapperFor/unwrap method", connection);
            }

            return queryExecutionResultBuilder
                    .setRowsCount(rowsCount)
                    .endTimer()
                    .build();
        }
    }

    private QueryExecutionResult executeUpdateQuery(
            Connection connection,
            QueryExecutionResultBuilder queryExecutionResultBuilder,
            String sqlStatement,
            Optional<Path> resultFile)
            throws SQLException
    {
        try (Statement statement = connection.createStatement()) {
            if (statement.isWrapperFor(TrinoStatement.class)) {
                TrinoStatement trinoStatement = statement.unwrap(TrinoStatement.class);
                trinoStatement.setProgressMonitor(stats -> queryExecutionResultBuilder.setPrestoQueryId(stats.getQueryId())
                        .setPrestoQueryStats(stats));
            }

            int rowCount = statement.executeUpdate(sqlStatement);
            resultFile.ifPresent(path -> compareCount(path, rowCount));

            return queryExecutionResultBuilder
                    .setRowsCount(rowCount)
                    .endTimer()
                    .build();
        }
    }
}
