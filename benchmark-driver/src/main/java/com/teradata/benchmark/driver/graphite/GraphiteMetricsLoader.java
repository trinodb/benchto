/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.graphite;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.teradata.benchmark.driver.service.Measurement;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.teradata.benchmark.driver.service.Measurement.measurement;
import static java.lang.String.format;

@Service
public class GraphiteMetricsLoader
{
    private static final Logger LOG = LoggerFactory.getLogger(GraphiteMetricsLoader.class);

    @Autowired
    private GraphiteClient graphiteClient;

    @Autowired
    private GraphiteProperties graphiteProperties;

    private Map<String, String> queryMetrics;

    @PostConstruct
    public void initQueryMetrics()
    {
        queryMetrics = newHashMap();
        graphiteProperties.getCpuGraphiteExpr().ifPresent(value -> queryMetrics.put("cpu", value));
        graphiteProperties.getMemoryGraphiteExpr().ifPresent(value -> queryMetrics.put("memory", value));
        graphiteProperties.getNetworkGraphiteExpr().ifPresent(value -> queryMetrics.put("network", value));
        graphiteProperties.getNetworkGraphiteExpr().ifPresent(value -> queryMetrics.put("network_total", format("integral(%s)", value)));
    }

    public List<Measurement> loadMetrics(ZonedDateTime from, ZonedDateTime to)
    {
        Optional<Integer> cutOffThresholdSeconds = graphiteProperties.cutOffThresholdSecondsForMeasurementReporting();
        if (queryMetrics.isEmpty() || !cutOffThresholdSeconds.isPresent()) {
            return ImmutableList.of();
        }

        from = from.minusSeconds(cutOffThresholdSeconds.get());
        to = to.plusSeconds(cutOffThresholdSeconds.get());

        LOG.debug("Loading metrics {} - from: {}, to: {}", queryMetrics, from, to);

        Map<String, double[]> loadedMetrics = graphiteClient.loadMetrics(queryMetrics, from, to);

        List<Measurement> measurements = newArrayList();

        if (graphiteProperties.getCpuGraphiteExpr().isPresent() && loadedMetrics.containsKey("cpu")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "cpu", "PERCENT");
        }

        if (graphiteProperties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("memory")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "memory", "BYTES");
        }

        if (graphiteProperties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("network")) {
            addMeanMaxMeasurements(loadedMetrics, measurements, "network", "BYTES");
        }

        if (graphiteProperties.getMemoryGraphiteExpr().isPresent() && loadedMetrics.containsKey("network_total")) {
            double[] metricValues = loadedMetrics.get("network_total");
            if (metricValues.length > 0) {
                // last non zero measurement contains total over time
                double totalBytes = getLastValueGreaterThanZero(metricValues);
                measurements.add(measurement("network_total", "BYTES", totalBytes));
            }
        }

        return measurements;
    }

    private void addMeanMaxMeasurements(Map<String, double[]> loadedMetrics, List<Measurement> measurements, String metricName, String unit)
    {
        Optional<StatisticalSummary> statistics = getStats(loadedMetrics, metricName);
        if (statistics.isPresent()) {
            measurements.add(measurement(metricName + "_max", unit, statistics.get().getMax()));
            measurements.add(measurement(metricName + "_mean", unit, statistics.get().getMean()));
        }
    }

    private Optional<StatisticalSummary> getStats(Map<String, double[]> loadedMetrics, String metricName)
    {
        double[] metricValues = loadedMetrics.get(metricName);
        if (metricValues.length >= 2) {
            return Optional.of(new DescriptiveStatistics(metricValues));
        }
        else if (metricValues.length == 1) {
            double value = metricValues[0];
            return Optional.of(new StatisticalSummaryValues(value, 0, 1, value, value, value));
        }
        else {
            return Optional.empty();
        }
    }

    private double getLastValueGreaterThanZero(double[] metricValues)
    {
        for (int i = metricValues.length - 1; i >= 0; --i) {
            if (metricValues[i] > 0.0) {
                return metricValues[i];
            }
        }
        return 0;
    }
}
