/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.benchto.driver;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.trino.benchto.driver.presto.PrestoClient;
import io.trino.benchto.driver.service.Measurement;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        restServiceServer.expect(requestTo("http://presto-test-master:8090/v1/query/test_query_id"))
                .andRespond(withSuccess(response, APPLICATION_JSON));

        List<Measurement> measurements = prestoClient.loadMetrics("test_query_id");

        Map<String, String> attributes = Collections.singletonMap("scope", "prestoQuery");
        assertThat(measurements).containsExactly(
                Measurement.measurement("analysisTime", "MILLISECONDS", 21.07, attributes),
                Measurement.measurement("planningTime", "MILLISECONDS", 24.72, attributes),
                Measurement.measurement("totalScheduledTime", "MILLISECONDS", 66000.0, attributes),
                Measurement.measurement("totalCpuTime", "MILLISECONDS", 63600.0, attributes),
                Measurement.measurement("totalBlockedTime", "MILLISECONDS", 287400.0, attributes),
                Measurement.measurement("finishingTime", "MILLISECONDS", 69000.0, attributes),
                Measurement.measurement("rawInputDataSize", "BYTES", 1.34E9, attributes),
                Measurement.measurement("processedInputDataSize", "BYTES", 7.3961E8, attributes),
                Measurement.measurement("internalNetworkInputDataSize", "BYTES", 7.2961E8, attributes),
                Measurement.measurement("physicalInputDataSize", "BYTES", 1.35E9, attributes),
                Measurement.measurement("outputDataSize", "BYTES", 6900.0, attributes),
                Measurement.measurement("peakTotalMemoryReservation", "BYTES", 6800.0, attributes),
                Measurement.measurement("physicalWrittenDataSize", "BYTES", 462265065.0, attributes));

        restServiceServer.verify();
    }

    @Test
    public void testPrestoClientGetQueryInfo()
            throws IOException
    {
        String response = Resources.toString(Resources.getResource("json/presto_query_info_response.json"), Charsets.UTF_8);
        restServiceServer.expect(requestTo("http://presto-test-master:8090/v1/query/test_query_id"))
                .andRespond(withSuccess(response, APPLICATION_JSON));

        assertThat(prestoClient.getQueryInfo("test_query_id")).isEqualTo(response);

        restServiceServer.verify();
    }
}
