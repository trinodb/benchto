/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.execution;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.Query;
import com.teradata.benchto.driver.concurrent.ExecutorServiceFactory;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult.BenchmarkExecutionResultBuilder;
import com.teradata.benchto.driver.execution.QueryExecutionResult.QueryExecutionResultBuilder;
import com.teradata.benchto.driver.listeners.benchmark.BenchmarkStatusReporter;
import com.teradata.benchto.driver.macro.MacroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

@Component
public class BenchmarkExecutionDriver
{
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkExecutionDriver.class);

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

    @Autowired
    private ApplicationContext applicationContext;

    public BenchmarkExecutionResult execute(Benchmark benchmark, int benchmarkOrdinalNumber, int benchmarkTotalCount)
    {
        LOG.info("[{} of {}] processing benchmark: {}", benchmarkOrdinalNumber, benchmarkTotalCount, benchmark);

        BenchmarkExecutionResult benchmarkExecutionResult = null;
        try {

            macroService.runBenchmarkMacros(benchmark.getBeforeBenchmarkMacros(), Optional.of(benchmark));

            benchmarkExecutionResult = executeBenchmark(benchmark);

            macroService.runBenchmarkMacros(benchmark.getAfterBenchmarkMacros(), Optional.of(benchmark));

            return benchmarkExecutionResult;
        }
        catch (Exception e) {
            if (benchmarkExecutionResult == null || benchmarkExecutionResult.isSuccessful()) {
                return failedBenchmarkResult(benchmark, e);
            }
            else {
                checkState(!benchmarkExecutionResult.isSuccessful(), "Benchmark is already failed.");
                LOG.error("Error while running after benchmark macros for successful benchmark({})",
                        benchmark.getAfterBenchmarkMacros(), e);
                return benchmarkExecutionResult;
            }
        }
    }

    private BenchmarkExecutionResult executeBenchmark(Benchmark benchmark)
    {
        try {
            executePrewarm(benchmark);

            statusReporter.reportBenchmarkStarted(benchmark);

            BenchmarkExecutionResultBuilder resultBuilder = new BenchmarkExecutionResultBuilder(benchmark)
                    .startTimer();

            List<QueryExecutionResult> executions = executeBenchmarkQueries(benchmark);

            BenchmarkExecutionResult executionResult = resultBuilder
                    .endTimer()
                    .withExecutions(executions)
                    .build();

            executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmark);

            statusReporter.reportBenchmarkFinished(executionResult);

            return executionResult;
        }
        catch (RuntimeException e) {
            return failedBenchmarkResult(benchmark, e);
        }
    }

    private BenchmarkExecutionResult failedBenchmarkResult(Benchmark benchmark, Exception e)
    {
        return new BenchmarkExecutionResultBuilder(benchmark)
                .withUnexpectedException(e)
                .build();
    }

    private void executePrewarm(Benchmark benchmark)
    {
        ListeningExecutorService executorService = executorServiceFactory.create(benchmark.getConcurrency());
        try {
            if (benchmark.getPrewarmRuns() < 1) {
                return;
            }
            LOG.info("Executing prewarm for benchmark: {}", benchmark.getUniqueName());

            List<ListenableFuture<QueryExecutionResult>> executionFutures = newArrayList();
            for (Query query : benchmark.getQueries()) {
                for (int run = 0; run < benchmark.getPrewarmRuns(); ++run) {
                    QueryExecution queryExecution = new QueryExecution(benchmark, query, run);
                    executionFutures.add(executorService.submit(() -> {
                        LOG.info("Executing prewarm query: {} ({})", query.getName(), queryExecution.getRun());

                        try (Connection connection = getConnectionFor(queryExecution)) {
                            return queryExecutionDriver.execute(queryExecution, connection);
                        }
                        finally {
                            LOG.info("Executed prewarm query: {} ({})", query.getName(), queryExecution.getRun());
                        }
                    }));
                }
            }

            boolean prewarmSuccessful = Futures.allAsList(executionFutures).get().stream()
                    .allMatch(QueryExecutionResult::isSuccessful);

            LOG.info("Finished prewarm for benchmark: {}, successful: {}", benchmark.getUniqueName(), prewarmSuccessful);

            executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmark);
        }
        catch (Exception e) {
            throw new BenchmarkExecutionException("Could not execute benchmark prewarm", e);
        }
        finally {
            executorService.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private List<QueryExecutionResult> executeBenchmarkQueries(Benchmark benchmark)
    {
        ListeningExecutorService executorService = executorServiceFactory.create(benchmark.getConcurrency());
        try {
            List<Callable<QueryExecutionResult>> queryExecutionCallables = buildQueryExecutionCallables(benchmark);
            List<ListenableFuture<QueryExecutionResult>> executionFutures = (List) executorService.invokeAll(queryExecutionCallables);
            return Futures.allAsList(executionFutures).get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
        finally {
            executorService.shutdown();
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

                    try (Connection connection = getConnectionFor(queryExecution)) {
                        macroService.runBenchmarkMacros(benchmark.getBeforeExecutionMacros(), Optional.of(benchmark), Optional.of(connection));

                        statusReporter.reportExecutionStarted(queryExecution);
                        try {
                            result = queryExecutionDriver.execute(queryExecution, connection);
                        }
                        catch (Exception e) {
                            result = new QueryExecutionResultBuilder(queryExecution)
                                    .failed(e)
                                    .build();
                        }

                        executionSynchronizer.awaitAfterQueryExecutionAndBeforeResultReport(result);

                        statusReporter.reportExecutionFinished(result);

                        macroService.runBenchmarkMacros(benchmark.getAfterExecutionMacros(), Optional.of(benchmark), Optional.of(connection));
                    }

                    return result;
                });
            }
        }
        return executionCallables;
    }

    private Connection getConnectionFor(QueryExecution queryExecution)
            throws SQLException
    {
        return applicationContext.getBean(queryExecution.getBenchmark().getDataSource(), DataSource.class).getConnection();
    }
}
