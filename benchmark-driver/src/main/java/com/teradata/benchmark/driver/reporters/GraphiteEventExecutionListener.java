/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.reporters;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.teradata.benchmark.driver.BenchmarkQuery;
import com.teradata.benchmark.driver.BenchmarkQueryResult;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static java.lang.String.format;

@Component
@ConditionalOnProperty(prefix = "graphite", name = "url")
public class GraphiteEventExecutionListener
        implements BenchmarkExecutionListener
{

    @Value("${graphite.url}")
    private String graphiteURL;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void benchmarkStarted(BenchmarkQuery benchmarkQuery)
    {
        GraphiteEventRequest request = new GraphiteEventRequest();
        request.what = format("Benchmark %s started", benchmarkQuery.getName());
        request.tags = "benchmark started";
        request.data = "";

        restTemplate.postForObject("{graphiteURL}/events/", request, Object.class, ImmutableMap.of("graphiteURL", graphiteURL));
    }

    @Override
    public void benchmarkFinished(BenchmarkQueryResult benchmarkQueryResult)
    {
        GraphiteEventRequest request = new GraphiteEventRequest();
        request.what = format("Benchmark %s ended", benchmarkQueryResult.getQuery().getName());
        request.tags = "benchmark ended";
        request.data = format("successful %b, mean: %f.2, stdDev: %f.2", benchmarkQueryResult.isSuccessful(),
                benchmarkQueryResult.getDurationStatistics().getMean(),
                benchmarkQueryResult.getDurationStatistics().getStandardDeviation());

        restTemplate.postForObject("{graphiteURL}/events/", request, Object.class, ImmutableMap.of("graphiteURL", graphiteURL));
    }

    @Override
    public void executionStarted(BenchmarkQuery benchmarkQuery, int run)
    {
        GraphiteEventRequest request = new GraphiteEventRequest();
        request.what = format("Execution %s-%d started", benchmarkQuery.getName(), run);
        request.tags = "execution started";
        request.data = "";

        restTemplate.postForObject("{graphiteURL}/events/", request, Object.class, ImmutableMap.of("graphiteURL", graphiteURL));
    }

    @Override
    public void executionFinished(BenchmarkQuery benchmarkQuery, int run, QueryExecution queryExecution)
    {
        GraphiteEventRequest request = new GraphiteEventRequest();
        request.what = format("Execution %s-%d ended", benchmarkQuery.getName(), run);
        request.tags = "execution ended";
        request.data = format("duration: %d ms", queryExecution.getQueryDuration().toMillis());

        restTemplate.postForObject("{graphiteURL}/events/", request, Object.class, ImmutableMap.of("graphiteURL", graphiteURL));
    }

    @Override
    public void suiteFinished(List<BenchmarkQueryResult> queryResults)
    {
        // DO NOTHING
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class GraphiteEventRequest
    {
        private String what;
        private String tags;
        private String data;
    }
}
