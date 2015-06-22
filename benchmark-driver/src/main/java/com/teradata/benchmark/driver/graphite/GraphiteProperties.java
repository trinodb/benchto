/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.graphite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.facebook.presto.jdbc.internal.guava.base.MoreObjects.toStringHelper;

@Component
public class GraphiteProperties
{
    private static final int GRAPHITE_WAIT_BETWEEN_REPORTING_RESOLUTION_COUNT = 3;
    private static final int GRAPHITE_CUT_OFF_THRESHOLD_RESOLUTION_COUNT = 2;

    @Value("${graphite.metrics.cpu:#{null}}")
    private String cpuGraphiteExpr;

    @Value("${graphite.metrics.memory:#{null}}")
    private String memoryGraphiteExpr;

    @Value("${graphite.metrics.network:#{null}}")
    private String networkGraphiteExpr;

    @Value("${graphite.resolution.seconds:0}")
    private int graphiteResolutionSeconds;

    public Optional<String> getCpuGraphiteExpr()
    {
        return Optional.ofNullable(cpuGraphiteExpr);
    }

    public Optional<String> getMemoryGraphiteExpr()
    {
        return Optional.ofNullable(memoryGraphiteExpr);
    }

    public Optional<String> getNetworkGraphiteExpr()
    {
        return Optional.ofNullable(networkGraphiteExpr);
    }

    public Optional<Integer> getGraphiteResolutionSeconds()
    {
        if (graphiteResolutionSeconds <= 0) {
            return Optional.empty();
        }
        return Optional.of(graphiteResolutionSeconds);
    }

    public Optional<Integer> waitSecondsBeforeExecutionReporting()
    {
        if (getGraphiteResolutionSeconds().isPresent()) {
            return Optional.of(getGraphiteResolutionSeconds().get() * GRAPHITE_WAIT_BETWEEN_REPORTING_RESOLUTION_COUNT);
        }
        return Optional.empty();
    }

    public Optional<Integer> cutOffThresholdSecondsForMeasurementReporting()
    {
        if (getGraphiteResolutionSeconds().isPresent()) {
            return Optional.of(getGraphiteResolutionSeconds().get() * GRAPHITE_CUT_OFF_THRESHOLD_RESOLUTION_COUNT);
        }
        return Optional.empty();
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("cpuGraphiteExpr", cpuGraphiteExpr)
                .add("memoryGraphiteExpr", memoryGraphiteExpr)
                .add("networkGraphiteExpr", networkGraphiteExpr)
                .add("graphiteResolutionSeconds", graphiteResolutionSeconds)
                .toString();
    }
}
