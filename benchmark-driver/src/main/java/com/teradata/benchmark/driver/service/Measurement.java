package com.teradata.benchmark.driver.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unused")
@JsonAutoDetect(fieldVisibility = ANY)
public class Measurement
{
    private String name;
    private String unit;
    private double value;

    public static Measurement measurement(String name, String unit, double value)
    {
        Measurement measurement = new Measurement();
        measurement.name = checkNotNull(name);
        measurement.unit = checkNotNull(unit);
        measurement.value = value;
        return measurement;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Measurement that = (Measurement) o;

        return Double.compare(that.value, value) == 0 && name.equals(that.name) && unit.equals(that.unit);
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        result = name.hashCode();
        result = 31 * result + unit.hashCode();
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
