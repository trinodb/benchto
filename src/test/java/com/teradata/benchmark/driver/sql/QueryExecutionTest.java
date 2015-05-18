/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.sql;

import com.teradata.benchmark.driver.sql.QueryExecution.QueryExecutionBuilder;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryExecutionTest
{

    @Test
    public void testBuilder_successful_run()
            throws InterruptedException
    {
        QueryExecutionBuilder queryExecutionBuilder = new QueryExecutionBuilder()
                .setRowsCount(100);

        queryExecutionBuilder.startTimer();
        TimeUnit.MILLISECONDS.sleep(500L);
        queryExecutionBuilder.endTimer();

        QueryExecution execution = queryExecutionBuilder.build();

        assertThat(execution.isSuccessful()).isTrue();
        assertThat(execution.getRowsCount()).isEqualTo(100);
        assertThat(execution.getQueryDuration().toMillis()).isBetween(500L, 600L);
    }

    @Test
    public void testBuilder_failed_run()
            throws InterruptedException
    {
        QueryExecutionBuilder queryExecutionBuilder = new QueryExecutionBuilder();

        queryExecutionBuilder.startTimer();
        TimeUnit.MILLISECONDS.sleep(500L);
        queryExecutionBuilder.failed(new NullPointerException());
        queryExecutionBuilder.endTimer();

        QueryExecution execution = queryExecutionBuilder.build();

        assertThat(execution.isSuccessful()).isFalse();
        assertThat(execution.getRowsCount()).isEqualTo(0);
        assertThat(execution.getFailureCause().getClass()).isEqualTo(NullPointerException.class);
        assertThat(execution.getQueryDuration().toMillis()).isBetween(500L, 600L);
    }
}
