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
    @Value("${graphite.metrics.cpu:#{null}}")
    private String cpuGraphiteExpr;

    @Value("${graphite.metrics.memory:#{null}}")
    private String memoryGraphiteExpr;

    @Value("${graphite.metrics.network:#{null}}")
    private String networkGraphiteExpr;

    @Value("${graphite.resolution.seconds:#{null}}")
    private int graphiteResolutionSeconds;

    @Value("${benchmark.feature.graphite.metrics.collection.enabled:#{false}}")
    private boolean graphiteMetricsCollectionEnabled;

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
        return Optional.ofNullable(graphiteResolutionSeconds);
    }

    public boolean isGraphiteMetricsCollectionEnabled()
    {
        return graphiteMetricsCollectionEnabled;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("cpuGraphiteExpr", cpuGraphiteExpr)
                .add("memoryGraphiteExpr", memoryGraphiteExpr)
                .add("networkGraphiteExpr", networkGraphiteExpr)
                .add("graphiteResolutionSeconds", graphiteResolutionSeconds)
                .add("graphiteMetricsCollectionEnabled", graphiteMetricsCollectionEnabled)
                .toString();
    }
}
