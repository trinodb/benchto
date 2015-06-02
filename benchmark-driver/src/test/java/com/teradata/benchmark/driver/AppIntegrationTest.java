/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@IntegrationTest({"executionSequenceId=BEN_SEQ_ID", "runs=2"})
public class AppIntegrationTest
{

    @Autowired
    private BenchmarkDriver benchmarkDriver;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer restServiceServer;

    @Before
    public void initializeRestServiceServer()
    {
        restServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

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
