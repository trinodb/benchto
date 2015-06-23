/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.teradata.benchmark.driver.Measurable;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.Duration;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class BenchmarkExecutionResult
        extends Measurable
{
    private final BenchmarkExecution benchmarkExecution;
    private List<QueryExecutionResult> executions;
    private DescriptiveStatistics durationStatistics;

    private BenchmarkExecutionResult(BenchmarkExecution benchmarkExecution)
    {
        this.benchmarkExecution = benchmarkExecution;
    }

    @Override
    public BenchmarkExecution getBenchmarkExecution()
    {
        return benchmarkExecution;
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

    public static class BenchmarkExecutionResultBuilder
            extends Measurable.MeasuredBuilder<BenchmarkExecutionResult, BenchmarkExecutionResultBuilder>
    {

        public BenchmarkExecutionResultBuilder(BenchmarkExecution benchmarkExecution)
        {
            super(new BenchmarkExecutionResult(benchmarkExecution));
        }

        public BenchmarkExecutionResultBuilder setExecutions(List<QueryExecutionResult> executions)
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
        public BenchmarkExecutionResult build()
        {
            checkNotNull(object.executions, "Executions are not set");
            return super.build();
        }
    }
}
