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
package com.teradata.benchto.driver;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.teradata.benchto.driver.concurrent.ExecutorServiceFactory;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionDriver;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import com.teradata.benchto.driver.macro.MacroService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static org.mockito.Mockito.spy;

public class TestConfig
{
    @Primary
    @Bean
    public AsyncTaskExecutor defaultTaskExecutor()
    {
        // MockRestServiceServer expects calls in particular order,
        // we need to use sync task executor
        return new TaskExecutorAdapter(MoreExecutors.directExecutor());
    }

    @Primary
    @Bean
    public MacroService macroExecutionDriver(MacroService macroService)
    {
        return spy(macroService);
    }

    @Primary
    @Bean
    public ExecutorServiceFactory getDirectTestExecutorServiceFactory()
    {
        return new ExecutorServiceFactory()
        {
            @Override
            public ListeningExecutorService create(int concurrency)
            {
                // no concurrency in tests
                return listeningDecorator(newDirectExecutorService());
            }
        };
    }

    @Primary
    @Bean
    public QueryExecutionDriver queryExecutionDriver()
    {
        return new QueryExecutionDriver()
        {
            @Override
            public QueryExecutionResult execute(QueryExecution queryExecution, Connection connection)
                    throws SQLException
            {
                QueryExecutionResult executionResult = super.execute(queryExecution, connection);

                // Queries in tests need to seemingly take non-zero duration (measured with seconds precision), even if Graphite precision is subtracted.
                ZonedDateTime newStart = ((ZonedDateTime) ReflectionTestUtils.getField(executionResult, "utcStart"))
                        .minus(2, ChronoUnit.SECONDS);
                ReflectionTestUtils.setField(executionResult, "utcStart", newStart);

                return executionResult;
            }
        };
    }
}
