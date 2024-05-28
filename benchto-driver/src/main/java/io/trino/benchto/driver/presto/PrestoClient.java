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
package io.trino.benchto.driver.presto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableMap;
import io.trino.benchto.driver.BenchmarkProperties;
import io.trino.benchto.driver.service.Measurement;
import io.trino.benchto.driver.utils.UnitConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.measure.unit.Unit;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkState;
import static io.trino.benchto.driver.service.Measurement.measurement;
import static java.util.stream.Collectors.toList;
import static javax.measure.unit.NonSI.BYTE;
import static javax.measure.unit.SI.MILLI;
import static javax.measure.unit.SI.SECOND;

@Component
@ConditionalOnProperty(prefix = "presto", value = "url")
public class PrestoClient
{
    private static final Map<String, Unit> DEFAULT_METRICS = ImmutableMap.<String, Unit>builder()
            .put("planningTime", MILLI(SECOND))
            .put("analysisTime", MILLI(SECOND))
            .put("totalScheduledTime", MILLI(SECOND))
            .put("totalCpuTime", MILLI(SECOND))
            .put("totalBlockedTime", MILLI(SECOND))
            .put("finishingTime", MILLI(SECOND))
            .put("rawInputDataSize", BYTE)
            .put("physicalInputDataSize", BYTE)
            .put("processedInputDataSize", BYTE)
            .put("internalNetworkInputDataSize", BYTE)
            .put("outputDataSize", BYTE)
            .put("peakTotalMemoryReservation", BYTE)
            .put("physicalWrittenDataSize", BYTE)
            .build();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BenchmarkProperties properties;

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public List<Measurement> loadMetrics(String queryId)
    {
        return loadMetrics(queryId, DEFAULT_METRICS);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public String getQueryInfo(String queryId)
    {
        URI uri = buildQueryInfoURI(queryId);

        HttpHeaders headers = new HttpHeaders();
        properties.getPrestoUsername().ifPresent(username -> headers.set("X-Trino-User", username));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    private List<Measurement> loadMetrics(String queryId, Map<String, Unit> requiredStatistics)
    {
        URI uri = buildQueryInfoURI(queryId);

        HttpHeaders headers = new HttpHeaders();
        properties.getPrestoUsername().ifPresent(username -> headers.set("X-Trino-User", username));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<QueryInfoResponseItem> response = restTemplate.exchange(uri, HttpMethod.GET, entity, QueryInfoResponseItem.class);

        Map<String, Object> queryStats = response.getBody().getQueryStats();
        return queryStats.keySet()
                .stream()
                .filter(requiredStatistics::containsKey)
                .map(name -> parseQueryStatistic(name, queryStats.get(name), requiredStatistics.get(name)))
                .collect(toList());
    }

    private URI buildQueryInfoURI(String queryId)
    {
        checkState(!properties.getPrestoURL().isEmpty());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(properties.getPrestoURL())
                .pathSegment("v1", "query", queryId);

        return URI.create(uriBuilder.toUriString());
    }

    private Measurement parseQueryStatistic(String name, Object statistic, Unit requiredUnit)
    {
        double value = UnitConverter.parseValueAsUnit(statistic.toString(), requiredUnit);
        return measurement("prestoQuery-" + name, UnitConverter.format(requiredUnit), value);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public Optional<String> getQueryCompletionEvent(String queryId)
    {
        Optional<URI> uri = buildQueryCompletionEventURI(queryId);
        if (uri.isEmpty()) {
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        HttpEntity<String> response = restTemplate.exchange(uri.get(), HttpMethod.GET, entity, String.class);
        return Optional.of(response.getBody());
    }

    private Optional<URI> buildQueryCompletionEventURI(String queryId)
    {
        return properties.getPrestoHttpEventListenerURL()
                .map(baseUrl -> {
                    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                            .pathSegment("v1", "events", "completedQueries", "get", queryId);
                    return URI.create(uriBuilder.toUriString());
                });
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class QueryInfoResponseItem
    {
        private Map<String, Object> queryStats;

        Map<String, Object> getQueryStats()
        {
            return queryStats;
        }
    }
}
