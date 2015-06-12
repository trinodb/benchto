/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.listeners.BenchmarkStatusReporter;
import com.teradata.benchmark.driver.sql.QueryExecution;
import com.teradata.benchmark.driver.sql.QueryExecutionResult;
import com.teradata.benchmark.driver.sql.SqlStatementExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

@Component
public class BenchmarkDriver
{

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkDriver.class);

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private BenchmarkLoader benchmarkLoader;

    @Autowired
    private SqlStatementExecutor sqlStatementExecutor;

    @Autowired
    private BenchmarkStatusReporter statusReporter;

    @Qualifier("queryTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor queryTaskExecutor;

    /**
     * @return true if all benchmark queries passed
     */
    public boolean run()
    {
        LOG.info("Running benchmark with properties: {}", properties);

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks();
        LOG.info("Loaded {} benchmarks", benchmarks.size());

        List<BenchmarkResult> benchmarkResults = benchmarks.stream()
                .map(this::executeBenchmark)
                .collect(toList());
        statusReporter.reportBenchmarkFinished(benchmarkResults);

        return !benchmarkResults.stream()
                .filter(result -> !result.isSuccessful())
                .findAny().isPresent();
    }

    private BenchmarkResult executeBenchmark(Benchmark benchmark)
    {
        statusReporter.reportBenchmarkStarted(benchmark);

        List<QueryExecutionCallable> executionCallables = buildQueryExecutionCallables(benchmark);

        try {
            List<Future<QueryExecutionResult>> executionFutures = queryTaskExecutor.getThreadPoolExecutor().invokeAll(executionCallables);
            List<QueryExecutionResult> executionResults = executionFutures.stream()
                    .map(this::awaitAndExtractExecutionResult)
                    .collect(toList());

            BenchmarkResult benchmarkResult = new BenchmarkResult(benchmark, executionResults);

            statusReporter.reportBenchmarkFinished(benchmarkResult);

            return benchmarkResult;
        }
        catch (InterruptedException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
    }

    private List<QueryExecutionCallable> buildQueryExecutionCallables(Benchmark benchmark)
    {
        List<QueryExecutionCallable> executionCallables = newArrayList();
        for (Query query : benchmark.getQueries()) {
            for (int run = 0; run < benchmark.getRuns(); ++run) {
                QueryExecution queryExecution = new QueryExecution(benchmark, query, run);
                QueryExecutionCallable queryExecutionCallable = new QueryExecutionCallable(queryExecution, statusReporter, sqlStatementExecutor,
                        properties.getGraphiteProperties().waitSecondsBeforeExecutionReporting());

                executionCallables.add(queryExecutionCallable);
            }
        }
        return executionCallables;
    }

    private QueryExecutionResult awaitAndExtractExecutionResult(Future<QueryExecutionResult> resultFuture)
    {
        try {
            return resultFuture.get();
        }
        catch (Exception e) {
            throw new BenchmarkExecutionException("Could not execute benchmark query", e);
        }
    }

    private static class QueryExecutionCallable
            implements Callable<QueryExecutionResult>
    {

        private QueryExecution queryExecution;
        private BenchmarkStatusReporter statusReporter;
        private SqlStatementExecutor sqlStatementExecutor;
        private Optional<Integer> waitSecondsBeforeExecutionReporting;

        public QueryExecutionCallable(QueryExecution queryExecution, BenchmarkStatusReporter statusReporter, SqlStatementExecutor sqlStatementExecutor, Optional<Integer> waitSecondsBeforeExecutionReporting)
        {
            this.queryExecution = queryExecution;
            this.statusReporter = statusReporter;
            this.sqlStatementExecutor = sqlStatementExecutor;
            this.waitSecondsBeforeExecutionReporting = waitSecondsBeforeExecutionReporting;
        }

        @Override
        public QueryExecutionResult call()
                throws Exception
        {
            statusReporter.reportExecutionStarted(queryExecution);

            QueryExecutionResult result = sqlStatementExecutor.executeQuery(queryExecution);

            if (waitSecondsBeforeExecutionReporting.isPresent()) {
                TimeUnit.SECONDS.sleep(waitSecondsBeforeExecutionReporting.get());
            }

            statusReporter.reportExecutionFinished(result);

            return result;
        }
    }
}
