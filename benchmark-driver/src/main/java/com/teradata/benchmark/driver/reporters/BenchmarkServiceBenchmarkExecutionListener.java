/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.reporters;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.BenchmarkQuery;
import com.teradata.benchmark.driver.BenchmarkQueryResult;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.collect.Maps.newHashMap;
import static com.teradata.benchmark.driver.reporters.BenchmarkServiceBenchmarkExecutionListener.FinishRequest.Status.ENDED;
import static com.teradata.benchmark.driver.reporters.BenchmarkServiceBenchmarkExecutionListener.FinishRequest.Status.FAILED;
import static com.teradata.benchmark.driver.reporters.BenchmarkServiceBenchmarkExecutionListener.Measurement.measurement;
import static com.teradata.benchmark.driver.utils.ExceptionUtils.stackTraceToString;

@Component
public class BenchmarkServiceBenchmarkExecutionListener
        implements BenchmarkExecutionListener
{

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkServiceBenchmarkExecutionListener.class);

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BenchmarkProperties benchmarkProperties;

    @Override
    public void benchmarkStarted(BenchmarkQuery benchmarkQuery)
    {
        String benchmarkName = benchmarkQuery.getName();
        Map<String, String> requestParams = requestParams(benchmarkName);

        BenchmarkStartRequest request = new BenchmarkStartRequest();
        request.environmentName = benchmarkProperties.getEnvironmentName();
        request.attributes = ImmutableMap.of("sqlStatement", benchmarkQuery.getSql());

        LOG.info("Benchmark URL: {}/#/benchmark/{}/{}", serviceUrl, benchmarkName, benchmarkSequenceId());

        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", request, Object.class, requestParams);
    }

    @Override
    public void benchmarkFinished(BenchmarkQueryResult benchmarkQueryResult)
    {
        Map<String, String> requestParams = requestParams(benchmarkQueryResult.getQuery().getName());

        FinishRequest request = new FinishRequest();
        request.status = benchmarkQueryResult.isSuccessful() ? ENDED : FAILED;

        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", request, Object.class, requestParams);
    }

    @Override
    public void executionStarted(BenchmarkQuery benchmarkQuery, int run)
    {
        Map<String, String> requestParams = requestParams(benchmarkQuery.getName());
        requestParams.put("executionSequenceId", executionSequenceId(run));

        ExecutionStartRequest request = new ExecutionStartRequest();

        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", request, Object.class, requestParams);
    }

    @Override
    public void executionFinished(BenchmarkQuery benchmarkQuery, int run, QueryExecution queryExecution)
    {
        Map<String, String> requestParams = requestParams(benchmarkQuery.getName());
        requestParams.put("executionSequenceId", executionSequenceId(run));

        FinishRequest request = new FinishRequest();
        request.status = queryExecution.isSuccessful() ? ENDED : FAILED;
        request.measurements = ImmutableList.of(measurement("duration", "MILLISECONDS", queryExecution.getQueryDuration().toMillis()));
        request.attributes = newHashMap();

        if (queryExecution.getPrestoQueryId().isPresent()) {
            request.attributes.put("prestoQueryId", queryExecution.getPrestoQueryId().get());
        }

        if (!queryExecution.isSuccessful()) {
            request.attributes.put("failureMessage", queryExecution.getFailureCause().getMessage());
            request.attributes.put("failureStackTrace", stackTraceToString(queryExecution));

            if (queryExecution.getFailureCause() instanceof SQLException) {
                request.attributes.put("failureSQLErrorCode", "" + ((SQLException) queryExecution.getFailureCause()).getErrorCode());
            }
        }

        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", request, Object.class, requestParams);
    }

    @Override
    public void suiteFinished(List<BenchmarkQueryResult> queryResults)
    {
        // DO NOTHING
    }

    private Map<String, String> requestParams(String benchmarkName)
    {
        Map<String, String> params = newHashMap();
        params.put("serviceUrl", serviceUrl);
        params.put("benchmarkName", benchmarkName);
        params.put("benchmarkSequenceId", benchmarkSequenceId());
        return params;
    }

    private String benchmarkSequenceId()
    {
        return benchmarkProperties.getExecutionSequenceId();
    }

    private String executionSequenceId(int run)
    {
        return "" + run;
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class BenchmarkStartRequest
    {
        private String environmentName;
        private Map<String, String> attributes;
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class ExecutionStartRequest
    {
        private Map<String, String> attributes;
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class FinishRequest
    {
        private Status status;
        private List<Measurement> measurements;
        private Map<String, String> attributes;

        public enum Status
        {
            STARTED, ENDED, FAILED
        }
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class Measurement
    {
        private String name;
        private String unit;
        private double value;

        public static Measurement measurement(String name, String unit, double value)
        {
            Measurement measurement = new Measurement();
            measurement.name = name;
            measurement.unit = unit;
            measurement.value = value;
            return measurement;
        }
    }
}
