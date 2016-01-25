/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.graphite;

import com.teradata.benchto.driver.IntegrationTest;
import com.teradata.benchto.driver.graphite.GraphiteClient.GraphiteEventRequest.GraphiteEventRequestBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpServerErrorException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class GraphiteClientRetryTest
        extends IntegrationTest
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private GraphiteClient graphiteClient;

    @Test
    public void testRetries_successful()
    {
        restServiceServer.expect(requestTo("http://graphite:18088/events/")).andRespond(withServerError()); // 1 attempt fail
        restServiceServer.expect(requestTo("http://graphite:18088/events/")).andRespond(withServerError()); // 2 attempt fail
        restServiceServer.expect(requestTo("http://graphite:18088/events/")).andRespond(withSuccess()); // 3 attempt success

        graphiteClient.storeEvent(new GraphiteEventRequestBuilder()
                .build());

        restServiceServer.verify();
    }

    @Test
    public void testRetries_exceeded()
    {
        expectedException.expect(HttpServerErrorException.class);

        restServiceServer.expect(requestTo("http://graphite:18088/events/")).andRespond(withServerError()); // 1 attempt fail
        restServiceServer.expect(requestTo("http://graphite:18088/events/")).andRespond(withServerError()); // 2 attempt fail
        restServiceServer.expect(requestTo("http://graphite:18088/events/")).andRespond(withServerError()); // 3 attempt fail

        graphiteClient.storeEvent(new GraphiteEventRequestBuilder()
                .build());
    }
}
