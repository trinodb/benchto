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
package com.teradata.benchto.driver.execution;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.graphite.GraphiteProperties;
import com.teradata.benchto.driver.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This class is responsible for synchronizing threads in driver if graphite metrics collection
 * is enabled. Graphite collects metrics with predefined resolution, ex. 10 s.
 * <p/>
 * After query/benchmark is finished we should wait at least 2 resolutions before we execute
 * next query/benchmark, so runs does not interfere with each other.
 * <p/>
 * Graphite metrics loading should be delayed at least 1 resolution to make sure that last
 * probe was stored in graphite.
 */
@Component
public class ExecutionSynchronizer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionSynchronizer.class);

    private static final double GRAPHITE_WAIT_BETWEEN_REPORTING_RESOLUTION_COUNT = 2;

    @Autowired
    private GraphiteProperties properties;

    /**
     * If metrics collection is enabled and we are doing serial benchmark, we should wait
     * between queries, so measurements are accurate.
     */
    public void awaitAfterQueryExecutionAndBeforeResultReport(QueryExecutionResult queryExecutionResult)
    {
        if (properties.isGraphiteMetricsCollectionEnabled() && queryExecutionResult.getBenchmark().isSerial()) {
            int waitSecondsBetweenRuns = waitSecondsBetweenRuns();
            LOGGER.info("Waiting {}s between queries - thread ({})", waitSecondsBetweenRuns, currThreadName());
            TimeUtils.sleep(waitSecondsBetweenRuns, SECONDS);
        }
    }

    /**
     * If metrics collection is enabled and we are doing concurrent benchmark, we should wait
     * between benchmarks, so measurements are accurate.
     */
    public void awaitAfterBenchmarkExecutionAndBeforeResultReport(Benchmark benchmark)
    {
        if (properties.isGraphiteMetricsCollectionEnabled() && benchmark.isConcurrent()) {
            int waitSecondsBetweenRuns = waitSecondsBetweenRuns();
            LOGGER.info("Waiting {}s between benchmarks - thread ({})", waitSecondsBetweenRuns, currThreadName());
            TimeUtils.sleep(waitSecondsBetweenRuns, SECONDS);
        }
    }

    private int waitSecondsBetweenRuns()
    {
        return (int) (properties.getGraphiteResolutionSeconds() * GRAPHITE_WAIT_BETWEEN_REPORTING_RESOLUTION_COUNT);
    }

    private String currThreadName()
    {
        return Thread.currentThread().getName();
    }
}
