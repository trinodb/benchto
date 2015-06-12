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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class AppIntegrationTest
        extends IntegrationTest
{

    @Autowired
    private BenchmarkDriver benchmarkDriver;

    @Test
    public void benchmarkTestQuery()
    {
        // benchmark start
        restServiceServer.expect(matchAll(
                requestTo("http://localhost:8080/v1/benchmark/test_query/BEN_SEQ_ID/start"),
                method(HttpMethod.POST),
                jsonPath("$.environmentName", is("TEST_ENV")),
                jsonPath("$.attributes.sqlStatement", is("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS"))
        )).andRespond(withSuccess());

        // first execution
        restServiceServer.expect(matchAll(
                requestTo("http://localhost:8080/v1/benchmark/test_query/BEN_SEQ_ID/execution/0/start"),
                method(HttpMethod.POST)
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://localhost:8080/v1/benchmark/test_query/BEN_SEQ_ID/execution/0/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", is("ENDED")),
                jsonPath("$.measurements.[*].name", containsInAnyOrder("duration"))
        )).andRespond(withSuccess());

        // second execution
        restServiceServer.expect(matchAll(
                requestTo("http://localhost:8080/v1/benchmark/test_query/BEN_SEQ_ID/execution/1/start"),
                method(HttpMethod.POST)
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://localhost:8080/v1/benchmark/test_query/BEN_SEQ_ID/execution/1/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", is("ENDED")),
                jsonPath("$.measurements.[*].name", containsInAnyOrder("duration"))
        )).andRespond(withSuccess());

        // benchmark finished
        restServiceServer.expect(matchAll(
                requestTo("http://localhost:8080/v1/benchmark/test_query/BEN_SEQ_ID/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", is("ENDED"))
        )).andRespond(withSuccess());

        boolean successful = benchmarkDriver.run();

        assertThat(successful).isTrue();

        restServiceServer.verify();
    }

    public RequestMatcher matchAll(RequestMatcher... matchers)
    {
        return request -> {
            for (RequestMatcher matcher : matchers) {
                matcher.match(request);
            }
        };
    }
}
