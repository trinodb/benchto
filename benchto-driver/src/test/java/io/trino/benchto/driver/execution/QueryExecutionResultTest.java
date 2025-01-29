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
import io.trino.jdbc.QueryStats;
import io.trino.jdbc.StageStats;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
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
    public void testBuilder_queryMetrics()
    {
        QueryStats queryStats = new QueryStats("abc123", "ENDED", false, false, OptionalDouble.of(100), 2, 3,
                0, 0, 3, 5678, 7423, 2147, 6783, 350,
                56789, 1296433, 9156, 568, 28469135, 8249673, 64852,
                12, 82497, Optional.of(mock(StageStats.class)));

        QueryExecutionResult.QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResult.QueryExecutionResultBuilder(queryExecution())
                .setRowsCount(25).setPrestoQueryStats(queryStats);

        QueryExecutionResult execution = queryExecutionResultBuilder.build();

        assertThat(execution.isSuccessful()).isTrue();
        assertThat(execution.getRowsCount()).isEqualTo(25);
        assertThat(execution.getPrestoQueryStats().isPresent()).isTrue();
        assertThat(execution.getPrestoQueryStats().get().getQueryId()).isEqualTo("abc123");
        assertThat(execution.getPrestoQueryStats().get().getState()).isEqualTo("ENDED");
        assertThat(execution.getPrestoQueryStats().get().getPlanningTimeMillis()).isEqualTo(5678);
        assertThat(execution.getPrestoQueryStats().get().getAnalysisTimeMillis()).isEqualTo(7423);
        assertThat(execution.getPrestoQueryStats().get().getCpuTimeMillis()).isEqualTo(2147);
        assertThat(execution.getPrestoQueryStats().get().getWallTimeMillis()).isEqualTo(6783);
        assertThat(execution.getPrestoQueryStats().get().getQueuedTimeMillis()).isEqualTo(350);
        assertThat(execution.getPrestoQueryStats().get().getElapsedTimeMillis()).isEqualTo(56789);
        assertThat(execution.getPrestoQueryStats().get().getFinishingTimeMillis()).isEqualTo(1296433);
        assertThat(execution.getPrestoQueryStats().get().getPhysicalInputTimeMillis()).isEqualTo(9156);
        assertThat(execution.getPrestoQueryStats().get().getProcessedRows()).isEqualTo(568);
        assertThat(execution.getPrestoQueryStats().get().getProcessedBytes()).isEqualTo(28469135);
        assertThat(execution.getPrestoQueryStats().get().getPeakMemoryBytes()).isEqualTo(8249673);
        assertThat(execution.getPrestoQueryStats().get().getPhysicalInputBytes()).isEqualTo(64852);
        assertThat(execution.getPrestoQueryStats().get().getPhysicalWrittenBytes()).isEqualTo(12);
        assertThat(execution.getPrestoQueryStats().get().getInternalNetworkInputBytes()).isEqualTo(82497);
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
        return new QueryExecution(mock(Benchmark.class), mock(Query.class), 0, new SqlStatementGenerator()
        {
            @Override
            public List<String> generateQuerySqlStatement(Query query, Map<String, ?> attributes)
            {
                return Collections.singletonList(query.getSqlTemplate());
            }
        });
    }
}
