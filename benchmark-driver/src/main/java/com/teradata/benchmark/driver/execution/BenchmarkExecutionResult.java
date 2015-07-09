/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Measurable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

public class BenchmarkExecutionResult
        extends Measurable
{
    private final Benchmark benchmark;
    private boolean prewarmFailed;
    private List<QueryExecutionResult> executions;

    private BenchmarkExecutionResult(Benchmark benchmark)
    {
        this.benchmark = benchmark;
    }

    @Override
    public Benchmark getBenchmark()
    {
        return benchmark;
    }

    public List<QueryExecutionResult> getExecutions()
    {
        return executions;
    }

    public boolean isSuccessful()

    {
        return !prewarmFailed && executions.stream().allMatch(QueryExecutionResult::isSuccessful);
    }

    public static class BenchmarkExecutionResultBuilder
            extends Measurable.MeasuredBuilder<BenchmarkExecutionResult, BenchmarkExecutionResultBuilder>
    {

        public BenchmarkExecutionResultBuilder(Benchmark benchmark)
        {
            super(new BenchmarkExecutionResult(benchmark));
        }

        public BenchmarkExecutionResultBuilder withPrewarmFailed()
        {
            object.prewarmFailed = true;
            object.executions = emptyList();
            return this;
        }

        public BenchmarkExecutionResultBuilder setExecutions(List<QueryExecutionResult> executions)
        {
            object.executions = executions;
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
