/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.listeners;

import com.teradata.benchmark.driver.execution.BenchmarkExecution;
import com.teradata.benchmark.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchmark.driver.execution.QueryExecution;
import com.teradata.benchmark.driver.execution.QueryExecutionResult;
import com.teradata.benchmark.driver.listeners.benchmark.BenchmarkExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingPrewarmExecutionListener
        implements BenchmarkExecutionListener
{

    private static final Logger LOG = LoggerFactory.getLogger(LoggingPrewarmExecutionListener.class);

    @Override
    public void benchmarkStarted(BenchmarkExecution benchmarkExecution)
    {
        LOG.info("Executing prewarm: {}", benchmarkExecution.getBenchmarkName());
    }

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult result)
    {
        LOG.info("Finished prewarm: {}", result.getBenchmarkExecution().getBenchmarkName());
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        LOG.info("Prewarm query started: {} ({})", execution.getQueryName(), execution.getRun());
    }

    @Override
    public void executionFinished(QueryExecutionResult result)
    {
        if (result.isSuccessful()) {
            LOG.info("Prewarm query finished: {} ({}), rows count: {}, duration: {}", result.getQueryName(), result.getQueryExecution().getRun(), result.getRowsCount(), result.getQueryDuration());
        }
        else {
            LOG.error("Prewarm query failed: {} ({}), execution error: {}", result.getQueryName(), result.getQueryExecution().getRun(), result.getFailureCause().getMessage());
        }
    }
}
