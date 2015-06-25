/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.presto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableMap;
import com.teradata.benchmark.driver.service.Measurement;
import com.teradata.benchmark.driver.utils.UnitConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkState;
import static com.teradata.benchmark.driver.service.Measurement.measurement;
import static java.util.stream.Collectors.toList;
import static javax.measure.unit.NonSI.BYTE;
import static javax.measure.unit.SI.MILLI;
import static javax.measure.unit.SI.SECOND;

@Component
@ConditionalOnProperty(prefix = "presto", value = "url")
public class PrestoClient
{
    private static final Map<String, Unit> DEFAULT_METRICS = ImmutableMap.<String, Unit>builder()
            .put("totalPlanningTime", MILLI(SECOND))
            .put("totalScheduledTime", MILLI(SECOND))
            .put("totalCpuTime", MILLI(SECOND))
            .put("totalUserTime", MILLI(SECOND))
            .put("totalBlockedTime", MILLI(SECOND))
            .put("processedInputDataSize", BYTE)
            .put("outputDataSize", BYTE)
            .build();

    @Value("${presto.url}")
    private String prestoURL;

    @Autowired
    private RestTemplate restTemplate;

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public List<Measurement> loadMetrics(String queryId)
    {
        return loadMetrics(queryId, DEFAULT_METRICS);
    }

    private List<Measurement> loadMetrics(String queryId, Map<String, Unit> requiredStatistics)
    {
        URI uri = buildQueryInfoURI(queryId);
        ResponseEntity<QueryInfoResponseItem> response = restTemplate.getForEntity(uri, QueryInfoResponseItem.class);

        Map<String, String> queryStats = response.getBody().getQueryStats();
        return queryStats.keySet()
                .stream()
                .filter(requiredStatistics::containsKey)
                .map(name -> parseQueryStatistic(name, queryStats.get(name), requiredStatistics.get(name)))
                .collect(toList());
    }

    private URI buildQueryInfoURI(String queryId)
    {
        checkState(!prestoURL.isEmpty());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(prestoURL)
                .pathSegment("v1", "query", queryId);

        return URI.create(uriBuilder.toUriString());
    }

    private Measurement parseQueryStatistic(String name, String statistic, Unit requiredUnit)
    {
        double value = UnitConverter.parseValueAsUnit(statistic, requiredUnit);
        return measurement("prestoQuery-" + name, UnitConverter.format(requiredUnit), value);
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class QueryInfoResponseItem
    {
        private Map<String, String> queryStats;

        Map<String, String> getQueryStats()
        {
            return queryStats;
        }
    }
}
