/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.execution.BenchmarkExecution;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.execution.QueryExecution;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkExecutionListener;
import com.teradata.benchmark.driver.listeners.suite.SuiteExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Component
public class LoggingBenchmarkExecutionListener
        implements BenchmarkExecutionListener, SuiteExecutionListener
{

    private static final Logger LOG = LoggerFactory.getLogger(LoggingBenchmarkExecutionListener.class);

    @Override
    public void benchmarkStarted(BenchmarkExecution benchmarkExecution)
    {
        LOG.info("Executing benchmark: {}", benchmarkExecution.getBenchmarkName());
    }

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult result)
    {
        LOG.trace("Finished benchmark: {}", result.getBenchmarkExecution().getBenchmarkName());
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        LOG.trace("Query started: {} ({})", execution.getQueryName(), execution.getRun());
    }

    @Override
    public void executionFinished(QueryExecutionResult result)
    {
        if (result.isSuccessful()) {
            LOG.trace("Query finished: {} ({}), rows count: {}, duration: {}", result.getQueryName(), result.getQueryExecution().getRun(), result.getRowsCount(), result.getQueryDuration());
        }
        else {
            LOG.error("Query failed: {} ({}), execution error: {}", result.getQueryName(), result.getQueryExecution().getRun(), result.getFailureCause().getMessage());
        }
    }

    @Override
    public void suiteFinished(List<BenchmarkExecutionResult> benchmarkExecutionResults)
    {
        List<BenchmarkExecutionResult> successful = benchmarkExecutionResults.stream()
                .filter(BenchmarkExecutionResult::isSuccessful)
                .collect(toList());
        List<BenchmarkExecutionResult> failed = newArrayList(benchmarkExecutionResults);
        failed.removeAll(successful);

        if (!failed.isEmpty()) {
            StringBuilder failedQueriesMessage = new StringBuilder("\nFailed benchmark queries:\n");
            failed.stream()
                    .map(benchmarkResult -> benchmarkResult.getExecutions())
                    .flatMap(Collection::stream)
                    .forEach(queryExecution -> failedQueriesMessage.append(queryExecution.getBenchmarkExecution().getBenchmarkName())
                            .append(" - \n")
                            .append(queryExecution.getSql())
                            .append("\n-----------------------------\n"));
            failedQueriesMessage.append('\n');
            System.err.println(failedQueriesMessage.toString());
        }

        System.out.println(format("\nTests run: %d, Failures: %d\n", benchmarkExecutionResults.size(), failed.size()));

        for (BenchmarkExecutionResult result : successful) {
            System.out.println(format("Benchmark query duration times for %s - min: %f, max: %f, mean: %f, std-dev: %f",
                    result.getBenchmarkExecution().getBenchmarkName(),
                    result.getDurationStatistics().getMin(),
                    result.getDurationStatistics().getMax(),
                    result.getDurationStatistics().getMean(),
                    result.getDurationStatistics().getStandardDeviation()));
        }
    }
}
