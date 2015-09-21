/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.listeners;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import com.teradata.benchto.driver.graphite.GraphiteClient;
import com.teradata.benchto.driver.graphite.GraphiteClient.GraphiteEventRequest;
import com.teradata.benchto.driver.graphite.GraphiteClient.GraphiteEventRequest.GraphiteEventRequestBuilder;
import com.teradata.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
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
    public void benchmarkStarted(Benchmark benchmark)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s started", benchmark.getUniqueName()))
                .tags("benchmark", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s ended", benchmarkExecutionResult.getBenchmark().getUniqueName()))
                .tags("benchmark ended")
                .data(format("successful %b", benchmarkExecutionResult.isSuccessful()))
                .when(benchmarkExecutionResult.getUtcEnd())
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        if (execution.getBenchmark().isConcurrent()) {
            return;
        }

        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, query %s (%d) started", execution.getBenchmark().getUniqueName(), execution.getQueryName(), execution.getRun()))
                .tags("execution", "started")
                .build();

        graphiteClient.storeEvent(request);
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {
        if (executionResult.getBenchmark().isConcurrent()) {
            return;
        }

        QueryExecution queryExecution = executionResult.getQueryExecution();
        GraphiteEventRequest request = new GraphiteEventRequestBuilder()
                .what(format("Benchmark %s, query %s (%d) ended", queryExecution.getBenchmark().getUniqueName(), executionResult.getQueryName(), queryExecution.getRun()))
                .tags("execution", "ended")
                .data(format("duration: %d ms", executionResult.getQueryDuration().toMillis()))
                .when(executionResult.getUtcEnd())
                .build();

        graphiteClient.storeEvent(request);
    }
}
