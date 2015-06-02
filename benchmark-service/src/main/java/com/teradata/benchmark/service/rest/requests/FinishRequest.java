/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.rest.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teradata.benchmark.service.model.Measurement;
import com.teradata.benchmark.service.model.Status;

import java.util.List;
import java.util.Map;

public class FinishRequest
{
    private final Status status;
    private final List<Measurement> measurements;
    private final Map<String, String> attributes;

    @JsonCreator
    public FinishRequest(@JsonProperty("status") Status status, @JsonProperty("measurements") List<Measurement> measurements, @JsonProperty("attributes") Map<String, String> attributes)
    {
        this.status = status;
        this.measurements = measurements;
        this.attributes = attributes;
    }

    public Status getStatus()
    {
        return status;
    }

    public List<Measurement> getMeasurements()
    {
        return measurements;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }
}
