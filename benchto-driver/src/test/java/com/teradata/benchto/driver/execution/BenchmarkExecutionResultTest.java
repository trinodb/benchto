/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.execution;

import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult.BenchmarkExecutionResultBuilder;
import org.junit.Test;

import static com.teradata.benchto.driver.utils.TimeUtils.sleep;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class BenchmarkExecutionResultTest

{
    @Test
    public void testBuilder_successful_run()
            throws InterruptedException
    {
        Benchmark benchmark = new Benchmark.BenchmarkBuilder("name", "sequenceId", emptyList())
                .withDataSource("datasource")
                .withEnvironment("environment")
                .withRuns(1)
                .withPrewarmRuns(0)
                .withConcurrency(1)
                .withBeforeBenchmarkMacros(emptyList())
                .withAfterBenchmarkMacros(emptyList())
                .withVariables(emptyMap())
                .createBenchmark();
        BenchmarkExecutionResultBuilder builder = new BenchmarkExecutionResultBuilder(benchmark);

        builder.startTimer();
        sleep(500, MILLISECONDS);
        builder.endTimer();

        BenchmarkExecutionResult benchmarkExecutionResult = builder.withExecutions(ImmutableList.of()).build();

        assertThat(benchmarkExecutionResult.isSuccessful()).isTrue();
        assertThat(benchmarkExecutionResult.getQueryDuration().toMillis()).isBetween(500L, 600L);
    }
}
