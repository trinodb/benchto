/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.execution.BenchmarkExecution;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.execution.QueryExecution;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;
import com.teradata.benchmark.driver.graphite.GraphiteClient;
import com.teradata.benchmark.driver.graphite.GraphiteClient.GraphiteEventRequest;
import com.teradata.benchmark.driver.graphite.GraphiteClient.GraphiteEventRequest.GraphiteEventRequestBuilder;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.graphite", value = "event.reporting.enabled")
public class GraphiteEventExecutionListener
        implements BenchmarkExecutionListener
{

    @Autowired
    private GraphiteClient graphiteClient;

    @Override
    public void benchmarkStarted(BenchmarkExecution benchmarkExecution)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s started", benchmarkExecution.getBenchmarkName()))
                .tags("benchmark", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s ended", benchmarkExecutionResult.getBenchmarkExecution().getBenchmarkName()))
                .tags("benchmark ended")
                .data(format("successful %b, mean: %f.2, stdDev: %f.2", benchmarkExecutionResult.isSuccessful(),
                        benchmarkExecutionResult.getDurationStatistics().getMean(),
                        benchmarkExecutionResult.getDurationStatistics().getStandardDeviation()))
                .when(benchmarkExecutionResult.getUtcEnd())
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        if (execution.getBenchmarkExecution().isConcurrent()) {
            return;
        }

        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, execution %d started", execution.getQueryName(), execution.getRun()))
                .tags("execution", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {
        if (executionResult.getBenchmarkExecution().isConcurrent()) {
            return;
        }

        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, execution %d ended", executionResult.getQueryName(), executionResult.getQueryExecution().getRun()))
                .tags("execution", "ended")
                .data(format("duration: %d ms", executionResult.getQueryDuration().toMillis()))
                .when(executionResult.getUtcEnd())
                .build();

        graphiteClient.storeEvent(request);
    }
}
