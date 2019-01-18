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
package io.prestosql.benchto.driver.jdbc;

import io.prestosql.benchto.driver.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleDataSourcesTest
        extends IntegrationTest
{
    private static final String SQL_TEST_STATEMENT = "SELECT TOP 1 1 as VAL FROM INFORMATION_SCHEMA.SYSTEM_TABLES";

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testAllDataSourcesAvailable()
            throws Exception
    {
        testDataSourceAvailable("test_datasource");
        testDataSourceAvailable("test_datasource_2");
    }

    private void testDataSourceAvailable(String dataSourceName)
            throws SQLException
    {
        DataSource dataSource = applicationContext.getBean(dataSourceName, DataSource.class);
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(SQL_TEST_STATEMENT)) {
            int rowsCount = 0;
            while (resultSet.next()) {
                rowsCount++;
            }
            assertThat(rowsCount).isEqualTo(1);
        }
    }
}
