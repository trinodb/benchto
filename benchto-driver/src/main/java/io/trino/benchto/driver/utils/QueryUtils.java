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
package io.trino.benchto.driver.utils;

import io.trino.benchto.driver.execution.ResultComparisonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.StringJoiner;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public final class QueryUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryUtils.class);
    private static final int LOGGED_ROWS = 10;

    public static int fetchRows(String sqlStatement, ResultSet resultSet)
            throws SQLException
    {
        LOGGER.info("First {} rows for query: {}", LOGGED_ROWS, sqlStatement);

        int rowsCount = 0;
        while (resultSet.next()) {
            if (rowsCount < LOGGED_ROWS) {
                logRow(rowsCount + 1, resultSet);
            }
            else if (rowsCount == LOGGED_ROWS) {
                LOGGER.info("There are more unlogged rows");
            }
            rowsCount++;
        }

        return rowsCount;
    }

    public static int compareRows(Path resultFile, ResultSet resultSet)
            throws SQLException
    {
        LOGGER.info("Comparing result with {}", resultFile);

        try (BufferedReader reader = Files.newBufferedReader(resultFile)) {
            int lineCount = 0;
            while (true) {
                lineCount++;
                String resultRow = reader.readLine();
                boolean hasRows = resultSet.next();
                if (resultRow == null && !hasRows) {
                    break;
                }
                if (resultRow == null) {
                    throw new ResultComparisonException(format("Result file has %d lines, actual result has more rows", lineCount - 1));
                }
                if (!hasRows) {
                    throw new ResultComparisonException(format("Actual result has %d rows, result file has more lines", lineCount - 1));
                }
                String dbRow = resultRowToString(resultSet);
                if (!dbRow.equals(resultRow)) {
                    throw new ResultComparisonException(format("Incorrect result at row %d, expected %s, got %s",
                            lineCount,
                            resultRow,
                            dbRow));
                }
            }
            return lineCount;
        }
        catch (IOException e) {
            throw new ResultComparisonException("Error opening result file", e);
        }
    }

    private static String resultRowToString(ResultSet resultSet)
            throws SQLException
    {
        return IntStream.range(1, resultSet.getMetaData().getColumnCount() + 1)
                .boxed()
                .map(i -> {
                    try {
                        Object value = resultSet.getObject(i);
                        return value == null ? "" : value.toString().replaceAll("\\s+", "");
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(joining(","));
    }

    private static void logRow(int rowNumber, ResultSet resultSet)
            throws SQLException
    {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        StringJoiner joiner = new StringJoiner("; ", "[", "]");
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); ++i) {
            joiner.add(resultSetMetaData.getColumnName(i) + ": " + resultSet.getObject(i));
        }

        LOGGER.info("Row: " + rowNumber + ", column values: " + joiner);
    }

    private QueryUtils()
    {
    }
}
