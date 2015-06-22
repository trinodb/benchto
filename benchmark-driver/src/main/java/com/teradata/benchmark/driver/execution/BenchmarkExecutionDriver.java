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
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;
import com.teradata.benchmark.driver.listeners.suite.SuiteStatusReporter;
import com.teradata.benchmark.driver.loader.BenchmarkLoader;
import com.teradata.benchmark.driver.macro.MacroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

            for (int i = 0; i < benchmark.getPrewarmRepeats(); ++i) {
                BenchmarkResult result = executeBenchmark(benchmark, prewarmStatusReporter);
                if (!result.isSuccessful()) {
                    return result;
                }
            }

            return executeBenchmark(benchmark, benchmarkStatusReporter);
        }
        catch (InterruptedException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
    }

    private void executeBeforeBenchmarkMacros(Benchmark benchmark)
    {
        macroService.runMacros(benchmark.getBeforeBenchmarkMacros());
    }

    private BenchmarkResult executeBenchmark(Benchmark benchmark, BenchmarkStatusReporter statusReporter)
            throws InterruptedException
    {
        ListeningExecutorService executorService = executorServiceFactory.create(benchmark.getConcurrency());
        try {
            statusReporter.reportBenchmarkStarted(benchmark);
            BenchmarkResultBuilder benchmarkResultBuilder = new BenchmarkResultBuilder(benchmark);
            benchmarkResultBuilder.startTimer();

            List<QueryExecutionResult> executionResults = runQueries(benchmark, executorService, statusReporter);

            BenchmarkResult benchmarkResult = benchmarkResultBuilder
                    .endTimer()
                    .setExecutions(executionResults)
                    .build();

            statusReporter.reportBenchmarkFinished(benchmarkResult);

            return benchmarkResult;
        }
        finally {
            executorService.shutdown();
        }
    }

    private List<QueryExecutionResult> runQueries(Benchmark benchmark, ListeningExecutorService executorService, BenchmarkStatusReporter statusReporter)
            throws InterruptedException
    {
        try {
            @SuppressWarnings("unchecked")
            List<ListenableFuture<QueryExecutionResult>> executionFutures = (List) executorService.invokeAll(buildQueryExecutionCallables(benchmark, statusReporter));
            return Futures.allAsList(executionFutures).get();
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new BenchmarkExecutionException("Could not execute benchmark query: " + cause.getMessage(), cause);
        }
    }

    private List<Callable<QueryExecutionResult>> buildQueryExecutionCallables(Benchmark benchmark, BenchmarkStatusReporter statusReporter)
    {
        List<Callable<QueryExecutionResult>> executionCallables = newArrayList();
        for (Query query : benchmark.getQueries()) {
            for (int run = 0; run < benchmark.getRuns(); ++run) {
                QueryExecution queryExecution = new QueryExecution(benchmark, query, run);
                executionCallables.add(() -> queryExecutionDriver.execute(queryExecution, statusReporter));
            }
        }
        return executionCallables;
    }
}
