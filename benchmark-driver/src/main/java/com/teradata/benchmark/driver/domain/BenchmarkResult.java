/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.domain;

import com.google.common.base.Preconditions;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.Duration;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class BenchmarkResult
        extends Measurable
{
    private final Benchmark benchmark;
    private List<QueryExecutionResult> executions;
    private DescriptiveStatistics durationStatistics;

    private BenchmarkResult(Benchmark benchmark)
    {
        this.benchmark = benchmark;
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

    public static class BenchmarkResultBuilder
            extends Measurable.MeasuredBuilder<BenchmarkResult, BenchmarkResultBuilder>
    {

        public BenchmarkResultBuilder(Benchmark benchmark)
        {
            super(new BenchmarkResult(benchmark));
        }

        public BenchmarkResultBuilder setExecutions(List<QueryExecutionResult> executions)
        {
            object.executions = executions;
            object.durationStatistics = new DescriptiveStatistics(
                    executions.stream()
                            .map(QueryExecutionResult::getQueryDuration)
                            .map(Duration::toMillis)
                            .mapToDouble(Long::doubleValue)
                            .toArray());
            return this;
        }

        @Override
        public BenchmarkResult build()
        {
            checkNotNull(object.executions, "Executions are not set");
            return super.build();
        }
    }
}
