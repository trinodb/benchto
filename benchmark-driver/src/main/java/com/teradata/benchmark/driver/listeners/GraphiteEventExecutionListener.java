/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.graphite.GraphiteClient;
import com.teradata.benchmark.driver.graphite.GraphiteClient.GraphiteEventRequest;
import com.teradata.benchmark.driver.graphite.GraphiteClient.GraphiteEventRequest.GraphiteEventRequestBuilder;
import com.teradata.benchmark.driver.sql.QueryExecution;
import com.teradata.benchmark.driver.sql.QueryExecutionResult;
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
    public void benchmarkStarted(Benchmark benchmark)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s started", benchmark.getName()))
                .tags("benchmark", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void benchmarkFinished(BenchmarkResult benchmarkResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s ended", benchmarkResult.getBenchmark().getName()))
                .tags("benchmark ended")
                .data(format("successful %b, mean: %f.2, stdDev: %f.2", benchmarkResult.isSuccessful(),
                        benchmarkResult.getDurationStatistics().getMean(),
                        benchmarkResult.getDurationStatistics().getStandardDeviation()))
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Execution %s-%d started", execution.getQuery().getName(), execution.getRun()))
                .tags("execution", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Execution %s-%d ended", executionResult.getQuery().getName(), executionResult.getQueryExecution().getRun()))
                .tags("execution", "ended")
                .data(format("duration: %d ms", executionResult.getQueryDuration().toMillis()))
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void suiteFinished(List<BenchmarkResult> queryResults)
    {
        // DO NOTHING
    }
}
