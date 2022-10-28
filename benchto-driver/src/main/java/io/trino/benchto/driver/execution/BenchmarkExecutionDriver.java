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
package io.trino.benchto.driver.execution;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.BenchmarkExecutionException;
import io.trino.benchto.driver.BenchmarkProperties;
import io.trino.benchto.driver.Query;
import io.trino.benchto.driver.concurrent.ExecutorServiceFactory;
import io.trino.benchto.driver.execution.BenchmarkExecutionResult.BenchmarkExecutionResultBuilder;
import io.trino.benchto.driver.execution.QueryExecutionResult.QueryExecutionResultBuilder;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkStatusReporter;
import io.trino.benchto.driver.loader.SqlStatementGenerator;
import io.trino.benchto.driver.macro.MacroService;
import io.trino.benchto.driver.utils.PermutationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Lists.newArrayList;
import static io.trino.benchto.driver.utils.QueryUtils.isSelectQuery;
import static io.trino.benchto.driver.utils.TimeUtils.nowUtc;
import static java.lang.String.format;

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

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private SqlStatementGenerator sqlStatementGenerator;

    public BenchmarkExecutionResult execute(Benchmark benchmark, int benchmarkOrdinalNumber, int benchmarkTotalCount, Optional<ZonedDateTime> executionTimeLimit)
    {
        LOG.info("[{} of {}] processing benchmark: {}", benchmarkOrdinalNumber, benchmarkTotalCount, benchmark);

        BenchmarkExecutionResult benchmarkExecutionResult = null;
        try {
            macroService.runBenchmarkMacros(benchmark.getBeforeBenchmarkMacros(), benchmark);

            benchmarkExecutionResult = executeBenchmark(benchmark, executionTimeLimit);

            macroService.runBenchmarkMacros(benchmark.getAfterBenchmarkMacros(), benchmark);

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

    private BenchmarkExecutionResult executeBenchmark(Benchmark benchmark, Optional<ZonedDateTime> executionTimeLimit)
    {
        BenchmarkExecutionResultBuilder resultBuilder = new BenchmarkExecutionResultBuilder(benchmark);
        List<QueryExecutionResult> executions;
        try {
            executions = executeQueries(benchmark, benchmark.getPrewarmRuns(), true, executionTimeLimit);
            String comparisonFailures = executions.stream()
                    .filter(execution -> execution.getFailureCause() != null)
                    .filter(execution -> execution.getFailureCause().getClass().equals(ResultComparisonException.class))
                    .map(execution -> format("%s [%s]", execution.getQueryName(), execution.getFailureCause()))
                    .distinct()
                    .collect(Collectors.joining("\n"));

            executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmark);

            statusReporter.reportBenchmarkStarted(benchmark);

            resultBuilder = resultBuilder.startTimer();

            try {
                if (!comparisonFailures.isEmpty()) {
                    throw new RuntimeException(format("Query result comparison failed for queries: %s", comparisonFailures));
                }
                executions = executeQueries(benchmark, benchmark.getRuns(), false, executionTimeLimit);
                resultBuilder = resultBuilder.withExecutions(executions);
            }
            finally {
                resultBuilder = resultBuilder.endTimer();
            }
        }
        catch (RuntimeException e) {
            resultBuilder = resultBuilder.withUnexpectedException(e);
        }

        BenchmarkExecutionResult executionResult = resultBuilder.build();

        statusReporter.reportBenchmarkFinished(executionResult);

        return executionResult;
    }

    private BenchmarkExecutionResult failedBenchmarkResult(Benchmark benchmark, Exception e)
    {
        return new BenchmarkExecutionResultBuilder(benchmark)
                .withUnexpectedException(e)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<QueryExecutionResult> executeQueries(Benchmark benchmark, int runs, boolean warmup, Optional<ZonedDateTime> executionTimeLimit)
    {
        ListeningExecutorService executorService = executorServiceFactory.create(benchmark.getConcurrency());
        try {
            if (benchmark.isThroughputTest()) {
                List<Callable<List<QueryExecutionResult>>> queryExecutionCallables = buildConcurrencyQueryExecutionCallables(benchmark, runs, warmup, executionTimeLimit);
                List<ListenableFuture<List<QueryExecutionResult>>> executionFutures = (List) executorService.invokeAll(queryExecutionCallables);
                return Futures.allAsList(executionFutures).get().stream()
                        .flatMap(List::stream)
                        .collect(toImmutableList());
            }
            else {
                List<Callable<QueryExecutionResult>> queryExecutionCallables = buildQueryExecutionCallables(benchmark, runs, warmup);
                List<ListenableFuture<QueryExecutionResult>> executionFutures = (List) executorService.invokeAll(queryExecutionCallables);
                return Futures.allAsList(executionFutures).get();
            }
        }
        catch (InterruptedException | ExecutionException e) {
            throw new BenchmarkExecutionException("Could not execute benchmark", e);
        }
        finally {
            executorService.shutdown();
        }
    }

    private List<Callable<QueryExecutionResult>> buildQueryExecutionCallables(Benchmark benchmark, int runs, boolean warmup)
    {
        List<Callable<QueryExecutionResult>> executionCallables = newArrayList();
        for (Query query : benchmark.getQueries()) {
            for (int run = 1; run <= runs; run++) {
                final int finalRun = run;
                QueryExecution queryExecution = new QueryExecution(benchmark, query, run, sqlStatementGenerator);
                Optional<Path> resultFile = benchmark.getQueryResults()
                        // only check result of the first warmup run or all runs of non select statements
                        .filter(dir -> (warmup && finalRun == 1) || (!isSelectQuery(query.getSqlTemplate())))
                        .map(queryResult -> properties.getQueryResultsDir().resolve(queryResult));
                executionCallables.add(() -> {
                    try (Connection connection = getConnectionFor(queryExecution)) {
                        return executeSingleQuery(queryExecution, benchmark, connection, warmup, Optional.empty(), resultFile);
                    }
                });
            }
        }
        return executionCallables;
    }

    private List<Callable<List<QueryExecutionResult>>> buildConcurrencyQueryExecutionCallables(Benchmark benchmark, int runs, boolean warmup, Optional<ZonedDateTime> executionTimeLimit)
    {
        List<Callable<List<QueryExecutionResult>>> executionCallables = newArrayList();
        for (int thread = 0; thread < benchmark.getConcurrency(); thread++) {
            int finalThread = thread;
            executionCallables.add(() -> {
                LOG.info("Running throughput test: {} queries, {} runs", benchmark.getQueries().size(), runs);
                int[] queryOrder = PermutationUtils.preparePermutation(benchmark.getQueries().size(), finalThread);
                List<QueryExecutionResult> queryExecutionResults = executeConcurrentQueries(benchmark, runs, warmup, executionTimeLimit, finalThread, queryOrder);
                if (!warmup) {
                    statusReporter.reportConcurrencyTestExecutionFinished(queryExecutionResults);
                }
                return queryExecutionResults;
            });
        }
        return executionCallables;
    }

    private List<QueryExecutionResult> executeConcurrentQueries(Benchmark benchmark, int runs, boolean warmup, Optional<ZonedDateTime> executionTimeLimit, int threadNumber, int[] queryOrder)
            throws SQLException
    {
        boolean firstQuery = true;
        List<QueryExecutionResult> queryExecutionResults = newArrayList();
        try (Connection connection = getConnectionFor(new QueryExecution(benchmark, benchmark.getQueries().get(0), 0, sqlStatementGenerator))) {
            for (int run = 1; run <= runs; run++) {
                for (int queryIndex = 0; queryIndex < benchmark.getQueries().size(); queryIndex++) {
                    int permutedQueryIndex = queryIndex;
                    if (warmup) {
                        if (queryIndex % benchmark.getConcurrency() != threadNumber) {
                            // for pre-warming we split queries among all threads instead
                            // of each thread running all queries
                            continue;
                        }
                        LOG.info("Executing pre-warm query {}", queryIndex);
                    }
                    else {
                        permutedQueryIndex = queryOrder[queryIndex];
                    }
                    Query query = benchmark.getQueries().get(permutedQueryIndex);
                    int sequenceId = queryIndex
                            + threadNumber * benchmark.getQueries().size()
                            + (run - 1) * benchmark.getConcurrency() * benchmark.getQueries().size();
                    QueryExecution queryExecution = new QueryExecution(benchmark, query, sequenceId, sqlStatementGenerator);
                    if (firstQuery && !warmup) {
                        statusReporter.reportExecutionStarted(queryExecution);
                        firstQuery = false;
                    }
                    try {
                        // We want to skip a reporting for concurrency benchmarks because it is unnecessary overhead.
                        // In concurrency benchmarks we are not interested in result for specific query
                        queryExecutionResults.add(executeSingleQuery(queryExecution, benchmark, connection, true, executionTimeLimit));
                    }
                    catch (TimeLimitException e) {
                        LOG.warn("Interrupting benchmark {} due to time limit exceeded", benchmark.getName());
                        return queryExecutionResults;
                    }
                }
            }
        }
        return queryExecutionResults;
    }

    private QueryExecutionResult executeSingleQuery(
            QueryExecution queryExecution,
            Benchmark benchmark,
            Connection connection,
            boolean skipReport,
            Optional<ZonedDateTime> executionTimeLimit)
            throws TimeLimitException
    {
        return executeSingleQuery(queryExecution, benchmark, connection, skipReport, executionTimeLimit, Optional.empty());
    }

    private QueryExecutionResult executeSingleQuery(
            QueryExecution queryExecution,
            Benchmark benchmark,
            Connection connection,
            boolean skipReport,
            Optional<ZonedDateTime> executionTimeLimit,
            Optional<Path> outputFile)
            throws TimeLimitException
    {
        QueryExecutionResult result;
        macroService.runBenchmarkMacros(benchmark.getBeforeExecutionMacros(), benchmark, connection);

        if (!skipReport) {
            statusReporter.reportExecutionStarted(queryExecution);
        }
        QueryExecutionResultBuilder failureResult = new QueryExecutionResultBuilder(queryExecution)
                .startTimer();
        try {
            result = queryExecutionDriver.execute(queryExecution, connection, outputFile);
        }
        catch (Exception e) {
            LOG.error(format("Query Execution failed for benchmark %s query %s", benchmark.getName(), queryExecution.getQueryName()), e);
            result = failureResult
                    .endTimer()
                    .failed(e)
                    .build();
        }
        if (isTimeLimitExceeded(executionTimeLimit)) {
            throw new TimeLimitException(benchmark, queryExecution);
        }

        if (!skipReport) {
            statusReporter.reportExecutionFinished(result);
        }

        macroService.runBenchmarkMacros(benchmark.getAfterExecutionMacros(), benchmark, connection);
        return result;
    }

    private Connection getConnectionFor(QueryExecution queryExecution)
            throws SQLException
    {
        return applicationContext.getBean(queryExecution.getBenchmark().getDataSource(), DataSource.class).getConnection();
    }

    private boolean isTimeLimitExceeded(Optional<ZonedDateTime> executionTimeLimit)
    {
        return executionTimeLimit.map(limit -> limit.compareTo(nowUtc()) < 0).orElse(false);
    }

    static class TimeLimitException
            extends RuntimeException
    {
        public TimeLimitException(Benchmark benchmark, QueryExecution queryExecution)
        {
            super(format(
                    "Query execution exceeded time limit for benchmark %s query %s",
                    benchmark.getName(),
                    queryExecution.getQueryName()));
        }
    }
}
