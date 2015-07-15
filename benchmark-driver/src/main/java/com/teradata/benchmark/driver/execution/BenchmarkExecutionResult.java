/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Measurable;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class BenchmarkExecutionResult
        extends Measurable
{
    private final Benchmark benchmark;
    private Optional<Exception> failure = Optional.empty();
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

    @Override
    public boolean isSuccessful()
    {
        return !failure.isPresent() && executions.stream().allMatch(QueryExecutionResult::isSuccessful);
    }

    public List<Exception> getFailureCauses()
    {
        List<Exception> failureCauses = executions.stream()
                .filter(queryExecutionResult -> !queryExecutionResult.isSuccessful())
                .map(QueryExecutionResult::getFailureCause)
                .collect(toList());
        failure.ifPresent(failureCauses::add);
        return failureCauses;
    }

    public static class BenchmarkExecutionResultBuilder
            extends Measurable.MeasuredBuilder<BenchmarkExecutionResult, BenchmarkExecutionResultBuilder>
    {

        public BenchmarkExecutionResultBuilder(Benchmark benchmark)
        {
            super(new BenchmarkExecutionResult(benchmark));
        }

        public BenchmarkExecutionResultBuilder withUnexpectedException(Exception failure)
        {
            object.failure = Optional.of(failure);
            object.executions = emptyList();
            return this;
        }

        public BenchmarkExecutionResultBuilder withExecutions(List<QueryExecutionResult> executions)
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
