/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.concurrent.ExecutorServiceFactory;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult.BenchmarkExecutionResultBuilder;
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

    @Autowired
    private ExecutionSynchronizer executionSynchronizer;

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
        List<BenchmarkExecutionResult> benchmarkExecutionResults = benchmarks.stream()
                .map(this::executePrewarmAndBenchmark)
                .collect(toList());
        suiteStatusReporter.reportSuiteFinished(benchmarkExecutionResults);

        return !benchmarkExecutionResults.stream()
                .filter(result -> !result.isSuccessful())
                .findAny().isPresent();
    }

    private String benchmarkExecutionSequenceId()
    {
        return properties.getExecutionSequenceId().orElse(nowUtc().format(DATE_TIME_FORMATTER));
    }

    private BenchmarkExecutionResult executePrewarmAndBenchmark(Benchmark benchmark)
    {
        try {
            executeBeforeBenchmarkMacros(benchmark);

            BenchmarkExecution prewarmBenchmarkExecution = new BenchmarkExecution(benchmark, benchmark.getConcurrency(), benchmark.getPrewarmRuns());
            BenchmarkExecutionResult prewarmResult = executeBenchmark(prewarmBenchmarkExecution, prewarmStatusReporter);
            if (!prewarmResult.isSuccessful()) {
                return prewarmResult;
            }

            BenchmarkExecution benchmarkExecution = new BenchmarkExecution(benchmark, benchmark.getConcurrency(), benchmark.getRuns());
            return executeBenchmark(benchmarkExecution, benchmarkStatusReporter);
        }
        catch (InterruptedException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
    }

    private void executeBeforeBenchmarkMacros(Benchmark benchmark)
    {
        macroService.runMacros(benchmark.getBeforeBenchmarkMacros());
    }

    private BenchmarkExecutionResult executeBenchmark(BenchmarkExecution benchmarkExecution, BenchmarkStatusReporter benchmarkStatusReporter)
            throws InterruptedException
    {
        ListeningExecutorService executorService = executorServiceFactory.create(benchmarkExecution.getConcurrency());
        try {
            benchmarkStatusReporter.reportBenchmarkStarted(benchmarkExecution);
            BenchmarkExecutionResultBuilder resultBuilder = new BenchmarkExecutionResultBuilder(benchmarkExecution);
            resultBuilder.startTimer();

            List<QueryExecutionResult> executionResults = runQueries(benchmarkExecution, benchmarkStatusReporter, executorService);

            BenchmarkExecutionResult benchmarkExecutionResult = resultBuilder
                    .endTimer()
                    .setExecutions(executionResults)
                    .build();

            executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmarkExecutionResult);

            benchmarkStatusReporter.reportBenchmarkFinished(benchmarkExecutionResult);

            return benchmarkExecutionResult;
        }
        finally {
            executorService.shutdown();
        }
    }

    private List<QueryExecutionResult> runQueries(BenchmarkExecution benchmarkExecution, BenchmarkStatusReporter benchmarkStatusReporter, ListeningExecutorService executorService)
            throws InterruptedException
    {
        try {
            List<Callable<QueryExecutionResult>> queryExecutionCallables = buildQueryExecutionCallables(benchmarkExecution, benchmarkStatusReporter);
            @SuppressWarnings("unchecked")
            List<ListenableFuture<QueryExecutionResult>> executionFutures = (List) executorService.invokeAll(queryExecutionCallables);
            return Futures.allAsList(executionFutures).get();
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new BenchmarkExecutionException("Could not execute benchmark query: " + cause.getMessage(), cause);
        }
    }

    private List<Callable<QueryExecutionResult>> buildQueryExecutionCallables(BenchmarkExecution benchmarkExecution, BenchmarkStatusReporter benchmarkStatusReporter)
    {
        List<Callable<QueryExecutionResult>> executionCallables = newArrayList();
        for (Query query : benchmarkExecution.getQueries()) {
            for (int run = 0; run < benchmarkExecution.getRuns(); ++run) {
                QueryExecution queryExecution = new QueryExecution(benchmarkExecution, query, run);
                executionCallables.add(() -> {
                    return queryExecutionDriver.execute(queryExecution, benchmarkStatusReporter, executionSynchronizer);
                });
            }
        }
        return executionCallables;
    }
}
