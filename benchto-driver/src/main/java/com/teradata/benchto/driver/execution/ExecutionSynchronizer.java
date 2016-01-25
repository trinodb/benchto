/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
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
    private static final double GRAPHITE_CUT_OFF_THRESHOLD_RESOLUTION_COUNT = 1.3;

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

    /**
     * This method returns number of seconds which should be passed before/after query or benchmark
     * run. This is needed as graphite collects metrics with given resolution, so we need to make sure
     * that time range which is passed to Graphite query consists all data points.
     *
     * @return number of seconds which should be passed before/after query or benchmark run
     */
    public long cutOffThresholdSecondsForMeasurementReporting()
    {
        return (long) (properties.getGraphiteResolutionSeconds() * GRAPHITE_CUT_OFF_THRESHOLD_RESOLUTION_COUNT);
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
