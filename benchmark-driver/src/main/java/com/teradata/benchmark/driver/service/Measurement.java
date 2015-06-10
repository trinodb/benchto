package com.teradata.benchmark.driver.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

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
        measurement.name = name;
        measurement.unit = unit;
        measurement.value = value;
        return measurement;
    }
}
