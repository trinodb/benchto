/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners.measurements;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.Measurable;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import com.teradata.benchmark.driver.service.Measurement;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.teradata.benchmark.driver.service.Measurement.measurement;

@Component
public class DurationMeasurementProvider
        implements PostExecutionMeasurementProvider
{
    @Override
    public List<Measurement> loadMeasurements(Measurable measurable)
    {
        if (shouldMeasureDuration(measurable)) {
            return ImmutableList.of(measurement("duration", "MILLISECONDS", measurable.getQueryDuration().toMillis()));
        }
        else {
            return ImmutableList.of();
        }
    }

    private boolean shouldMeasureDuration(Measurable measurable)
    {
        if (measurable instanceof QueryExecutionResult) {
            return true;
        }
        else if (measurable instanceof BenchmarkResult && measurable.getBenchmark().isConcurrent()) {
            return true;
        }
        return false;
    }
}
