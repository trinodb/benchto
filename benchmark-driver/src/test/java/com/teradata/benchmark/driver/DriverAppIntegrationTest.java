/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionDriver;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;
import com.teradata.benchmark.driver.macro.MacroService;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.RequestMatcher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class DriverAppIntegrationTest
        extends IntegrationTest
{
    private static final String GRAPHITE_METRICS_RESPONSE = "[" +
            "{\"target\":\"cpu\",\"datapoints\":[[10, 10],[10, 10]]}" +
            ",{\"target\":\"memory\",\"datapoints\":[[10, 10],[10, 10]]}" +
            ",{\"target\":\"network\",\"datapoints\":[[10, 10],[10, 10]]}" +
            ",{\"target\":\"network_total\",\"datapoints\":[[10, 10],[10, 10]]}" +
            "]";
    private static final List<String> GRAPHITE_MEASUREMENT_NAMES = ImmutableList.of(
            "cluster-memory_max", "cluster-memory_mean", "cluster-cpu_max", "cluster-cpu_mean", "cluster-network_max", "cluster-network_mean", "cluster-network_total");

    private static final String TEST_QUERY = "SELECT 1\nFROM \"INFORMATION_SCHEMA\".SYSTEM_USERS\n";

    private static final Matcher<String> ENDED_STATUS_MATCHER = is("ENDED");

    @Autowired
    private BenchmarkExecutionDriver benchmarkExecutionDriver;

    @Autowired
    private BenchmarkProperties benchmarkProperties;

    @Autowired
    @Qualifier("prewarmStatusReporter")
    private BenchmarkStatusReporter prewarmStatusReporter;

    @Autowired
    private MacroService macroService;

    @Test
    public void simpleSelectBenchmark()
    {
        setBenchmark("simple_select_benchmark");
        verifyBenchmarkStart("simple_select_benchmark_schema=INFORMATION_SCHEMA_env=TEST_ENV", TEST_QUERY);
        verifySerialExecution("simple_select_benchmark_schema=INFORMATION_SCHEMA_env=TEST_ENV", "simple_select", 0);
        verifyBenchmarkFinish("simple_select_benchmark_schema=INFORMATION_SCHEMA_env=TEST_ENV", ImmutableList.of());
        verifyComplete();

        verify(prewarmStatusReporter, times(1)).reportBenchmarkStarted(any());
        verify(prewarmStatusReporter, times(1)).reportExecutionStarted(any());
        verify(prewarmStatusReporter, times(1)).reportExecutionFinished(any());
        verify(prewarmStatusReporter, times(1)).reportBenchmarkFinished(any());

        verifyNoMoreInteractions(prewarmStatusReporter);
    }

    @Test
    public void testBenchmark()
    {
        setBenchmark("test_benchmark");
        verifyBenchmarkStart("test_benchmark_env=TEST_ENV", TEST_QUERY);
        verifySerialExecution("test_benchmark_env=TEST_ENV", "test_query", 0);
        verifySerialExecution("test_benchmark_env=TEST_ENV", "test_query", 1);
        verifyBenchmarkFinish("test_benchmark_env=TEST_ENV", ImmutableList.of());
        verifyComplete();
    }

    @Test
    public void testConcurrentBenchmark()
    {
        ImmutableList<String> concurrentQueryMeasurementName = ImmutableList.of("duration");
        ImmutableList<String> concurrentBenchmarkMeasurementNames = ImmutableList.<String>builder()
                .addAll(GRAPHITE_MEASUREMENT_NAMES)
                .add("throughput")
                .add("duration")
                .build();

        setBenchmark("test_concurrent_benchmark");

        verifyBenchmarkStart("test_concurrent_benchmark_env=TEST_ENV", TEST_QUERY);
        verifyExecutionStarted("test_concurrent_benchmark_env=TEST_ENV", 0);
        verifyExecutionFinished("test_concurrent_benchmark_env=TEST_ENV", 0, concurrentQueryMeasurementName);
        verifyExecutionStarted("test_concurrent_benchmark_env=TEST_ENV", 1);
        verifyExecutionFinished("test_concurrent_benchmark_env=TEST_ENV", 1, concurrentQueryMeasurementName);
        verifyGetGraphiteMeasurements();
        verifyBenchmarkFinish("test_concurrent_benchmark_env=TEST_ENV", concurrentBenchmarkMeasurementNames);
        verifyComplete();
    }

    private void setBenchmark(String s)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "activeBenchmarks", s);
    }

    private void verifyBenchmarkStart(String benchmarkName, String sql)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/start"),
                method(HttpMethod.POST),
                jsonPath("$.environmentName", is("TEST_ENV")),
                jsonPath("$.attributes.sqlStatement", is(sql))
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + benchmarkName + " started")),
                jsonPath("$.tags", is("benchmark started")),
                jsonPath("$.data", is(""))
        )).andRespond(withSuccess());
    }

    private void verifyBenchmarkFinish(String benchmarkName, List<String> measurementNames)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", ENDED_STATUS_MATCHER),
                jsonPath("$.measurements.[*].name", containsInAnyOrder(measurementNames.toArray()))
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + benchmarkName + " ended")),
                jsonPath("$.tags", is("benchmark ended")),
                jsonPath("$.data", startsWith("successful"))
        )).andRespond(withSuccess());
    }

    private void verifySerialExecution(String benchmarkName, String queryName, int executionNumber)
    {
        ImmutableList<String> serialQueryMeasurementNames = ImmutableList.<String>builder()
                .addAll(GRAPHITE_MEASUREMENT_NAMES)
                .add("duration")
                .build();
        verifySerialExecutionStarted(benchmarkName, queryName, executionNumber);
        verifyGetGraphiteMeasurements();
        verifySerialExecutionFinished(benchmarkName, queryName, executionNumber, serialQueryMeasurementNames);
    }

    private void verifySerialExecutionStarted(String benchmarkName, String queryName, int executionNumber)
    {
        verifyExecutionStarted(benchmarkName, executionNumber);

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + queryName + ", execution " + executionNumber + " started")),
                jsonPath("$.tags", is("execution started")),
                jsonPath("$.data", is(""))
        )).andRespond(withSuccess());
    }

    private void verifyExecutionStarted(String benchmarkName, int executionNumber)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/execution/" + executionNumber + "/start"),
                method(HttpMethod.POST)
        )).andRespond(withSuccess());
    }

    private void verifySerialExecutionFinished(String benchmarkName, String queryName, int executionNumber, List<String> measurementNames)
    {
        verifyExecutionFinished(benchmarkName, executionNumber, measurementNames);

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + queryName + ", execution " + executionNumber + " ended")),
                jsonPath("$.tags", is("execution ended")),
                jsonPath("$.data", startsWith("duration: "))
        )).andRespond(withSuccess());
    }

    private void verifyExecutionFinished(String benchmarkName, int executionNumber, List<String> measurementNames)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/execution/" + executionNumber + "/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", ENDED_STATUS_MATCHER),
                jsonPath("$.measurements.[*].name", containsInAnyOrder(measurementNames.toArray()))
        )).andRespond(withSuccess());
    }

    private void verifyGetGraphiteMeasurements()
    {
        restServiceServer.expect(matchAll(
                requestTo(startsWith("http://graphite:18088/render?format=json")),
                requestTo(containsString("&target=alias(TARGET_CPU,'cpu')")),
                requestTo(containsString("&target=alias(TARGET_MEMORY,'memory')")),
                requestTo(containsString("&target=alias(TARGET_NETWORK,'network')")),
                requestTo(containsString("&target=alias(integral(TARGET_NETWORK),'network_total')")),
                method(HttpMethod.GET)
        )).andRespond(withSuccess().contentType(APPLICATION_JSON).body(GRAPHITE_METRICS_RESPONSE));
    }

    private void verifyComplete()
    {
        boolean successful = benchmarkExecutionDriver.run();
        assertThat(successful).isTrue();

        verify(macroService, times(1)).runBenchmarkMacros(eq(ImmutableList.of("no-op", "test_query.sql")), any(Benchmark.class));
    }

    private RequestMatcher matchAll(RequestMatcher... matchers)
    {
        return request -> {
            for (RequestMatcher matcher : matchers) {
                matcher.match(request);
            }
        };
    }
}
