/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.measurements;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.Measurable;
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
        if (measurable instanceof BenchmarkResult && measurable.getBenchmark().isConcurrent()) {
            return ImmutableList.of(measurement("throughput", "QUERY_PER_SECOND", calculateThroughput((BenchmarkResult) measurable)));
        }
        return emptyList();
    }

    private double calculateThroughput(BenchmarkResult benchmarkResult)
    {
        long durationInMillis = benchmarkResult.getQueryDuration().toMillis();
        return (double) benchmarkResult.getExecutions().size() / durationInMillis * 1000;
    }
}
