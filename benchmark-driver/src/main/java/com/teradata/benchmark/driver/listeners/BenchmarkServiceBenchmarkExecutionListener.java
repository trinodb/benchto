/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.graphite.GraphiteMetricsLoader;
import com.teradata.benchmark.driver.presto.PrestoClient;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.BenchmarkStartRequest;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.BenchmarkStartRequest.BenchmarkStartRequestBuilder;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.ExecutionStartRequest;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.ExecutionStartRequest.ExecutionStartRequestBuilder;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest;
import com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.FinishRequestBuilder;
import com.teradata.benchmark.driver.service.Measurement;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

import static com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.Status.ENDED;
import static com.teradata.benchmark.driver.service.BenchmarkServiceClient.FinishRequest.Status.FAILED;
import static com.teradata.benchmark.driver.service.Measurement.measurement;
import static com.teradata.benchmark.driver.utils.ExceptionUtils.stackTraceToString;

@Component
public class BenchmarkServiceBenchmarkExecutionListener
        implements BenchmarkExecutionListener
{

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkServiceBenchmarkExecutionListener.class);

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
    public void benchmarkStarted(Query benchmarkQuery)
    {
        BenchmarkStartRequest request = new BenchmarkStartRequestBuilder()
                .environmentName(benchmarkProperties.getEnvironmentName())
                .addAttribute("sqlStatement", benchmarkQuery.getSql())
                .build();

        LOG.info("Benchmark URL: {}/#/benchmark/{}/{}", serviceUrl, benchmarkQuery.getName(), benchmarkSequenceId());

        benchmarkServiceClient.startBenchmark(benchmarkQuery.getName(), benchmarkSequenceId(), request);
    }

    @Override
    public void benchmarkFinished(BenchmarkResult benchmarkResult)
    {
        FinishRequest request = new FinishRequestBuilder()
                .withStatus(benchmarkResult.isSuccessful() ? ENDED : FAILED)
                .build();

        benchmarkServiceClient.finishBenchmark(benchmarkResult.getQuery().getName(), benchmarkSequenceId(), request);
    }

    @Override
    public void executionStarted(Query benchmarkQuery, int run)
    {
        ExecutionStartRequest request = new ExecutionStartRequestBuilder()
                .build();

        benchmarkServiceClient.startExecution(benchmarkQuery.getName(), benchmarkSequenceId(), executionSequenceId(run), request);
    }

    @Override
    public void executionFinished(Query benchmarkQuery, int run, QueryExecution queryExecution)
    {
        List<Measurement> graphiteMeasurements = graphiteMetricsLoader.loadMetrics(queryExecution.getStart(), queryExecution.getEnd());

        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(queryExecution.isSuccessful() ? ENDED : FAILED)
                .addMeasurement(measurement("duration", "MILLISECONDS", queryExecution.getQueryDuration().toMillis()))
                .addMeasurements(graphiteMeasurements);

        if (queryExecution.getPrestoQueryId().isPresent()) {
            requestBuilder.addAttribute("prestoQueryId", queryExecution.getPrestoQueryId().get());
            requestBuilder.addMeasurements(prestoClient.loadMetrics(queryExecution.getPrestoQueryId().get()));
        }

        if (!queryExecution.isSuccessful()) {
            requestBuilder.addAttribute("failureMessage", queryExecution.getFailureCause().getMessage());
            requestBuilder.addAttribute("failureStackTrace", stackTraceToString(queryExecution));

            if (queryExecution.getFailureCause() instanceof SQLException) {
                requestBuilder.addAttribute("failureSQLErrorCode", "" + ((SQLException) queryExecution.getFailureCause()).getErrorCode());
            }
        }

        benchmarkServiceClient.finishExecution(benchmarkQuery.getName(), benchmarkSequenceId(), executionSequenceId(run), requestBuilder.build());
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

    private String executionSequenceId(int run)
    {
        return "" + run;
    }
}
