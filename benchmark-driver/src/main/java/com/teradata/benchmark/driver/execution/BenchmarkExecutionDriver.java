/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.concurrent.ExecutorServiceFactory;
import com.teradata.benchmark.driver.domain.Benchmark;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.BenchmarkResult.BenchmarkResultBuilder;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import com.teradata.benchmark.driver.listeners.BenchmarkStatusReporter;
import com.teradata.benchmark.driver.loader.BenchmarkLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.google.common.collect.Lists.newArrayList;
import static com.teradata.benchmark.driver.utils.TimeUtils.nowUtc;
import static java.util.stream.Collectors.toList;

@Component
public class BenchmarkExecutionDriver
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS");
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkExecutionDriver.class);

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private BenchmarkLoader benchmarkLoader;

    @Autowired
    private QueryExecutionDriver queryExecutionDriver;

    @Autowired
    private BenchmarkStatusReporter statusReporter;

    @Autowired
    private ExecutorServiceFactory executorServiceFactory;

    /**
     * @return true if all benchmark queries passed
     */
    public boolean run()
    {
        String executionSequenceId = benchmarkExecutionSequenceId();

        LOG.info("Running benchmarks(executionSequenceId={}) with properties: {}", executionSequenceId, properties);

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks(executionSequenceId);
        LOG.info("Loaded {} benchmarks", benchmarks.size());

        List<BenchmarkResult> benchmarkResults = benchmarks.stream()
                .map(this::executeBenchmark)
                .collect(toList());
        statusReporter.reportSuiteFinished(benchmarkResults);

        return !benchmarkResults.stream()
                .filter(result -> !result.isSuccessful())
                .findAny().isPresent();
    }

    private String benchmarkExecutionSequenceId()
    {
        return properties.getExecutionSequenceId().orElse(nowUtc().format(DATE_TIME_FORMATTER));
    }

    private BenchmarkResult executeBenchmark(Benchmark benchmark)
    {
        statusReporter.reportBenchmarkStarted(benchmark);

        BenchmarkResultBuilder benchmarkResultBuilder = new BenchmarkResultBuilder(benchmark)
                .startTimer();

        ListeningExecutorService executorService = executorServiceFactory.create(benchmark.getConcurrency());
        try {
            List<QueryExecutionResult> executionResults = runQueries(benchmark, executorService);

            BenchmarkResult benchmarkResult = benchmarkResultBuilder
                    .endTimer()
                    .setExecutions(executionResults)
                    .build();

            statusReporter.reportBenchmarkFinished(benchmarkResult);

            return benchmarkResult;
        }
        catch (InterruptedException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
        finally {
            executorService.shutdown();
        }
    }

    private List<QueryExecutionResult> runQueries(Benchmark benchmark, ListeningExecutorService executorService)
            throws InterruptedException
    {
        try {
            @SuppressWarnings("unchecked")
            List<ListenableFuture<QueryExecutionResult>> executionFutures = (List) executorService.invokeAll(buildQueryExecutionCallables(benchmark));
            return Futures.allAsList(executionFutures).get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BenchmarkExecutionException("Query execution was interrupted", e);
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new BenchmarkExecutionException("Could not execute benchmark query: " + cause.getMessage(), cause);
        }
    }

    private List<Callable<QueryExecutionResult>> buildQueryExecutionCallables(Benchmark benchmark)
    {
        List<Callable<QueryExecutionResult>> executionCallables = newArrayList();
        for (Query query : benchmark.getQueries()) {
            for (int run = 0; run < benchmark.getRuns(); ++run) {
                QueryExecution queryExecution = new QueryExecution(benchmark, query, run);
                executionCallables.add(() -> queryExecutionDriver.execute(queryExecution));
            }
        }
        return executionCallables;
    }
}
