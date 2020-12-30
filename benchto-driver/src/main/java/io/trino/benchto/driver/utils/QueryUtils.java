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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.StringJoiner;

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

    private static void logRow(int rowNumber, ResultSet resultSet)
            throws SQLException
    {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        StringJoiner joiner = new StringJoiner("; ", "[", "]");
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); ++i) {
            joiner.add(resultSetMetaData.getColumnName(i) + ": " + resultSet.getObject(i));
        }

        LOGGER.info("Row: " + rowNumber + ", column values: " + joiner.toString());
    }

    private QueryUtils()
    {
    }
}
