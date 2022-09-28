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

import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.Query;
import io.trino.benchto.driver.loader.SqlStatementGenerator;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QueryExecutionResultTest
{
    @Test
    public void testBuilder_successful_run()
            throws InterruptedException
    {
        QueryExecutionResult.QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResult.QueryExecutionResultBuilder(queryExecution())
                .setRowsCount(100);

        queryExecutionResultBuilder.startTimer();
        TimeUnit.MILLISECONDS.sleep(500L);
        queryExecutionResultBuilder.endTimer();

        QueryExecutionResult execution = queryExecutionResultBuilder.build();

        assertThat(execution.isSuccessful()).isTrue();
        assertThat(execution.getRowsCount()).isEqualTo(100);
        assertThat(execution.getQueryDuration().toMillis()).isBetween(500L, 600L);
    }

    @Test
    public void testBuilder_failed_run()
            throws InterruptedException
    {
        QueryExecutionResult.QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResult.QueryExecutionResultBuilder(queryExecution());

        queryExecutionResultBuilder.startTimer();
        TimeUnit.MILLISECONDS.sleep(500L);
        queryExecutionResultBuilder.failed(new NullPointerException());
        queryExecutionResultBuilder.endTimer();

        QueryExecutionResult execution = queryExecutionResultBuilder.build();

        assertThat(execution.isSuccessful()).isFalse();
        assertThat(execution.getRowsCount()).isEqualTo(0);
        assertThat(execution.getFailureCause().getClass()).isEqualTo(NullPointerException.class);
        assertThat(execution.getQueryDuration().toMillis()).isBetween(500L, 600L);
    }

    private QueryExecution queryExecution()
    {
        return new QueryExecution(mock(Benchmark.class), mock(Query.class), 0, new SqlStatementGenerator(){
            @Override
            public List<String> generateQuerySqlStatement(Query query, Map<String, ?> attributes)
            {
                return Collections.singletonList(query.getSqlTemplate());
            }
        });
    }
}
