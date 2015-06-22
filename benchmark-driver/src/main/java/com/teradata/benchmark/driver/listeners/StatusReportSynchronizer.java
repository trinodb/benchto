/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.domain.Benchmark;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.teradata.benchmark.driver.utils.TimeUtils.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Status reporting is performed asynchronously, and in some cases of dependency between time and services
 * it could be required to synchronize.
 * Currently it is a case with graphite, where we need to wait, to make sure that there are metrics store in graphite.
 */
@Component
public class StatusReportSynchronizer
{

    @Autowired
    private BenchmarkProperties properties;

    public void synchronizeBenchmarkStart(Benchmark benchmark)
    {
        // DO NOTHING
    }

    public void synchronizeBenchmarkFinish(BenchmarkResult result)
    {
        if (result.getBenchmark().isConcurrent()) {
            sleepBeforeExecutingReport();
        }
    }

    public void synchronizeExecutionStart(QueryExecution queryExecution)
    {
        // DO NOTHING
    }

    public void synchronizeExecutionFinish(QueryExecutionResult execution)
    {
        if (execution.getBenchmark().isSerial()) {
            sleepBeforeExecutingReport();
        }
    }

    public void synchronizeSuiteFinish(List<BenchmarkResult> benchmarkResults)
    {
        // DO NOTHING
    }

    private void sleepBeforeExecutingReport()
    {
        Optional<Integer> waitSecondsBeforeExecutionReporting = properties.getGraphiteProperties().waitSecondsBeforeExecutionReporting();

        if (waitSecondsBeforeExecutionReporting.isPresent()) {
            sleep(waitSecondsBeforeExecutionReporting.get(), SECONDS);
        }
    }
}
