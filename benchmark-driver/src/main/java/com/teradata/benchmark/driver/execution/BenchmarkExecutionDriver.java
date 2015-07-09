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
import com.teradata.benchmark.driver.execution.QueryExecutionResult.QueryExecutionResultBuilder;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkStatusReporter;
import com.teradata.benchmark.driver.loader.BenchmarkLoader;
import com.teradata.benchmark.driver.macro.MacroService;
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

    private String benchmarkExecutionSequenceId()
    {
        return properties.getExecutionSequenceId().orElse(nowUtc().format(DATE_TIME_FORMATTER));
    }

    private boolean run(List<Benchmark> benchmarks)
    {
        List<BenchmarkExecutionResult> benchmarkExecutionResults = newArrayList();
        int benchmarkOrdinalNumber = 1;
        for (Benchmark benchmark : benchmarks) {
            benchmarkExecutionResults.add(executePrewarmAndBenchmark(benchmark, benchmarkOrdinalNumber++, benchmarks.size()));
        }

        return benchmarkExecutionResults.stream()
                .allMatch(BenchmarkExecutionResult::isSuccessful);
    }

    private BenchmarkExecutionResult executePrewarmAndBenchmark(Benchmark benchmark, int benchmarkOrdinalNumber, int benchmarkTotalCount)
    {
        try {
            LOG.info("[{} of {}] processing benchmark: {}", benchmarkOrdinalNumber, benchmarkTotalCount, benchmark);

            macroService.runBenchmarkMacros(benchmark.getBeforeBenchmarkMacros(), benchmark);

            boolean prewarmSuccessful = executePrewarm(benchmark);
            if (!prewarmSuccessful) {
                LOG.error("Prewarm for benchmark {} failed, skipping this benchmark", benchmark);
                return new BenchmarkExecutionResultBuilder(benchmark)
                        .withPrewarmFailed()
                        .build();
            }

            statusReporter.reportBenchmarkStarted(benchmark);

            BenchmarkExecutionResultBuilder resultBuilder = new BenchmarkExecutionResultBuilder(benchmark)
                    .startTimer();

            List<QueryExecutionResult> executions = executeBenchmarkQueries(benchmark);

            BenchmarkExecutionResult executionResult = resultBuilder
                    .endTimer()
                    .setExecutions(executions)
                    .build();

            executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmark);

            statusReporter.reportBenchmarkFinished(executionResult);

            return executionResult;
        }
        finally {
            try {
                macroService.runBenchmarkMacros(benchmark.getAfterBenchmarkMacros(), benchmark);
            }
            catch (RuntimeException e) {
                LOG.error("Error while running after benchmark macros ({})", benchmark.getAfterBenchmarkMacros(), e);
            }
        }
    }

    private boolean executePrewarm(Benchmark benchmark)
    {
        try {
            if (benchmark.getPrewarmRuns() < 1) {
                return true;
            }
            LOG.info("Executing prewarm for benchmark: {}", benchmark);

            ListeningExecutorService executorService = executorServiceFactory.create(benchmark.getConcurrency());

            List<ListenableFuture<QueryExecutionResult>> executionFutures = newArrayList();
            for (Query query : benchmark.getQueries()) {
                for (int run = 0; run < benchmark.getPrewarmRuns(); ++run) {
                    QueryExecution queryExecution = new QueryExecution(benchmark, query, run);
                    executionFutures.add(executorService.submit(() -> {
                        try {
                            LOG.info("Executing prewarm query: {} ({})", query, queryExecution.getRun());

                            return queryExecutionDriver.execute(queryExecution);
                        }
                        finally {
                            LOG.info("Executed prewarm query: {} ({})", query, queryExecution.getRun());
                        }
                    }));
                }
            }

            boolean prewarmSuccessful = Futures.allAsList(executionFutures).get().stream()
                    .allMatch(QueryExecutionResult::isSuccessful);

            LOG.info("Finished prewarm for benchmark: {}, successful: {}", benchmark, prewarmSuccessful);

            executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmark);

            return prewarmSuccessful;
        }
        catch (ExecutionException e) {
            LOG.error("Error while executing prewarm for benchmark {}", benchmark, e);
            return false;
        }
        catch (InterruptedException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark prewarm", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<QueryExecutionResult> executeBenchmarkQueries(Benchmark benchmark)
    {
        try {
            ListeningExecutorService executorService = executorServiceFactory.create(benchmark.getConcurrency());
            List<Callable<QueryExecutionResult>> queryExecutionCallables = buildQueryExecutionCallables(benchmark);
            List<ListenableFuture<QueryExecutionResult>> executionFutures = (List) executorService.invokeAll(queryExecutionCallables);
            return Futures.allAsList(executionFutures).get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
    }

    private List<Callable<QueryExecutionResult>> buildQueryExecutionCallables(Benchmark benchmark)
    {
        List<Callable<QueryExecutionResult>> executionCallables = newArrayList();
        for (Query query : benchmark.getQueries()) {
            for (int run = 0; run < benchmark.getRuns(); ++run) {
                QueryExecution queryExecution = new QueryExecution(benchmark, query, run);

                executionCallables.add(() -> {
                    QueryExecutionResult result;
                    statusReporter.reportExecutionStarted(queryExecution);
                    try {
                        result = queryExecutionDriver.execute(queryExecution);
                    }
                    catch (Exception e) {
                        result = new QueryExecutionResultBuilder(queryExecution)
                                .failed(e)
                                .build();
                    }

                    executionSynchronizer.awaitAfterQueryExecutionAndBeforeResultReport(result);

                    statusReporter.reportExecutionFinished(result);
                    return result;
                });
            }
        }
        return executionCallables;
    }
}
