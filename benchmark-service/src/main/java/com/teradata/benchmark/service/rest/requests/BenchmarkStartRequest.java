/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.rest.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class BenchmarkStartRequest
{
    private final String environmentName;
    private final Map<String, String> attributes;

    @JsonCreator
    public BenchmarkStartRequest(@JsonProperty("environmentName") String environmentName, @JsonProperty("attributes") Map<String, String> attributes)
    {
        this.environmentName = environmentName;
        this.attributes = attributes;
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }
}
