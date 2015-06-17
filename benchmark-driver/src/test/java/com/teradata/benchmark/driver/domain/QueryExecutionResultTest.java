/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.domain;

import com.teradata.benchmark.driver.domain.QueryExecutionResult.QueryExecutionResultBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryExecutionResultTest
{

    @Test
    public void testBuilder_successful_run()
            throws InterruptedException
    {
        QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResultBuilder(new QueryExecution(null, null, 0))
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
        QueryExecutionResultBuilder queryExecutionResultBuilder = new QueryExecutionResultBuilder(new QueryExecution(null, null, 0));

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
}
