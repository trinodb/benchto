/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.sql.QueryExecutionResult;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.Duration;
import java.util.List;

public class BenchmarkResult
{
    private final Benchmark benchmark;
    private final List<QueryExecutionResult> executions;
    private final DescriptiveStatistics durationStatistics;

    public BenchmarkResult(Benchmark benchmark, List<QueryExecutionResult> executions)
    {
        this.benchmark = benchmark;
        this.executions = executions;
        this.durationStatistics = new DescriptiveStatistics(
                executions.stream()
                        .map(QueryExecutionResult::getQueryDuration)
                        .map(Duration::toMillis)
                        .mapToDouble(Long::doubleValue)
                        .toArray());
    }

    public Benchmark getBenchmark()
    {
        return benchmark;
    }

    public List<QueryExecutionResult> getExecutions()
    {
        return executions;
    }

    public DescriptiveStatistics getDurationStatistics()
    {
        return durationStatistics;
    }

    public boolean isSuccessful()
    {
        return executions.stream().allMatch(QueryExecutionResult::isSuccessful);
    }
}
