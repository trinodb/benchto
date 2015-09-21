/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.listeners.measurements;

import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.Measurable;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.service.Measurement;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.teradata.benchto.driver.service.Measurement.measurement;
import static java.util.Collections.emptyList;

@Component
public class ThroughputMeasurementProvider
        implements PostExecutionMeasurementProvider
{
    @Override
    public List<Measurement> loadMeasurements(Measurable measurable)
    {
        if (measurable instanceof BenchmarkExecutionResult && measurable.getBenchmark().isConcurrent() && measurable.isSuccessful()) {
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
