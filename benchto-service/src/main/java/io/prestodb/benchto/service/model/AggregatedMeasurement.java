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
package io.prestodb.benchto.service.model;

import com.google.common.collect.Iterables;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.Serializable;
import java.util.Collection;

public class AggregatedMeasurement
        implements Serializable
{
    private final MeasurementUnit unit;
    private final double min;
    private final double max;
    private final double mean;
    private final double stdDev;
    private final double stdDevPercent;

    public AggregatedMeasurement(MeasurementUnit unit, double min, double max, double mean, double stdDev, double stdDevPercent)
    {
        this.unit = unit;
        this.stdDev = stdDev;
        this.mean = mean;
        this.max = max;
        this.min = min;
        this.stdDevPercent = stdDevPercent;
    }

    public static AggregatedMeasurement aggregate(MeasurementUnit unit, Collection<Double> values)
    {
        if (values.size() < 2) {
            Double value = Iterables.getOnlyElement(values);
            return new AggregatedMeasurement(unit, value, value, value, 0.0, 0.0);
        }
        DescriptiveStatistics statistics = new DescriptiveStatistics(values.stream()
                .mapToDouble(Double::doubleValue)
                .toArray());

        double stdDevPercent = 0.0;
        if (statistics.getStandardDeviation() > 0.0) {
            stdDevPercent = (statistics.getStandardDeviation() / statistics.getMean()) * 100;
        }

        return new AggregatedMeasurement(unit,
                statistics.getMin(),
                statistics.getMax(),
                statistics.getMean(),
                statistics.getStandardDeviation(),
                stdDevPercent
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

    public double getStdDevPercent()
    {
        return stdDevPercent;
    }
}
