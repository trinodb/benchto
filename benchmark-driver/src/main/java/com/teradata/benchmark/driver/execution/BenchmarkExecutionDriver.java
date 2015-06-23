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
import com.teradata.benchmark.driver.domain.BenchmarkExecution;
import com.teradata.benchmark.driver.domain.BenchmarkResult;
import com.teradata.benchmark.driver.domain.BenchmarkResult.BenchmarkResultBuilder;
import com.teradata.benchmark.driver.domain.QueryExecution;
import com.teradata.benchmark.driver.domain.QueryExecutionResult;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;
import com.teradata.benchmark.driver.listeners.suite.SuiteStatusReporter;
import com.teradata.benchmark.driver.loader.BenchmarkLoader;
import com.teradata.benchmark.driver.macro.MacroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Qualifier("prewarmStatusReporter")
    @Autowired
    private BenchmarkStatusReporter prewarmStatusReporter;

    @Qualifier("benchmarkStatusReporter")
    @Autowired
    private BenchmarkStatusReporter benchmarkStatusReporter;

    @Autowired
    private SuiteStatusReporter suiteStatusReporter;

    @Autowired
    private ExecutorServiceFactory executorServiceFactory;

    @Autowired
    private MacroService macroService;

    /**
     * @return true if all benchmark queries passed
     */
    public boolean run()
    {
        String executionSequenceId = benchmarkExecutionSequenceId();
        LOG.info("Running benchmarks(executionSequenceId={}) with properties: {}", executionSequenceId, properties);

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks(executionSequenceId);
        LOG.info("Loaded {} benchmarks", benchmarks.size());

        return run(benchmarks);
    }

    public boolean run(List<Benchmark> benchmarks)
    {
        List<BenchmarkResult> benchmarkResults = benchmarks.stream()
                .map(this::executePrewarmAndBenchmark)
                .collect(toList());
        suiteStatusReporter.reportSuiteFinished(benchmarkResults);

        return !benchmarkResults.stream()
                .filter(result -> !result.isSuccessful())
                .findAny().isPresent();
    }

    private String benchmarkExecutionSequenceId()
    {
        return properties.getExecutionSequenceId().orElse(nowUtc().format(DATE_TIME_FORMATTER));
    }

    private BenchmarkResult executePrewarmAndBenchmark(Benchmark benchmark)
    {
        try {
            executeBeforeBenchmarkMacros(benchmark);

            BenchmarkResult prewarmResult = executeBenchmark(new BenchmarkExecution(benchmark, prewarmStatusReporter, benchmark.getConcurrency(), benchmark.getPrewarmRuns()));
            if (!prewarmResult.isSuccessful()) {
                return prewarmResult;
            }

            return executeBenchmark(new BenchmarkExecution(benchmark, benchmarkStatusReporter, benchmark.getConcurrency(), benchmark.getRuns()));
        }
        catch (InterruptedException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
    }

    private void executeBeforeBenchmarkMacros(Benchmark benchmark)
    {
        macroService.runMacros(benchmark.getBeforeBenchmarkMacros());
    }

    private BenchmarkResult executeBenchmark(BenchmarkExecution benchmarkExecution)
            throws InterruptedException
    {
        ListeningExecutorService executorService = executorServiceFactory.create(benchmarkExecution.getConcurrency());
        try {
            benchmarkExecution.getStatusReporter().reportBenchmarkStarted(benchmarkExecution.getBenchmark());
            BenchmarkResultBuilder benchmarkResultBuilder = new BenchmarkResultBuilder(benchmarkExecution.getBenchmark());
            benchmarkResultBuilder.startTimer();

            List<QueryExecutionResult> executionResults = runQueries(benchmarkExecution, executorService);

            BenchmarkResult benchmarkResult = benchmarkResultBuilder
                    .endTimer()
                    .setExecutions(executionResults)
                    .build();

            benchmarkExecution.getStatusReporter().reportBenchmarkFinished(benchmarkResult);

            return benchmarkResult;
        }
        finally {
            executorService.shutdown();
        }
    }

    private List<QueryExecutionResult> runQueries(BenchmarkExecution benchmarkExecution, ListeningExecutorService executorService)
            throws InterruptedException
    {
        try {
            @SuppressWarnings("unchecked")
            List<ListenableFuture<QueryExecutionResult>> executionFutures = (List) executorService.invokeAll(buildQueryExecutionCallables(benchmarkExecution));
            return Futures.allAsList(executionFutures).get();
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new BenchmarkExecutionException("Could not execute benchmark query: " + cause.getMessage(), cause);
        }
    }

    private List<Callable<QueryExecutionResult>> buildQueryExecutionCallables(BenchmarkExecution benchmarkExecution)
    {
        List<Callable<QueryExecutionResult>> executionCallables = newArrayList();
        for (Query query : benchmarkExecution.getBenchmark().getQueries()) {
            for (int run = 0; run < benchmarkExecution.getRuns(); ++run) {
                QueryExecution queryExecution = new QueryExecution(benchmarkExecution.getBenchmark(), query, run, benchmarkExecution.getStatusReporter());
                executionCallables.add(() -> queryExecutionDriver.execute(queryExecution));
            }
        }
        return executionCallables;
    }
}
