/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.RequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
    private static final String[] MEASUREMENT_NAMES = new String[] {
            "duration", "memory_max", "memory_mean", "cpu_max", "cpu_mean", "network_max", "network_mean", "network_total"};

    @Autowired
    private BenchmarkDriver benchmarkDriver;

    @Test
    public void benchmarkTestQuery()
    {
        // simple_select benchmark start
        verifyBenchmarkStart("simple_select_benchmark", "SELECT 1\nFROM \"INFORMATION_SCHEMA\".SYSTEM_USERS\n");

        // first execution
        verifyExecution("simple_select_benchmark", "simple_select", 0, "ENDED");

        // simple_select benchmark end
        verifyBenchmarkFinish("simple_select_benchmark", "ENDED");

        // test_benchmark benchmark start
        verifyBenchmarkStart("test_benchmark", "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS\n");

        // first execution
        verifyExecution("test_benchmark", "test_query", 0, "ENDED");

        // second execution
        verifyExecution("test_benchmark", "test_query", 1, "ENDED");

        // test_benchmark benchmark finished
        verifyBenchmarkFinish("test_benchmark", "ENDED");

        boolean successful = benchmarkDriver.run();

        assertThat(successful).isTrue();

        restServiceServer.verify();
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

    private void verifyBenchmarkFinish(String benchmarkName, String status)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", is(status))
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + benchmarkName + " ended")),
                jsonPath("$.tags", is("benchmark ended")),
                jsonPath("$.data", startsWith("successful"))
        )).andRespond(withSuccess());
    }

    private void verifyExecution(String benchmarkName, String queryName, int executionNumber, String status)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/execution/" + executionNumber + "/start"),
                method(HttpMethod.POST)
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Execution " + queryName + "-" + executionNumber + " started")),
                jsonPath("$.tags", is("execution started")),
                jsonPath("$.data", is(""))
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo(startsWith("http://graphite:18088/render?format=json")),
                requestTo(containsString("&target=alias(TARGET_CPU,'cpu')")),
                requestTo(containsString("&target=alias(TARGET_MEMORY,'memory')")),
                requestTo(containsString("&target=alias(TARGET_NETWORK,'network')")),
                requestTo(containsString("&target=alias(integral(TARGET_NETWORK),'network_total')")),
                method(HttpMethod.GET)
        )).andRespond(withSuccess().contentType(APPLICATION_JSON).body(GRAPHITE_METRICS_RESPONSE));

        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/execution/" + executionNumber + "/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", is(status)),
                jsonPath("$.measurements.[*].name", containsInAnyOrder(MEASUREMENT_NAMES))
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Execution " + queryName + "-" + executionNumber + " ended")),
                jsonPath("$.tags", is("execution ended")),
                jsonPath("$.data", startsWith("duration: "))
        )).andRespond(withSuccess());
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
