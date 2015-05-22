/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.reporters;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.BenchmarkQuery;
import com.teradata.benchmark.driver.BenchmarkQueryResult;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

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
        String benchmarkSequenceId = benchmarkSequenceId();

        LOG.info("Benchmark URL: {}/#/benchmark/{}/{}", serviceUrl, benchmarkName, benchmarkSequenceId);

        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", null, Object.class,
                ImmutableMap.of(
                        "serviceUrl", serviceUrl,
                        "benchmarkName", benchmarkName,
                        "benchmarkSequenceId", benchmarkSequenceId));
    }

    @Override
    public void benchmarkFinished(BenchmarkQueryResult benchmarkQueryResult)
    {
        DescriptiveStatistics durationStatistics = benchmarkQueryResult.getDurationStatistics();
        List<Measurement> measurements = ImmutableList.of(
                new Measurement("durationMean", "MILLISECONDS", durationStatistics.getMean()),
                new Measurement("durationMin", "MILLISECONDS", durationStatistics.getMin()),
                new Measurement("durationMax", "MILLISECONDS", durationStatistics.getMax()),
                new Measurement("durationStdDev", "MILLISECONDS", durationStatistics.getStandardDeviation()));

        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", measurements, Object.class,
                ImmutableMap.of(
                        "serviceUrl", serviceUrl,
                        "benchmarkName", benchmarkQueryResult.getQuery().getName(),
                        "benchmarkSequenceId", benchmarkSequenceId()));
    }

    @Override
    public void executionStarted(BenchmarkQuery benchmarkQuery, int run)
    {
        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", null, Object.class,
                ImmutableMap.of(
                        "serviceUrl", serviceUrl,
                        "benchmarkName", benchmarkQuery.getName(),
                        "benchmarkSequenceId", benchmarkSequenceId(),
                        "executionSequenceId", run));
    }

    @Override
    public void executionFinished(BenchmarkQuery benchmarkQuery, int run, QueryExecution queryExecution)
    {
        List<Measurement> measurements = ImmutableList.of(
                new Measurement("duration", "MILLISECONDS", queryExecution.getQueryDuration().toMillis()));

        restTemplate.postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", measurements, Object.class,
                ImmutableMap.of(
                        "serviceUrl", serviceUrl,
                        "benchmarkName", benchmarkQuery.getName(),
                        "benchmarkSequenceId", benchmarkSequenceId(),
                        "executionSequenceId", run));
    }

    @Override
    public void suiteFinished(List<BenchmarkQueryResult> queryResults)
    {
        // DO NOTHING
    }

    private String benchmarkSequenceId()
    {
        return benchmarkProperties.getExecutionSequenceId();
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class Measurement
    {
        private final String name;
        private final String unit;
        private final double value;

        public Measurement(String name, String unit, double value)
        {
            this.name = name;
            this.unit = unit;
            this.value = value;
        }
    }
}
