/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
public class AppIntegrationTest
{

    @Autowired
    private BenchmarkDriver benchmarkDriver;

    @Autowired
    private MockBenchmarkResultReporter mockResultReporter;

    @Test
    public void benchmarkTestQuery()
    {
        boolean successful = benchmarkDriver.run();

        assertThat(successful).isTrue();
        assertThat(mockResultReporter.capturedBenchmarkResult().containsFailedQueries()).isFalse();
        assertThat(mockResultReporter.capturedBenchmarkResult().queryResults()).hasSize(1);
        assertThat(mockResultReporter.capturedQueryExecutions()).hasSize(3); // default 3 samples
        assertThat(mockResultReporter.capturedBenchmarkQueryResults()).hasSize(1);

        BenchmarkQueryResult benchmarkQueryResult = mockResultReporter.capturedBenchmarkQueryResults().get(0);
        assertThat(benchmarkQueryResult.getQuery().getName()).isEqualTo("test_query");
        assertThat(benchmarkQueryResult.getQuery().getSql()).isEqualTo("SELECT CURRENT_DATE AS today, CURRENT_TIME AS now FROM (VALUES (0))");

        assertThat(benchmarkQueryResult.getDurationStatistics().getValues().length).isEqualTo(3); // default 3 samples
        assertThat(benchmarkQueryResult.getDurationStatistics()).isNotNull();
        assertThat(benchmarkQueryResult.getDurationStatistics().getMean()).isGreaterThan(1.0);
    }
}
