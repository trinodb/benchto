/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.measurements;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.Measurable;
import com.teradata.benchmark.driver.service.Measurement;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.teradata.benchmark.driver.service.Measurement.measurement;
import static java.util.Collections.emptyList;

@Component
public class ThroughputMeasurementProvider
        implements PostExecutionMeasurementProvider
{
    @Override
    public List<Measurement> loadMeasurements(Measurable measurable)
    {
        if (measurable instanceof BenchmarkExecutionResult && measurable.getBenchmarkExecution().isConcurrent()) {
            return ImmutableList.of(measurement("throughput", "QUERY_PER_SECOND", calculateThroughput((BenchmarkExecutionResult) measurable)));
        }
        return emptyList();
    }

    private double calculateThroughput(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        long durationInMillis = benchmarkExecutionResult.getQueryDuration().toMillis();
        return (double) benchmarkExecutionResult.getExecutions().size() / durationInMillis * 1000;
    }
}
