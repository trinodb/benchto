/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.domain.Benchmark;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
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
        implements BenchmarkExecutionListener
{

    private static final Logger LOG = LoggerFactory.getLogger(LoggingBenchmarkExecutionListener.class);

    @Override
    public void benchmarkStarted(Benchmark benchmark)
    {
        LOG.info("Executing benchmark: {}", benchmark.getName());
    }

    @Override
    public void benchmarkFinished(BenchmarkResult result)
    {
        LOG.trace("Finished benchmark: {}", result.getBenchmark().getName());
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        LOG.trace("Query started: {} ({})", execution.getQuery().getName(), execution.getRun());
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
    public void suiteFinished(List<BenchmarkResult> benchmarkResults)
    {
        List<BenchmarkResult> successful = benchmarkResults.stream()
                .filter(BenchmarkResult::isSuccessful)
                .collect(toList());
        List<BenchmarkResult> failed = newArrayList(benchmarkResults);
        failed.removeAll(successful);

        if (!failed.isEmpty()) {
            StringBuilder failedQueriesMessage = new StringBuilder("\nFailed benchmark queries:\n");
            failed.stream()
                    .map(benchmarkResult -> benchmarkResult.getExecutions())
                    .flatMap(Collection::stream)
                    .forEach(queryExecution -> failedQueriesMessage.append(queryExecution.getBenchmark().getName())
                            .append(" - \n")
                            .append(queryExecution.getQuery().getSql())
                            .append("\n-----------------------------\n"));
            failedQueriesMessage.append('\n');
            System.err.println(failedQueriesMessage.toString());
        }

        System.out.println(format("\nTests run: %d, Failures: %d\n", benchmarkResults.size(), failed.size()));

        for (BenchmarkResult result : successful) {
            System.out.println(format("Benchmark query duration times for %s - min: %f, max: %f, mean: %f, std-dev: %f",
                    result.getBenchmark().getName(),
                    result.getDurationStatistics().getMin(),
                    result.getDurationStatistics().getMax(),
                    result.getDurationStatistics().getMean(),
                    result.getDurationStatistics().getStandardDeviation()));
        }
    }
}
