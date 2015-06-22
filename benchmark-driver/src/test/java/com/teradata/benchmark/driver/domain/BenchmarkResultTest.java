/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.domain;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.domain.BenchmarkResult.BenchmarkResultBuilder;
import org.junit.Test;

import static com.teradata.benchmark.driver.utils.TimeUtils.sleep;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class BenchmarkResultTest
{

    @Test
    public void testBuilder_successful_run()
            throws InterruptedException
    {
        BenchmarkResultBuilder builder = new BenchmarkResultBuilder(new Benchmark("name", "sequenceId", "datasource", "environment", ImmutableList.of(), 1, 1, emptyMap()));

        builder.startTimer();
        sleep(500, MILLISECONDS);
        builder.endTimer();

        BenchmarkResult benchmarkResult = builder.setExecutions(ImmutableList.of()).build();

        assertThat(benchmarkResult.isSuccessful()).isTrue();
        assertThat(benchmarkResult.getQueryDuration().toMillis()).isBetween(500L, 600L);
    }
}
