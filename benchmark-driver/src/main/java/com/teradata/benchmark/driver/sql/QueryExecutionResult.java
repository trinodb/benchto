/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.sql;

import com.google.common.base.MoreObjects;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.utils.TimeUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.Optional.empty;

public class QueryExecutionResult
{
    private final QueryExecution queryExecution;
    private int rowsCount;
    private Exception failureCause;
    private long start, end;
    private ZonedDateTime utcStart, utcEnd;

    // presto specific
    private Optional<String> prestoQueryId = empty();

    public QueryExecutionResult(QueryExecution queryExecution)
    {
        this.queryExecution = queryExecution;
    }

    public QueryExecution getQueryExecution()
    {
        return queryExecution;
    }

    public Query getQuery()
    {
        return queryExecution.getQuery();
    }

    public Benchmark getBenchmark()
    {
        return queryExecution.getBenchmark();
    }

    public boolean isSuccessful()
    {
        return failureCause == null;
    }

    public int getRowsCount()
    {
        return rowsCount;
    }

    public Duration getQueryDuration()
    {
        return Duration.of(end - start, NANOS);
    }

    public Exception getFailureCause()
    {
        return failureCause;
    }

    public Optional<String> getPrestoQueryId()
    {
        return prestoQueryId;
    }

    public ZonedDateTime getStart()
    {
        return utcStart;
    }

    public ZonedDateTime getEnd()
    {
        return utcEnd;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("queryExecution", queryExecution)
                .add("successful", isSuccessful())
                .add("rowsCount", rowsCount)
                .add("failureCause", failureCause)
                .add("queryDuration", getQueryDuration().toMillis() + " ms")
                .add("prestoQueryId", prestoQueryId)
                .toString();
    }

    public static class QueryExecutionResultBuilder
    {

        private QueryExecutionResult queryExecutionResult;

        public QueryExecutionResultBuilder(QueryExecution queryExecution)
        {
            this.queryExecutionResult = new QueryExecutionResult(queryExecution);
        }

        public QueryExecutionResultBuilder startTimer()
        {
            queryExecutionResult.start = System.nanoTime();
            queryExecutionResult.utcStart = TimeUtils.nowUtc();
            return this;
        }

        public QueryExecutionResultBuilder endTimer()
        {
            checkState(queryExecutionResult.start > 0);
            queryExecutionResult.end = System.nanoTime();
            queryExecutionResult.utcEnd = TimeUtils.nowUtc();
            return this;
        }

        public QueryExecutionResultBuilder failed(Exception cause)
        {
            queryExecutionResult.failureCause = cause;
            return this;
        }

        public QueryExecutionResultBuilder setRowsCount(int rowsCount)
        {
            queryExecutionResult.rowsCount = rowsCount;
            return this;
        }

        public QueryExecutionResultBuilder setPrestoQueryId(String prestoQueryId)
        {
            queryExecutionResult.prestoQueryId = Optional.of(prestoQueryId);
            return this;
        }

        public QueryExecutionResult build()
        {
            return queryExecutionResult;
        }
    }
}
