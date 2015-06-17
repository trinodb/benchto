/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@Component
public class BenchmarkServiceClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkServiceClient.class);

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Autowired
    private RestTemplate restTemplate;

    public void startBenchmark(String benchmarkName, String benchmarkSequenceId, BenchmarkStartRequest request)
    {
        Map<String, String> requestParams = requestParams(benchmarkName, benchmarkSequenceId);

        postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", request, requestParams);
    }

    public void finishBenchmark(String benchmarkName, String benchmarkSequenceId, FinishRequest request)
    {
        Map<String, String> requestParams = requestParams(benchmarkName, benchmarkSequenceId);

        postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", request, requestParams);
    }

    public void startExecution(String benchmarkName, String benchmarkSequenceId, String executionSequenceId, ExecutionStartRequest request)
    {
        Map<String, String> requestParams = requestParams(benchmarkName, benchmarkSequenceId);
        requestParams.put("executionSequenceId", executionSequenceId);

        postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", request, requestParams);
    }

    public void finishExecution(String benchmarkName, String benchmarkSequenceId, String executionSequenceId, FinishRequest request)
    {
        Map<String, String> requestParams = requestParams(benchmarkName, benchmarkSequenceId);
        requestParams.put("executionSequenceId", executionSequenceId);

        postForObject("{serviceUrl}/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", request, requestParams);
    }

    private Map<String, String> requestParams(String benchmarkName, String benchmarkSequenceId)
    {
        Map<String, String> params = newHashMap();
        params.put("serviceUrl", serviceUrl);
        params.put("benchmarkName", benchmarkName);
        params.put("benchmarkSequenceId", benchmarkSequenceId);
        return params;
    }

    private Object postForObject(String url, Object request, Map<String, String> requestParams)
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Post object to benchmark service on URL: {}, with request: {}",
                    new UriTemplate(url).expand(requestParams),
                    request);
        }

        return restTemplate.postForObject(url, request, Object.class, requestParams);
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static abstract class AttributeRequest
    {
        protected Map<String, String> attributes = newHashMap();

        public static abstract class AttributeRequestBuilder<T extends AttributeRequest>
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
        private String environmentName;

        private BenchmarkStartRequest()
        {
        }

        public static class BenchmarkStartRequestBuilder
                extends AttributeRequestBuilder<BenchmarkStartRequest>
        {
            public BenchmarkStartRequestBuilder()
            {
                super(new BenchmarkStartRequest());
            }

            public BenchmarkStartRequestBuilder environmentName(String environmentName)
            {
                request.environmentName = environmentName;
                return this;
            }
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("environmentName", environmentName)
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
            return MoreObjects.toStringHelper(this)
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
        private List<Measurement> measurements = newArrayList();

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
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("measurements", measurements)
                    .add("status", status)
                    .add("attributes", attributes)
                    .toString();
        }
    }
}
