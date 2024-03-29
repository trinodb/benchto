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
package io.trino.benchto.driver.listeners.benchmark;

import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.execution.BenchmarkExecutionResult;
import io.trino.benchto.driver.execution.QueryExecution;
import io.trino.benchto.driver.execution.QueryExecutionResult;
import org.springframework.core.Ordered;

import java.util.List;
import java.util.concurrent.Future;

public interface BenchmarkExecutionListener
        extends Ordered
{
    Future<?> benchmarkStarted(Benchmark benchmark);

    Future<?> benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult);

    Future<?> executionStarted(QueryExecution queryExecution);

    Future<?> executionFinished(QueryExecutionResult execution);

    Future<?> concurrencyTestExecutionFinished(List<QueryExecutionResult> executions);
}
