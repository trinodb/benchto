/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    public void benchmarkStarted(Query benchmarkQuery)
    {
        LOG.debug("Benchmarking query: {}", benchmarkQuery.getName());
    }

    @Override
    public void benchmarkFinished(BenchmarkResult result)
    {
        // Report at the end in benchmark result reporting
    }

    @Override
    public void executionStarted(Query benchmarkQuery, int run)
    {
        // Report at the end in benchmark result reporting
    }

    @Override
    public void executionFinished(Query benchmarkQuery, int run, QueryExecution queryExecution)
    {
        if (queryExecution.isSuccessful()) {
            LOG.trace("Query: {}, rows count: {}, duration: {}", benchmarkQuery.getName(), queryExecution.getRowsCount(), queryExecution.getQueryDuration());
        }
        else {
            LOG.error("Query: {}, execution error: {}", benchmarkQuery.getName(), queryExecution.getFailureCause().getMessage());
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
            StringBuilder failedQueriesMessage = new StringBuilder("\nFailed queries:\n");
            failed.stream().forEach(query -> failedQueriesMessage.append(query.getQuery().getName())
                    .append(" - \n")
                    .append(query.getQuery().getSql())
                    .append("\n-----------------------------\n"));
            failedQueriesMessage.append('\n');
            System.err.println(failedQueriesMessage.toString());
        }

        System.out.println(format("\nTests run: %d, Failures: %d\n", benchmarkResults.size(), failed.size()));

        for (BenchmarkResult result : successful) {
            System.out.println(format("Query %s - min: %f, max: %f, mean: %f, std-dev: %f",
                    result.getQuery().getName(),
                    result.getDurationStatistics().getMin(),
                    result.getDurationStatistics().getMax(),
                    result.getDurationStatistics().getMean(),
                    result.getDurationStatistics().getStandardDeviation()));
        }
    }
}
