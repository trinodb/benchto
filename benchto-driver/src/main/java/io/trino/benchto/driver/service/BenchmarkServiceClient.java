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
package io.trino.benchto.driver.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.benchto.driver.BenchmarkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;

@Component
public class BenchmarkServiceClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkServiceClient.class);

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private RestTemplate restTemplate;

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public Instant getServiceCurrentTime()
    {
        Long serviceCurrentTime = postForObject("/v1/time/current-time-millis", null, Long.class);
        return Instant.ofEpochMilli(requireNonNull(serviceCurrentTime, "service returned null time"));
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public List<String> generateUniqueBenchmarkNames(List<GenerateUniqueNamesRequestItem> generateUniqueNamesRequestItems)
    {
        String[] uniqueNames = postForObject("/v1/benchmark/generate-unique-names", generateUniqueNamesRequestItems, String[].class);
        return ImmutableList.copyOf(uniqueNames);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public List<Duration> getBenchmarkSuccessfulExecutionAges(List<String> benchmarkUniqueNames)
    {
        Duration[] ages = postForObject("/v1/benchmark/get-successful-execution-ages", benchmarkUniqueNames, Duration[].class);
        return ImmutableList.copyOf(ages);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public String startBenchmark(String uniqueBenchmarkName, String benchmarkSequenceId, BenchmarkStartRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);

        return postForObject("/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/start", request, requestParams);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public void finishBenchmark(String uniqueBenchmarkName, String benchmarkSequenceId, FinishRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);

        postForObject("/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/finish", request, requestParams);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public void startExecution(String uniqueBenchmarkName, String benchmarkSequenceId, String executionSequenceId, ExecutionStartRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);
        requestParams.put("executionSequenceId", executionSequenceId);

        postForObject("/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", request, requestParams);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public void finishExecution(String uniqueBenchmarkName, String benchmarkSequenceId, String executionSequenceId, FinishRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);
        requestParams.put("executionSequenceId", executionSequenceId);

        postForObject("/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", request, requestParams);
    }

    private Map<String, String> requestParams(String uniqueBenchmarkName, String benchmarkSequenceId)
    {
        Map<String, String> params = newHashMap();
        params.put("serviceUrl", properties.getServiceURL());
        params.put("uniqueBenchmarkName", uniqueBenchmarkName);
        params.put("benchmarkSequenceId", benchmarkSequenceId);
        return params;
    }

    private String postForObject(String url, Object request, Map<String, String> requestParams)
    {
        return postForObject(url, request, String.class, requestParams);
    }

    private <T> T postForObject(String url, Object request, Class<T> clazz)
    {
        return postForObject(url, request, clazz, ImmutableMap.of());
    }

    private <T> T postForObject(String url, Object request, Class<T> clazz, Map<String, String> requestParams)
    {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(properties.getServiceURL())
                .path(url);
        URI uri = uriBuilder.buildAndExpand(requestParams).toUri();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Post object to benchmark service on URL: {}, with request: {}", uri, request);
        }

        return restTemplate.postForObject(uri, request, clazz);
    }

    public static class GenerateUniqueNamesRequestItem
    {
        private final String name;
        private final Map<String, String> variables;

        private GenerateUniqueNamesRequestItem(String name, Map<String, String> variables)
        {
            this.name = name;
            this.variables = variables;
        }

        public String getName()
        {
            return name;
        }

        public Map<String, String> getVariables()
        {
            return variables;
        }

        public static GenerateUniqueNamesRequestItem generateUniqueNamesRequestItem(String name, Map<String, String> variables)
        {
            return new GenerateUniqueNamesRequestItem(name, variables);
        }

        @Override
        public String toString()
        {
            return toStringHelper(this)
                    .add("name", name)
                    .add("variables", variables)
                    .toString();
        }
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public abstract static class AttributeRequest
    {
        protected Map<String, String> attributes = newHashMap();

        public abstract static class AttributeRequestBuilder<T extends AttributeRequest>
        {
            protected final T request;

            public AttributeRequestBuilder(T request)
            {
                this.request = request;
            }

            public AttributeRequestBuilder<T> addAttribute(String name, String value)
            {
                request.attributes.put(name, value);
                return this;
            }

            public T build()
            {
                return request;
            }
        }
    }

    @SuppressWarnings("unused")
    public static class BenchmarkStartRequest
            extends AttributeRequest
    {
        private String name;
        private String environmentName;
        private final Map<String, String> variables = newHashMap();

        private BenchmarkStartRequest()
        {
        }

        public static class BenchmarkStartRequestBuilder
                extends AttributeRequestBuilder<BenchmarkStartRequest>
        {
            public BenchmarkStartRequestBuilder(String name)
            {
                super(new BenchmarkStartRequest());
                request.name = name;
            }

            public BenchmarkStartRequestBuilder environmentName(String environmentName)
            {
                request.environmentName = environmentName;
                return this;
            }

            public BenchmarkStartRequestBuilder addVariable(String name, String value)
            {
                request.variables.put(name, value);
                return this;
            }
        }

        @Override
        public String toString()
        {
            return toStringHelper(this)
                    .add("name", name)
                    .add("environmentName", environmentName)
                    .add("variables", variables)
                    .add("attributes", attributes)
                    .toString();
        }
    }

    public static class ExecutionStartRequest
            extends AttributeRequest
    {
        private ExecutionStartRequest()
        {
        }

        public static class ExecutionStartRequestBuilder
                extends AttributeRequestBuilder<ExecutionStartRequest>
        {
            public ExecutionStartRequestBuilder()
            {
                super(new ExecutionStartRequest());
            }
        }

        @Override
        public String toString()
        {
            return toStringHelper(this)
                    .add("attributes", attributes)
                    .toString();
        }
    }

    public static class FinishRequest
            extends AttributeRequest
    {
        public enum Status
        {
            STARTED, ENDED, FAILED
        }

        private Status status;
        private Instant endTime;
        private final List<Measurement> measurements = newArrayList();
        private String queryInfo;
        private String queryCompletionEvent;

        private FinishRequest()
        {
        }

        public static class FinishRequestBuilder
                extends AttributeRequestBuilder<FinishRequest>
        {
            public FinishRequestBuilder()
            {
                super(new FinishRequest());
            }

            public FinishRequestBuilder withStatus(Status status)
            {
                request.status = status;
                return this;
            }

            public FinishRequestBuilder withEndTime(Instant endTime)
            {
                request.endTime = endTime;
                return this;
            }

            public FinishRequestBuilder addMeasurement(Measurement measurement)
            {
                request.measurements.add(measurement);
                return this;
            }

            public FinishRequestBuilder addMeasurements(Collection<Measurement> measurements)
            {
                request.measurements.addAll(measurements);
                return this;
            }

            public FinishRequestBuilder addQueryInfo(String queryInfo)
            {
                request.queryInfo = queryInfo;
                return this;
            }

            public FinishRequestBuilder addQueryCompletionEvent(String queryCompletionEvent)
            {
                request.queryCompletionEvent = queryCompletionEvent;
                return this;
            }
        }

        @Override
        public String toString()
        {
            return toStringHelper(this)
                    .add("measurements", measurements)
                    .add("status", status)
                    .add("endTime", endTime)
                    .add("attributes", attributes)
                    .add("queryInfo", queryInfo == null ? "NULL" : "NOT NULL")
                    .add("queryCompletionEvent", queryCompletionEvent == null ? "NULL" : "NOT NULL")
                    .toString();
        }
    }
}
