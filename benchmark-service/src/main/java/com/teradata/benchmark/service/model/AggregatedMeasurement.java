/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.model;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Collection;

public class AggregatedMeasurement
{
    private final MeasurementUnit unit;
    private final double min, max, mean, stdDev;

    public AggregatedMeasurement(MeasurementUnit unit, double max, double min, double mean, double stdDev)
    {
        this.unit = unit;
        this.stdDev = stdDev;
        this.mean = mean;
        this.max = max;
        this.min = min;
    }

    public static AggregatedMeasurement aggregate(MeasurementUnit unit, Collection<Double> values)
    {
        DescriptiveStatistics statistics = new DescriptiveStatistics(values.stream()
                .mapToDouble(Double::doubleValue)
                .toArray());
        return new AggregatedMeasurement(unit,
                statistics.getMin(),
                statistics.getMax(),
                statistics.getMean(),
                statistics.getStandardDeviation()
        );
    }

    public MeasurementUnit getUnit()
    {
        return unit;
    }

    public double getMin()
    {
        return min;
    }

    public double getMax()
    {
        return max;
    }

    public double getMean()
    {
        return mean;
    }

    public double getStdDev()
    {
        return stdDev;
    }
}
