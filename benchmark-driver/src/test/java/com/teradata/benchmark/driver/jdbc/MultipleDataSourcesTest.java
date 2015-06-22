/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.jdbc;

import com.teradata.benchmark.driver.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleDataSourcesTest extends IntegrationTest {

    private static final String SQL_TEST_STATEMENT = "SELECT TOP 1 1 as VAL FROM INFORMATION_SCHEMA.SYSTEM_TABLES";

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testAllDataSourcesAvailable() throws Exception {
        testDataSourceAvailable("test_datasource");
        testDataSourceAvailable("test_datasource_2");
    }

    private void testDataSourceAvailable(String dataSourceName) throws SQLException {
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
