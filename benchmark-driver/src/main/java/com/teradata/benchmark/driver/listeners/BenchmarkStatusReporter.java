package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.BenchmarkResult;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.sql.QueryExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BenchmarkStatusReporter
{

    @Autowired
    private List<BenchmarkExecutionListener> executionListeners;

    public void reportBenchmarkStarted(Query query)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.benchmarkStarted(query);
        }
    }

    public void reportBenchmarkFinished(BenchmarkResult result)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.benchmarkFinished(result);
        }
    }

    public void reportExecutionStarted(Query query, int run)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.executionStarted(query, run);
        }
    }

    public void reportExecutionFinished(Query query, int run, QueryExecution execution)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.executionFinished(query, run, execution);
        }
    }

    public void reportBenchmarkFinished(List<BenchmarkResult> benchmarkResults)
    {
        for (BenchmarkExecutionListener listener : executionListeners) {
            listener.suiteFinished(benchmarkResults);
        }
    }
}
