/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.presto;

import com.teradata.benchmark.driver.Measurable;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;
import com.teradata.benchmark.driver.listeners.measurements.PostExecutionMeasurementProvider;
import com.teradata.benchmark.driver.service.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Collections.emptyList;

@Component
@ConditionalOnProperty(prefix = "benchmark.feature.presto", value = "metrics.collection.enabled")
public class PrestoMetricsLoader
        implements PostExecutionMeasurementProvider
{

    @Autowired
    private PrestoClient prestoClient;

    @Override
    public List<Measurement> loadMeasurements(Measurable measurable)
    {
        if (measurable instanceof QueryExecutionResult) {
            QueryExecutionResult executionResult = (QueryExecutionResult) measurable;
            if (executionResult.getPrestoQueryId().isPresent()) {
                return prestoClient.loadMetrics(executionResult.getPrestoQueryId().get());
            }
        }
        return emptyList();
    }
}
