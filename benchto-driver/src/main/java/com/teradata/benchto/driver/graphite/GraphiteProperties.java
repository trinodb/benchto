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
package com.teradata.benchto.driver.graphite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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

    @Value("${graphite.resolution.seconds:0}")
    private int graphiteResolutionSeconds;

    @Value("${graphite.metrics-delay.seconds:0}")
    private int graphiteMetricsDelaySeconds;

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

    public int getGraphiteResolutionSeconds()
    {
        return graphiteResolutionSeconds;
    }

    public Duration getGraphiteMetricsDelay()
    {
        return Duration.of(graphiteMetricsDelaySeconds, ChronoUnit.SECONDS);
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
                .add("graphiteMetricsDelaySeconds", graphiteMetricsDelaySeconds)
                .add("graphiteMetricsCollectionEnabled", graphiteMetricsCollectionEnabled)
                .toString();
    }
}
