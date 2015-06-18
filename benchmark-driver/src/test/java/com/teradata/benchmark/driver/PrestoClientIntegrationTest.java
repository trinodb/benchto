/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.teradata.benchmark.driver.presto.PrestoClient;
import com.teradata.benchmark.driver.service.Measurement;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

import static com.teradata.benchmark.driver.service.Measurement.measurement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class PrestoClientIntegrationTest
        extends IntegrationTest
{
    @Autowired
    private PrestoClient prestoClient;

    @Test
    public void testPrestoClientLoadMetrics()
            throws IOException
    {
        String response = Resources.toString(Resources.getResource("json/presto_query_info_response.json"), Charsets.UTF_8);
        restServiceServer.expect(requestTo("http://presto-test-master:8090/v1/query/test_query_id")).andRespond(withSuccess(response, APPLICATION_JSON));

        List<Measurement> measurements = prestoClient.loadMetrics("test_query_id");

        assertThat(measurements).containsExactly(
                measurement("prestoQuery-totalPlanningTime", "MILLISECONDS", 24.72),
                measurement("prestoQuery-totalMemoryReservation", "BYTES", 0.0),
                measurement("prestoQuery-totalScheduledTime", "MILLISECONDS", 66000.0),
                measurement("prestoQuery-totalCpuTime", "MILLISECONDS", 63600.0),
                measurement("prestoQuery-totalUserTime", "MILLISECONDS", 3744000.0),
                measurement("prestoQuery-totalBlockedTime", "MILLISECONDS", 287400.0),
                measurement("prestoQuery-rawInputDataSize", "BYTES", 1.34E9),
                measurement("prestoQuery-processedInputDataSize", "BYTES", 7.3961E8),
                measurement("prestoQuery-outputDataSize", "BYTES", 6900.0)
        );

        restServiceServer.verify();
    }
}
