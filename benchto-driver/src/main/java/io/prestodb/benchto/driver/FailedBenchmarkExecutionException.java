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
package io.prestodb.benchto.driver;

import com.google.common.collect.Iterables;
import io.prestodb.benchto.driver.execution.BenchmarkExecutionResult;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class FailedBenchmarkExecutionException
        extends BenchmarkExecutionException
{
    private final List<BenchmarkExecutionResult> failedBenchmarkResults;
    private final int benchmarksCount;

    public FailedBenchmarkExecutionException(List<BenchmarkExecutionResult> failedBenchmarkResults, int benchmarksCount)
    {
        super(createMessage(failedBenchmarkResults));
        this.failedBenchmarkResults = failedBenchmarkResults;
        this.benchmarksCount = benchmarksCount;

        failedBenchmarkResults.stream()
                .flatMap(benchmarkExecutionResult -> benchmarkExecutionResult.getFailureCauses().stream())
                .forEach(this::addSuppressed);
    }

    private static String createMessage(List<BenchmarkExecutionResult> failedBenchmarkResults)
    {
        checkArgument(!failedBenchmarkResults.isEmpty(), "no failures");

        return format("%s benchmarks failed, first failure was: %s",
                failedBenchmarkResults.size(),
                Iterables.getFirst(failedBenchmarkResults.get(0).getFailureCauses(), null)
        );
    }

    public List<BenchmarkExecutionResult> getFailedBenchmarkResults()
    {
        return failedBenchmarkResults;
    }

    public int getBenchmarksCount()
    {
        return benchmarksCount;
    }
}
