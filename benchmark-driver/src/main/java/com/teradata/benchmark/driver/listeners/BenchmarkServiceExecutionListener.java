/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.teradata.benchmark.driver.execution.BenchmarkExecution;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.Measurable;
import com.teradata.benchmark.driver.execution.QueryExecution;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkExecutionListener;
import com.teradata.benchmark.driver.listeners.measurements.PostExecutionMeasurementProvider;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.BenchmarkStartRequest.BenchmarkStartRequestBuilder;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.ExecutionStartRequest;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.ExecutionStartRequest.ExecutionStartRequestBuilder;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.FinishRequestBuilder;
import com.teradata.benchmark.driver.service.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.getFirst;
import static com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.Status.ENDED;
import static com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.Status.FAILED;
import static com.teradata.benchmark.driver.utils.ExceptionUtils.stackTraceToString;

@Component
public class BenchmarkServiceExecutionListener
        implements BenchmarkExecutionListener
{

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Autowired
    private BenchmarkServiceClient benchmarkServiceClient;

    @Autowired
    private List<PostExecutionMeasurementProvider> measurementProviders;

    @Override
    public void benchmarkStarted(BenchmarkExecution benchmarkExecution)
    {
        BenchmarkStartRequestBuilder requestBuilder = new BenchmarkStartRequestBuilder()
                .environmentName(benchmarkExecution.getEnvironment());

        requestBuilder.addAttribute("dataSource", benchmarkExecution.getDataSource())
                .addAttribute("runs", "" + benchmarkExecution.getRuns())
                .addAttribute("concurrency", "" + benchmarkExecution.getConcurrency());
        for (Map.Entry<String, String> variableEntry : benchmarkExecution.getVariables().entrySet()) {
            requestBuilder.addAttribute(variableEntry.getKey(), variableEntry.getValue());
        }

        if (benchmarkExecution.getQueries().size() == 1) {
            requestBuilder.addAttribute("sqlStatement", getFirst(benchmarkExecution.getQueries(), null).getSql());
        }

        benchmarkServiceClient.startBenchmark(benchmarkExecution.getBenchmarkName(), benchmarkExecution.getSequenceId(), requestBuilder.build());
    }

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(benchmarkExecutionResult.isSuccessful() ? ENDED : FAILED)
                .addMeasurements(getMeasurements(benchmarkExecutionResult));

        benchmarkServiceClient.finishBenchmark(benchmarkExecutionResult.getBenchmarkExecution().getBenchmarkName(), benchmarkExecutionResult.getBenchmarkExecution().getSequenceId(), requestBuilder.build());
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        ExecutionStartRequest request = new ExecutionStartRequestBuilder()
                .build();

        benchmarkServiceClient.startExecution(execution.getBenchmarkExecution().getBenchmarkName(), execution.getBenchmarkExecution().getSequenceId(), executionSequenceId(execution), request);
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {
        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(executionResult.isSuccessful() ? ENDED : FAILED)
                .addMeasurements(getMeasurements(executionResult));

        if (executionResult.getPrestoQueryId().isPresent()) {
            requestBuilder.addAttribute("prestoQueryId", executionResult.getPrestoQueryId().get());
        }

        if (!executionResult.isSuccessful()) {
            requestBuilder.addAttribute("failureMessage", executionResult.getFailureCause().getMessage());
            requestBuilder.addAttribute("failureStackTrace", stackTraceToString(executionResult));

            if (executionResult.getFailureCause() instanceof SQLException) {
                requestBuilder.addAttribute("failureSQLErrorCode", "" + ((SQLException) executionResult.getFailureCause()).getErrorCode());
            }
        }

        benchmarkServiceClient.finishExecution(executionResult.getBenchmarkExecution().getBenchmarkName(), executionResult.getBenchmarkExecution().getSequenceId(),
                executionSequenceId(executionResult.getQueryExecution()), requestBuilder.build());
    }

    private List<Measurement> getMeasurements(Measurable measurable)
    {
        ImmutableList.Builder<Measurement> measurementsList = ImmutableList.builder();
        for (PostExecutionMeasurementProvider measurementProvider : measurementProviders) {
            measurementsList.addAll(measurementProvider.loadMeasurements(measurable));
        }
        return measurementsList.build();
    }

    private String executionSequenceId(QueryExecution execution)
    {
        return "" + execution.getRun();
    }
}
