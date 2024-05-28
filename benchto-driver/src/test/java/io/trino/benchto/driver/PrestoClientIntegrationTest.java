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
import java.util.List;
import java.util.Optional;

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

        assertThat(measurements).containsExactly(
                Measurement.measurement("prestoQuery-analysisTime", "MILLISECONDS", 21.07),
                Measurement.measurement("prestoQuery-planningTime", "MILLISECONDS", 24.72),
                Measurement.measurement("prestoQuery-totalScheduledTime", "MILLISECONDS", 66000.0),
                Measurement.measurement("prestoQuery-totalCpuTime", "MILLISECONDS", 63600.0),
                Measurement.measurement("prestoQuery-totalBlockedTime", "MILLISECONDS", 287400.0),
                Measurement.measurement("prestoQuery-finishingTime", "MILLISECONDS", 69000.0),
                Measurement.measurement("prestoQuery-rawInputDataSize", "BYTES", 1.34E9),
                Measurement.measurement("prestoQuery-processedInputDataSize", "BYTES", 7.3961E8),
                Measurement.measurement("prestoQuery-internalNetworkInputDataSize", "BYTES", 7.2961E8),
                Measurement.measurement("prestoQuery-physicalInputDataSize", "BYTES", 1.35E9),
                Measurement.measurement("prestoQuery-outputDataSize", "BYTES", 6900.0),
                Measurement.measurement("prestoQuery-peakTotalMemoryReservation", "BYTES", 6800.0),
                Measurement.measurement("prestoQuery-physicalWrittenDataSize", "BYTES", 462265065.0));

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

    @Test
    public void testPrestoClientGetQueryCompletionEvent()
            throws IOException
    {
        String response = Resources.toString(Resources.getResource("json/presto_query_completion_event_response.json"), Charsets.UTF_8);
        restServiceServer.expect(requestTo("http://presto-test-master:8091/v1/events/completedQueries/get/test_query_id"))
                .andRespond(withSuccess(response, APPLICATION_JSON));

        assertThat(prestoClient.getQueryCompletionEvent("test_query_id")).isEqualTo(Optional.of(response));

        restServiceServer.verify();
    }
}
