/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.reporters.BenchmarkResultReporter;
import com.teradata.benchmark.driver.sql.QueryExecution;
import com.teradata.benchmark.driver.sql.SqlStatementExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private List<BenchmarkResultReporter> benchmarkResultReporters;

    /**
     * @return true if all benchmark queries passed
     */
    public boolean run()
    {
        LOG.info("Running benchmark with properties: {}", properties);

        List<BenchmarkQuery> sqlStatements = benchmarkQueryLoader.loadBenchmarkQueries();
        LOG.info("Loaded {} sql statements", sqlStatements.size());

        BenchmarkResult benchmarkResult = executeBenchmarks(sqlStatements);
        reportBenchmarkResult(benchmarkResult);

        return !benchmarkResult.containsFailedQueries();
    }

    private BenchmarkResult executeBenchmarks(List<BenchmarkQuery> sqlStatements)
    {
        List<BenchmarkQueryResult> benchmarkResults = newArrayList();
        for (BenchmarkQuery benchmarkQuery : sqlStatements) {
            BenchmarkQueryResult benchmarkQueryResult = new BenchmarkQueryResult(benchmarkQuery);
            LOG.debug("Benchmarking query: {}", benchmarkQuery.getName());

            for (int i = 0; i < properties.getRuns(); i++) {
                QueryExecution execution = sqlStatementExecutor.executeQuery(benchmarkQuery);

                reportQueryExecution(benchmarkQuery, execution);
                benchmarkQueryResult.addExecution(execution);
            }

            reportQueryResult(benchmarkQueryResult);
            benchmarkResults.add(benchmarkQueryResult);
        }
        return new BenchmarkResult(benchmarkResults);
    }

    private void reportQueryExecution(BenchmarkQuery benchmarkQuery, QueryExecution execution)
    {
        for (BenchmarkResultReporter reporter : benchmarkResultReporters) {
            reporter.reportQueryExecution(benchmarkQuery, execution);
        }
    }

    private void reportQueryResult(BenchmarkQueryResult result)
    {
        for (BenchmarkResultReporter reporter : benchmarkResultReporters) {
            reporter.reportQueryResult(result);
        }
    }

    private void reportBenchmarkResult(BenchmarkResult benchmarkResult)
    {
        for (BenchmarkResultReporter reporter : benchmarkResultReporters) {
            reporter.reportBenchmarkResult(benchmarkResult);
        }
    }
}
