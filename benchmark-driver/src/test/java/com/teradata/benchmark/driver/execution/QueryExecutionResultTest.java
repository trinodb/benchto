/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.execution.QueryExecutionResult.QueryExecutionResultBuilder;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QueryExecutionResultTest
{

    @Test
    public void testBuilder_successful_run()
            throws InterruptedException
    {
        QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResultBuilder(queryExecution())
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
        QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResultBuilder(queryExecution());

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
        return new QueryExecution(mock(Benchmark.class), mock(Query.class), 0);
    }
}
