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
package com.teradata.benchto.driver.listeners.benchmark;

import com.facebook.presto.jdbc.internal.guava.collect.Ordering;
import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;

@Component
public class BenchmarkStatusReporter
{
    @Autowired
    private List<BenchmarkExecutionListener> executionListeners;

    @PostConstruct
    public void sortExecutionListeners()
    {
        // HACK: listeners have to be sorted to provide tests determinism
        executionListeners = ImmutableList.copyOf(Ordering.usingToString().sortedCopy(executionListeners));
    }

    public void reportBenchmarkStarted(Benchmark benchmark)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.benchmarkStarted(benchmark);
        }
    }

    public void reportBenchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.benchmarkFinished(benchmarkExecutionResult);
        }
    }

    public void reportExecutionStarted(QueryExecution queryExecution)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.executionStarted(queryExecution);
        }
    }

    public void reportExecutionFinished(QueryExecutionResult queryExecutionResult)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.executionFinished(queryExecutionResult);
        }
    }
}
