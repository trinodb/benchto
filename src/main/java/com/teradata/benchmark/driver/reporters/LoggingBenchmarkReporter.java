/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.reporters;

import com.teradata.benchmark.driver.BenchmarkQuery;
import com.teradata.benchmark.driver.BenchmarkQueryResult;
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
public class LoggingBenchmarkReporter
        implements BenchmarkResultReporter
{

    private static final Logger LOG = LoggerFactory.getLogger(LoggingBenchmarkReporter.class);

    @Override
    public void reportQueryExecution(BenchmarkQuery benchmarkQuery, QueryExecution queryExecution)
    {
        if (queryExecution.isSuccessful()) {
            LOG.trace("Query: {}, rows count: {}, duration: {}", benchmarkQuery.getName(), queryExecution.getRowsCount(), queryExecution.getQueryDuration());
        }
        else {
            LOG.error("Query: {}, execution error: {}", benchmarkQuery.getName(), queryExecution.getFailureCause().getMessage());
        }
    }

    @Override
    public void reportQueryResult(BenchmarkQueryResult result)
    {
        // Report at the end in benchmark result reporting
    }

    @Override
    public void reportBenchmarkResult(BenchmarkResult benchmarkResult)
    {
        List<BenchmarkQueryResult> successful = benchmarkResult.queryResults().stream()
                .filter(BenchmarkQueryResult::isSuccessful)
                .collect(toList());
        List<BenchmarkQueryResult> failed = newArrayList(benchmarkResult.queryResults());
        failed.removeAll(successful);

        if (!failed.isEmpty()) {
            StringBuilder failedQueriesMessage = new StringBuilder("\nFailed queries:\n");
            failed.stream().forEach(query -> failedQueriesMessage.append(query.getQuery().getName())
                    .append(" - ")
                    .append(query.getQuery().getSql()));
            failedQueriesMessage.append('\n');
            System.err.println(failedQueriesMessage.toString());
        }

        System.out.println(format("\nTests run: %d, Failures: %d\n", benchmarkResult.queryResults().size(), failed.size()));

        for (BenchmarkQueryResult result : successful) {
            System.out.println(format("Query %s - min: %f, max: %f, mean: %f, std-dev: %f",
                    result.getQuery().getName(),
                    result.getDurationStatistics().getMin(),
                    result.getDurationStatistics().getMax(),
                    result.getDurationStatistics().getMean(),
                    result.getDurationStatistics().getStandardDeviation()));
        }
    }
}
