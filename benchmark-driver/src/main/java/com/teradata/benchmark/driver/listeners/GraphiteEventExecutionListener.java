/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.graphite.GraphiteClient;
import com.teradata.benchmark.driver.graphite.GraphiteClient.GraphiteEventRequest;
import com.teradata.benchmark.driver.graphite.GraphiteClient.GraphiteEventRequest.GraphiteEventRequestBuilder;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;

@Component
@ConditionalOnProperty(prefix = "graphite", name = "url")
public class GraphiteEventExecutionListener
        implements BenchmarkExecutionListener
{

    @Autowired
    private GraphiteClient graphiteClient;

    @Override
    public void benchmarkStarted(Query benchmarkQuery)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s started", benchmarkQuery.getName()))
                .tags("benchmark", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void benchmarkFinished(BenchmarkResult benchmarkResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s ended", benchmarkResult.getQuery().getName()))
                .tags("benchmark ended")
                .data(format("successful %b, mean: %f.2, stdDev: %f.2", benchmarkResult.isSuccessful(),
                        benchmarkResult.getDurationStatistics().getMean(),
                        benchmarkResult.getDurationStatistics().getStandardDeviation()))
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionStarted(Query benchmarkQuery, int run)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Execution %s-%d started", benchmarkQuery.getName(), run))
                .tags("execution", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionFinished(Query benchmarkQuery, int run, QueryExecution queryExecution)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Execution %s-%d ended", benchmarkQuery.getName(), run))
                .tags("execution", "ended")
                .data(format("duration: %d ms", queryExecution.getQueryDuration().toMillis()))
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void suiteFinished(List<BenchmarkResult> queryResults)
    {
        // DO NOTHING
    }
}
