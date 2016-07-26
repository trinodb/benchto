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
package com.teradata.benchto.driver.jdbc;

import com.teradata.benchto.driver.IntegrationTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionPoolTest
        extends IntegrationTest
{

    private static final String SQL_TEST_STATEMENT = "SELECT TOP 1 1 as VAL FROM INFORMATION_SCHEMA.SYSTEM_TABLES";

    @Autowired
    private ApplicationContext applicationContext;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldSuccessfullyOpenMaxConnectionsCount()
            throws Exception
    {
        openGivenConnectionsAmountSimultaneously("test_datasource", 500);
    }

    private void openGivenConnectionsAmountSimultaneously(String dataSourceName, int connectionsCount)
            throws SQLException, InterruptedException, TimeoutException
    {
        ExecutorService executorService = newFixedThreadPool(connectionsCount);
        ExecutorCompletionService<?> completionService = new ExecutorCompletionService(executorService);
        CountDownLatch countDownLatch = new CountDownLatch(connectionsCount);
        DataSource dataSource = applicationContext.getBean(dataSourceName, DataSource.class);

        range(0, connectionsCount)
                .mapToObj(i -> createQueryRunnable(dataSource, countDownLatch))
                .forEach(completionService::submit);

        try {
            for (int i = 0; i < connectionsCount; i++) {
                try {
                    Future<?> future = completionService.take();
                    future.get(1, MINUTES);
                }
                catch (ExecutionException e) {
                    rethrowException(e.getCause());
                }
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        finally {
            executorService.shutdownNow();
            executorService.awaitTermination(1, MINUTES);
        }
    }

    private Callable createQueryRunnable(DataSource dataSource, CountDownLatch countDownLatch)
    {
        return () -> {
            // open new connection
            try (Connection connection = dataSource.getConnection()) {
                // wait till all the other connections get opened
                countDownLatch.countDown();
                countDownLatch.await(1, MINUTES);

                //check that connection is alive
                checkThatConnectionAlive(connection);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        };
    }

    private void checkThatConnectionAlive(Connection connection)
            throws SQLException
    {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(SQL_TEST_STATEMENT)) {
            int rowsCount = 0;
            while (resultSet.next()) {
                rowsCount++;
            }
            assertThat(rowsCount).isEqualTo(1);
        }
    }

    private void rethrowException(Throwable e)
            throws SQLException
    {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        if (e instanceof Error) {
            throw (Error) e;
        }
        if (e instanceof SQLException) {
            throw (SQLException) e;
        }
        throw new RuntimeException(e);
    }
}
