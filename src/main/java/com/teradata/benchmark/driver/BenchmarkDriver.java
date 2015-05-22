/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.reporters.BenchmarkExecutionListener;
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
    private BenchmarkQueryLoader benchmarkQueryLoader;

    @Autowired
    private SqlStatementExecutor sqlStatementExecutor;

    @Autowired
    private List<BenchmarkExecutionListener> benchmarkExecutionListeners;

    /**
     * @return true if all benchmark queries passed
     */
    public boolean run()
    {
        LOG.info("Running benchmark with properties: {}", properties);

        List<BenchmarkQuery> benchmarkQueries = benchmarkQueryLoader.loadBenchmarkQueries();
        LOG.info("Loaded {} benchmark queries", benchmarkQueries.size());

        List<BenchmarkQueryResult> benchmarkResults = executeSuite(benchmarkQueries);
        suiteFinished(benchmarkResults);

        return !benchmarkResults.stream()
                .filter(result -> !result.isSuccessful())
                .findAny().isPresent();
    }

    private List<BenchmarkQueryResult> executeSuite(List<BenchmarkQuery> benchmarkQueries)
    {
        List<BenchmarkQueryResult> benchmarkResults = newArrayList();
        for (BenchmarkQuery benchmarkQuery : benchmarkQueries) {
            BenchmarkQueryResult benchmarkQueryResult = new BenchmarkQueryResult(benchmarkQuery);
            reportBenchmarkStarted(benchmarkQuery);

            for (int run = 0; run < properties.getRuns(); run++) {
                executeQuery(benchmarkQuery, run, benchmarkQueryResult);
            }

            suiteFinished(benchmarkQueryResult);
            benchmarkResults.add(benchmarkQueryResult);
        }
        return Collections.unmodifiableList(benchmarkResults);
    }

    private void executeQuery(BenchmarkQuery benchmarkQuery, int run, BenchmarkQueryResult benchmarkQueryResult)
    {
        reportExecutionStarted(benchmarkQuery, run);

        QueryExecution execution = sqlStatementExecutor.executeQuery(benchmarkQuery);

        reportExecutionFinished(benchmarkQuery, run, execution);
        benchmarkQueryResult.addExecution(execution);
    }

    private void reportBenchmarkStarted(BenchmarkQuery benchmarkQuery)
    {
        for (BenchmarkExecutionListener reporter : benchmarkExecutionListeners) {
            reporter.benchmarkStarted(benchmarkQuery);
        }
    }

    private void suiteFinished(BenchmarkQueryResult result)
    {
        for (BenchmarkExecutionListener reporter : benchmarkExecutionListeners) {
            reporter.benchmarkFinished(result);
        }
    }

    private void reportExecutionStarted(BenchmarkQuery benchmarkQuery, int run)
    {
        for (BenchmarkExecutionListener reporter : benchmarkExecutionListeners) {
            reporter.executionStarted(benchmarkQuery, run);
        }
    }

    private void reportExecutionFinished(BenchmarkQuery benchmarkQuery, int run, QueryExecution execution)
    {
        for (BenchmarkExecutionListener reporter : benchmarkExecutionListeners) {
            reporter.executionFinished(benchmarkQuery, run, execution);
        }
    }

    private void suiteFinished(List<BenchmarkQueryResult> benchmarkResults)
    {
        for (BenchmarkExecutionListener reporter : benchmarkExecutionListeners) {
            reporter.suiteFinished(benchmarkResults);
        }
    }
}
