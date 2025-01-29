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
import io.trino.benchto.driver.BenchmarkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

@Component
@ConditionalOnProperty(prefix = "presto", value = "url")
public class PrestoClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrestoClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BenchmarkProperties properties;

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

    private URI buildQueryInfoURI(String queryId)
    {
        checkState(!properties.getPrestoURL().isEmpty());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(properties.getPrestoURL())
                .pathSegment("v1", "query", queryId);

        return URI.create(uriBuilder.toUriString());
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

        try {
            HttpEntity<String> response = restTemplate.exchange(uri.get(), HttpMethod.GET, entity, String.class);
            return Optional.of(response.getBody());
        }
        catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.warn("Query completion event for " + queryId + " not found");
                return Optional.empty();
            }
            LOGGER.error("Unexpected error " + e.getStatusCode() + " when gettin query completion event for " + queryId, e);
            return Optional.empty();
        }
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
}
