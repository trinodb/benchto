/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.listeners.BenchmarkStatusReporter;
import com.teradata.benchmark.driver.sql.QueryExecution;
import com.teradata.benchmark.driver.sql.SqlStatementExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

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

    /**
     * @return true if all benchmark queries passed
     */
    public boolean run()
    {
        LOG.info("Running benchmark with properties: {}", properties);

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks();
        LOG.info("Loaded {} benchmarks", benchmarks.size());

        List<BenchmarkResult> benchmarkResults = executeSuite(benchmarks);
        statusReporter.reportBenchmarkFinished(benchmarkResults);

        return !benchmarkResults.stream()
                .filter(result -> !result.isSuccessful())
                .findAny().isPresent();
    }

    private List<BenchmarkResult> executeSuite(List<Benchmark> benchmarks)
    {
        List<BenchmarkResult> benchmarkResults = newArrayList();
        for (Benchmark benchmark : benchmarks) {
            BenchmarkResult benchmarkResult = new BenchmarkResult(benchmark.getQueries().get(0));
            statusReporter.reportBenchmarkStarted(benchmark.getQueries().get(0));

            for (int run = 0; run < properties.getRuns(); run++) {
                executeQuery(benchmark.getQueries().get(0), run, benchmarkResult);
            }

            statusReporter.reportBenchmarkFinished(benchmarkResult);
            benchmarkResults.add(benchmarkResult);
        }
        return Collections.unmodifiableList(benchmarkResults);
    }

    private void executeQuery(Query benchmarkQuery, int run, BenchmarkResult benchmarkResult)
    {
        statusReporter.reportExecutionStarted(benchmarkQuery, run);

        QueryExecution execution = sqlStatementExecutor.executeQuery(benchmarkQuery);

        statusReporter.reportExecutionFinished(benchmarkQuery, run, execution);
        benchmarkResult.addExecution(execution);
    }
}
