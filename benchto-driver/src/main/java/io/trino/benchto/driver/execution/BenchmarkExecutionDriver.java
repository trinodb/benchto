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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Lists.newArrayList;
import static io.trino.benchto.driver.utils.QueryUtils.isSelectQuery;
import static io.trino.benchto.driver.utils.TimeUtils.nowUtc;
import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

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

    public List<BenchmarkExecutionResult> execute(List<Benchmark> benchmarks, int benchmarkOrdinalNumber, int benchmarkTotalCount, Optional<ZonedDateTime> executionTimeLimit)
    {
        checkState(benchmarks.size() != 0, "List of benchmarks to execute cannot be empty.");
        for (int i = 0; i < benchmarks.size(); i++) {
            LOG.info("[{} of {}] processing benchmark: {}", benchmarkOrdinalNumber + i, benchmarkTotalCount, benchmarks.get(i));
        }
        Benchmark firstBenchmark = benchmarks.get(0);
        // this should be enforced by how Benchto creates combinations from variables when loading benchmarks
        checkState(
                benchmarks.stream().allMatch(benchmark -> benchmark.getBeforeBenchmarkMacros().equals(firstBenchmark.getBeforeBenchmarkMacros()) &&
                        benchmark.getAfterBenchmarkMacros().equals(firstBenchmark.getAfterBenchmarkMacros())),
                "All benchmarks in a group must have the same before and after benchmark macros.");
        checkState(
                benchmarks.stream().allMatch(benchmark -> benchmark.getRuns() == firstBenchmark.getRuns() &&
                        benchmark.getSuitePrewarmRuns() == firstBenchmark.getSuitePrewarmRuns()),
                "All benchmarks in a group must have the same number of runs and suite-prewarm-runs.");
        checkState(
                benchmarks.stream().allMatch(benchmark -> benchmark.getConcurrency() == firstBenchmark.getConcurrency() &&
                        benchmark.isThroughputTest() == firstBenchmark.isThroughputTest()),
                "All benchmarks in a group must have the same concurrency and either test throughput or not.");

        try {
            macroService.runBenchmarkMacros(firstBenchmark.getBeforeBenchmarkMacros(), firstBenchmark);
        }
        catch (Exception e) {
            return List.of(failedBenchmarkResult(firstBenchmark, e));
        }
        List<BenchmarkExecutionResult> benchmarkExecutionResults;
        if (properties.isWarmup()) {
            benchmarkExecutionResults = warmupBenchmarks(benchmarks, executionTimeLimit);
        }
        else {
            benchmarkExecutionResults = executeBenchmarks(benchmarks, executionTimeLimit);
        }

        try {
            macroService.runBenchmarkMacros(firstBenchmark.getAfterBenchmarkMacros(), firstBenchmark);
        }
        catch (Exception e) {
            if (benchmarkExecutionResults.stream().allMatch(BenchmarkExecutionResult::isSuccessful)) {
                return List.of(failedBenchmarkResult(firstBenchmark, e));
            }
            LOG.error("Error while running after benchmark macros for successful benchmark({})",
                    firstBenchmark.getAfterBenchmarkMacros(), e);
        }
        return benchmarkExecutionResults;
    }

    private List<BenchmarkExecutionResult> warmupBenchmarks(List<Benchmark> benchmarks, Optional<ZonedDateTime> executionTimeLimit)
    {
        Benchmark firstBenchmark = benchmarks.get(0);
        Map<Benchmark, BenchmarkExecutionResultBuilder> results = benchmarks.stream().collect(toMap(
                Function.identity(),
                benchmark -> new BenchmarkExecutionResultBuilder(benchmark).withExecutions(List.of())));
        List<QueryExecutionResult> executions;
        try {
            executions = executeQueries(benchmarks, firstBenchmark.getSuitePrewarmRuns(), true, executionTimeLimit);
        }
        catch (Exception e) {
            return results.values().stream()
                    .map(builder -> builder.withUnexpectedException(e).build())
                    .collect(toList());
        }
        Map<Benchmark, String> comparisonFailures = getComparisonFailures(executions);
        return results.entrySet().stream()
                .map(entry -> {
                    Benchmark benchmark = entry.getKey();
                    BenchmarkExecutionResultBuilder builder = entry.getValue();
                    String failure = comparisonFailures.getOrDefault(benchmark, "");
                    if (!failure.isEmpty()) {
                        builder.withUnexpectedException(new RuntimeException(format("Query result comparison failed for queries: %s", failure)));
                    }
                    return builder.build();
                })
                .collect(toList());
    }

    private List<BenchmarkExecutionResult> executeBenchmarks(List<Benchmark> benchmarks, Optional<ZonedDateTime> executionTimeLimit)
    {
        Benchmark firstBenchmark = benchmarks.get(0);
        Map<Benchmark, BenchmarkExecutionResultBuilder> results = benchmarks.stream().collect(toMap(
                Function.identity(),
                benchmark -> new BenchmarkExecutionResultBuilder(benchmark).withExecutions(List.of())));
        List<QueryExecutionResult> executions;
        try {
            executions = executeQueries(benchmarks, firstBenchmark.getSuitePrewarmRuns(), true, executionTimeLimit);
        }
        catch (Exception e) {
            return results.values().stream()
                    .map(builder -> builder.withUnexpectedException(e).build())
                    .collect(toList());
        }
        Map<Benchmark, String> comparisonFailures = getComparisonFailures(executions);
        List<Benchmark> validBenchmarks = new ArrayList<>(benchmarks);
        for (Map.Entry<Benchmark, BenchmarkExecutionResultBuilder> entry : results.entrySet()) {
            Benchmark benchmark = entry.getKey();
            BenchmarkExecutionResultBuilder result = entry.getValue();
            executionSynchronizer.awaitAfterBenchmarkExecutionAndBeforeResultReport(benchmark);
            statusReporter.reportBenchmarkStarted(benchmark);
            result.startTimer();
            String failure = comparisonFailures.getOrDefault(benchmark, "");
            if (!failure.isEmpty()) {
                result.withUnexpectedException(new RuntimeException(format("Query result comparison failed for queries: %s", failure)));
                result.endTimer();
                validBenchmarks.remove(benchmark);
            }
        }

        try {
            executions = executeQueries(validBenchmarks, firstBenchmark.getRuns(), false, executionTimeLimit);
        }
        catch (Exception e) {
            return results.values().stream()
                    .map(builder -> builder.withUnexpectedException(e).build())
                    .collect(toList());
        }
        Map<Benchmark, List<QueryExecutionResult>> groups = executions.stream().collect(groupingBy(QueryExecutionResult::getBenchmark, LinkedHashMap::new, toList()));
        groups.forEach((key, value) -> results.get(key).withExecutions(value).endTimer());

        return results.values().stream()
                .map(builder -> {
                    BenchmarkExecutionResult result = builder.build();
                    statusReporter.reportBenchmarkFinished(result);
                    return result;
                })
                .collect(toImmutableList());
    }

    private static Map<Benchmark, String> getComparisonFailures(List<QueryExecutionResult> executions)
    {
        Map<Benchmark, List<QueryExecutionResult>> groups = executions.stream().collect(groupingBy(QueryExecutionResult::getBenchmark, LinkedHashMap::new, toList()));
        return groups.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(execution -> execution.getFailureCause() != null && execution.getFailureCause().getClass().equals(ResultComparisonException.class)))
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .filter(execution -> execution.getFailureCause() != null && execution.getFailureCause().getClass().equals(ResultComparisonException.class))
                                .map(execution -> format("%s [%s]", execution.getQueryName(), execution.getFailureCause()))
                                .distinct()
                                .collect(Collectors.joining("\n"))));
    }

    private BenchmarkExecutionResult failedBenchmarkResult(Benchmark benchmark, Exception e)
    {
        return new BenchmarkExecutionResultBuilder(benchmark)
                .withUnexpectedException(e)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<QueryExecutionResult> executeQueries(List<Benchmark> benchmarks, int runs, boolean warmup, Optional<ZonedDateTime> executionTimeLimit)
    {
        if (benchmarks.size() == 0) {
            return List.of();
        }
        Benchmark firstBenchmark = benchmarks.get(0);
        ListeningExecutorService executorService = executorServiceFactory.create(firstBenchmark.getConcurrency());
        try {
            if (firstBenchmark.isThroughputTest()) {
                List<Callable<List<QueryExecutionResult>>> queryExecutionCallables = benchmarks.stream()
                        .flatMap(benchmark -> buildConcurrencyQueryExecutionCallables(benchmark, runs, warmup, executionTimeLimit).stream())
                        .collect(toImmutableList());
                List<ListenableFuture<List<QueryExecutionResult>>> executionFutures = (List) executorService.invokeAll(queryExecutionCallables);
                return Futures.allAsList(executionFutures).get().stream()
                        .flatMap(List::stream)
                        .collect(toImmutableList());
            }
            else {
                int numberOfBenchmarkRuns = properties.getQueryRepetitionScope() == BenchmarkProperties.QueryRepetitionScope.SUITE ? runs : 1;
                int numberOfQueryRuns = properties.getQueryRepetitionScope() == BenchmarkProperties.QueryRepetitionScope.BENCHMARK ? runs : 1;
                List<Callable<QueryExecutionResult>> queryExecutionCallables = IntStream.rangeClosed(1, numberOfBenchmarkRuns)
                        .boxed()
                        .flatMap(run -> benchmarks.stream()
                                .flatMap(benchmark -> buildQueryExecutionCallables(benchmark, run, warmup, numberOfQueryRuns).stream()))
                        .collect(toList());
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

    private List<Callable<QueryExecutionResult>> buildQueryExecutionCallables(Benchmark benchmark, int benchmarkRun, boolean suiteWarmup, int queryRuns)
    {
        List<Callable<QueryExecutionResult>> executionCallables = newArrayList();
        for (Query query : benchmark.getQueries()) {
            // warmup locally, but skip local warmup during global warmup
            if (!suiteWarmup) {
                for (int queryRun = 1; queryRun <= benchmark.getBenchmarkPrewarmRuns(); queryRun++) {
                    executionCallables.add(buildQueryExecutionCallable(benchmark, query, true, queryRun));
                }
            }
            // real benchmark
            for (int queryRun = 1; queryRun <= queryRuns; queryRun++) {
                int run = properties.getQueryRepetitionScope() == BenchmarkProperties.QueryRepetitionScope.BENCHMARK ? queryRun : benchmarkRun;
                executionCallables.add(buildQueryExecutionCallable(benchmark, query, suiteWarmup, run));
            }
        }
        return executionCallables;
    }

    private Callable<QueryExecutionResult> buildQueryExecutionCallable(Benchmark benchmark, Query query, boolean warmup, int run)
    {
        QueryExecution queryExecution = new QueryExecution(benchmark, query, run, sqlStatementGenerator);
        Optional<Path> resultFile = benchmark.getQueryResults()
                // only check result of the first warmup run or all runs of non-select statements
                .filter(dir -> (warmup && run == 1) || (!isSelectQuery(query.getSqlTemplate())))
                .map(queryResult -> properties.getQueryResultsDir().resolve(queryResult));
        return () -> {
            try (Connection connection = getConnectionFor(queryExecution)) {
                return executeSingleQuery(queryExecution, benchmark, connection, warmup, Optional.empty(), resultFile);
            }
        };
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
        LOG.info("Execute query, query=%s, skipReport=%s".formatted(benchmark.getQueries().get(0).getName(), skipReport));
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
