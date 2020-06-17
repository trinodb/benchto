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
package io.prestosql.benchto.driver.macro.query;

import io.prestosql.benchto.driver.Benchmark;
import io.prestosql.benchto.driver.BenchmarkExecutionException;
import io.prestosql.benchto.driver.Query;
import io.prestosql.benchto.driver.loader.QueryLoader;
import io.prestosql.benchto.driver.loader.SqlStatementGenerator;
import io.prestosql.benchto.driver.macro.MacroExecutionDriver;
import io.prestosql.jdbc.PrestoConnection;
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
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.prestosql.benchto.driver.loader.BenchmarkDescriptor.DATA_SOURCE_KEY;
import static io.prestosql.benchto.driver.utils.QueryUtils.fetchRows;
import static java.lang.String.format;

@Component
public class QueryMacroExecutionDriver
        implements MacroExecutionDriver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryMacroExecutionDriver.class);
    private static final String SET_SESSION = "set session";
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("([^=]+)=\'??([^\']+)\'??");

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
            sqlStatement = sqlStatement.trim();
            LOGGER.info("Executing macro query: {}", sqlStatement);
            if (sqlStatement.toLowerCase().startsWith(SET_SESSION) && connection.isWrapperFor(PrestoConnection.class)) {
                setSessionForPresto(connection, sqlStatement);
            }
            else {
                try (Statement statement = connection.createStatement()) {
                    if (statement.execute(sqlStatement)) {
                        try (ResultSet resultSet = statement.getResultSet()) {
                            fetchRows(sqlStatement, resultSet);
                        }
                    }
                }
            }
        }
    }

    private void setSessionForPresto(Connection connection, String sqlStatement)
    {
        PrestoConnection prestoConnection;
        try {
            prestoConnection = connection.unwrap(PrestoConnection.class);
        }
        catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw new UnsupportedOperationException(format("SET SESSION for non PrestoConnection [%s] is not supported", connection.getClass()));
        }
        String[] keyValue = extractKeyValue(sqlStatement);
        prestoConnection.setSessionProperty(keyValue[0].trim(), keyValue[1].trim());
    }

    public static String[] extractKeyValue(String sqlStatement)
    {
        String keyValueSql = sqlStatement.substring(SET_SESSION.length(), sqlStatement.length()).trim();
        Matcher matcher = KEY_VALUE_PATTERN.matcher(keyValueSql);
        checkState(matcher.matches(), "Unexpected SET SESSION format [%s]", sqlStatement);
        String[] keyValue = new String[2];
        keyValue[0] = matcher.group(1).trim();
        keyValue[1] = matcher.group(2).trim();
        return keyValue;
    }

    private Connection getConnectionFor(String dataSourceName)
            throws SQLException
    {
        return applicationContext.getBean(dataSourceName, DataSource.class).getConnection();
    }
}
