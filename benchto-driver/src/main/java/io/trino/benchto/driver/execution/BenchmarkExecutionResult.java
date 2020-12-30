/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.benchto.driver.execution;

import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.Measurable;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
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

    @Override
    public String getEnvironment()
    {
        return benchmark.getEnvironment();
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

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("benchmark", benchmark.getName())
                .add("successful", isSuccessful())
                .add("duration", getQueryDuration().toMillis() + " ms")
                .toString();
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
            requireNonNull(object.executions, "Executions are not set");
            return super.build();
        }
    }
}
