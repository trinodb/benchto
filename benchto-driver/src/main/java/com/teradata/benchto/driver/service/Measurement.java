/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.MoreObjects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkNotNull;

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

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("unit", unit)
                .add("value", value)
                .toString();
    }
}
