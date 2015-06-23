/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult.BenchmarkExecutionResultBuilder;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;
import org.junit.Test;

import static com.teradata.benchmark.driver.utils.TimeUtils.sleep;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BenchmarkExecutionResultTest

{
    @Test
    public void testBuilder_successful_run()
            throws InterruptedException
    {
        Benchmark benchmark = new Benchmark("name", "sequenceId", "datasource", "environment", ImmutableList.of(), 1, 0, 1, ImmutableList.of(), emptyMap());
        BenchmarkExecutionResultBuilder builder = new BenchmarkExecutionResultBuilder(new BenchmarkExecution(benchmark, mock(BenchmarkStatusReporter.class), 1, 0));

        builder.startTimer();
        sleep(500, MILLISECONDS);
        builder.endTimer();

        BenchmarkExecutionResult benchmarkExecutionResult = builder.setExecutions(ImmutableList.of()).build();

        assertThat(benchmarkExecutionResult.isSuccessful()).isTrue();
        assertThat(benchmarkExecutionResult.getQueryDuration().toMillis()).isBetween(500L, 600L);
    }
}
