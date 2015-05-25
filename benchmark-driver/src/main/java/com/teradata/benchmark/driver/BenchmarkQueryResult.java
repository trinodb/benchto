/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.sql.QueryExecution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.Duration;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class BenchmarkQueryResult
{
    private final BenchmarkQuery query;
    private List<QueryExecution> executions;

    public BenchmarkQueryResult(BenchmarkQuery query)
    {
        this.query = query;
        this.executions = newArrayList();
    }

    public void addExecution(QueryExecution execution)
    {
        executions.add(execution);
    }

    public DescriptiveStatistics getDurationStatistics()
    {
        return new DescriptiveStatistics(
                executions.stream()
                        .map(QueryExecution::getQueryDuration)
                        .map(Duration::toMillis)
                        .mapToDouble(Long::doubleValue)
                        .toArray());
    }

    public BenchmarkQuery getQuery()
    {
        return query;
    }

    public boolean isSuccessful()
    {
        return executions.stream().allMatch(QueryExecution::isSuccessful);
    }
}
