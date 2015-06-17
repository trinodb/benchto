/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.domain.Benchmark;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.Measurable;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import com.teradata.benchmark.driver.graphite.GraphiteMetricsLoader;
import com.teradata.benchmark.driver.presto.PrestoClient;
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

import static com.google.common.collect.Iterables.getFirst;
import static com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.Status.ENDED;
import static com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.Status.FAILED;
import static com.teradata.benchmark.driver.service.Measurement.measurement;
import static com.teradata.benchmark.driver.utils.ExceptionUtils.stackTraceToString;

@Component
public class BenchmarkServiceExecutionListener
        implements BenchmarkExecutionListener
{

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Autowired
    private BenchmarkProperties benchmarkProperties;

    @Autowired
    private GraphiteMetricsLoader graphiteMetricsLoader;

    @Autowired
    private PrestoClient prestoClient;

    @Autowired
    private BenchmarkServiceClient benchmarkServiceClient;

    @Override
    public void benchmarkStarted(Benchmark benchmark)
    {
        BenchmarkStartRequestBuilder requestBuilder = new BenchmarkStartRequestBuilder()
                .environmentName(benchmarkProperties.getEnvironmentName());

        if (benchmark.getQueries().size() == 1) {
            requestBuilder.addAttribute("sqlStatement", getFirst(benchmark.getQueries(), null).getSql());
        }

        benchmarkServiceClient.startBenchmark(benchmark.getName(), benchmarkSequenceId(), requestBuilder.build());
    }

    @Override
    public void benchmarkFinished(BenchmarkResult benchmarkResult)
    {
        FinishRequestBuilder finishRequestBuilder = new FinishRequestBuilder()
                .withStatus(benchmarkResult.isSuccessful() ? ENDED : FAILED);

        if (benchmarkResult.getBenchmark().isConcurrent()) {
            addGraphiteMeasurements(benchmarkResult, finishRequestBuilder);
            finishRequestBuilder.addMeasurement(measurement("throughput", "QUERY_PER_SECOND", calculateThroughput(benchmarkResult)));
        }
        finishRequestBuilder.addMeasurement(measurement("duration", "MILLISECONDS", benchmarkResult.getQueryDuration().toMillis()));

        benchmarkServiceClient.finishBenchmark(benchmarkResult.getBenchmark().getName(), benchmarkSequenceId(), finishRequestBuilder.build());
    }

    private double calculateThroughput(BenchmarkResult benchmarkResult)
    {
        long durationInMillis = benchmarkResult.getQueryDuration().toMillis();
        return (double) benchmarkResult.getExecutions().size() / (durationInMillis * 1000);
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        ExecutionStartRequest request = new ExecutionStartRequestBuilder()
                .build();

        benchmarkServiceClient.startExecution(execution.getBenchmark().getName(), benchmarkSequenceId(), executionSequenceId(execution), request);
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {

        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(executionResult.isSuccessful() ? ENDED : FAILED)
                .addMeasurement(measurement("duration", "MILLISECONDS", executionResult.getQueryDuration().toMillis()));

        if (executionResult.getBenchmark().isSerial()) {
            addGraphiteMeasurements(executionResult, requestBuilder);
        }

        if (executionResult.getPrestoQueryId().isPresent()) {
            requestBuilder.addAttribute("prestoQueryId", executionResult.getPrestoQueryId().get());
            requestBuilder.addMeasurements(prestoClient.loadMetrics(executionResult.getPrestoQueryId().get()));
        }

        if (!executionResult.isSuccessful()) {
            requestBuilder.addAttribute("failureMessage", executionResult.getFailureCause().getMessage());
            requestBuilder.addAttribute("failureStackTrace", stackTraceToString(executionResult));

            if (executionResult.getFailureCause() instanceof SQLException) {
                requestBuilder.addAttribute("failureSQLErrorCode", "" + ((SQLException) executionResult.getFailureCause()).getErrorCode());
            }
        }

        benchmarkServiceClient.finishExecution(executionResult.getBenchmark().getName(), benchmarkSequenceId(),
                executionSequenceId(executionResult.getQueryExecution()), requestBuilder.build());
    }

    private void addGraphiteMeasurements(Measurable measurable, FinishRequestBuilder requestBuilder)
    {
        List<Measurement> graphiteMeasurements = graphiteMetricsLoader.loadMetrics(measurable.getUtcStart(), measurable.getUtcEnd());
        requestBuilder.addMeasurements(graphiteMeasurements);
    }

    @Override
    public void suiteFinished(List<BenchmarkResult> queryResults)
    {
        // DO NOTHING
    }

    private String benchmarkSequenceId()
    {
        return benchmarkProperties.getExecutionSequenceId();
    }

    private String executionSequenceId(QueryExecution execution)
    {
        return "" + execution.getRun();
    }
}
