/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.sql;

import com.google.common.base.MoreObjects;
import com.teradata.benchmark.driver.utils.TimeUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.Optional.empty;

public class QueryExecution
{
    private int rowsCount;
    private Exception failureCause;
    private long start, end;
    private ZonedDateTime utcStart, utcEnd;

    // presto specific
    private Optional<String> prestoQueryId = empty();

    private QueryExecution()
    {
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
                .add("successful", isSuccessful())
                .add("rowsCount", rowsCount)
                .add("failureCause", failureCause)
                .add("queryDuration", getQueryDuration().toMillis() + " ms")
                .add("prestoQueryId", prestoQueryId)
                .toString();
    }

    public static class QueryExecutionBuilder
    {

        private QueryExecution queryExecution = new QueryExecution();

        public QueryExecutionBuilder startTimer()
        {
            queryExecution.start = System.nanoTime();
            queryExecution.utcStart = TimeUtils.nowUtc();
            return this;
        }

        public QueryExecutionBuilder endTimer()
        {
            checkState(queryExecution.start > 0);
            queryExecution.end = System.nanoTime();
            queryExecution.utcEnd = TimeUtils.nowUtc();
            return this;
        }

        public QueryExecutionBuilder failed(Exception cause)
        {
            queryExecution.failureCause = cause;
            return this;
        }

        public QueryExecutionBuilder setRowsCount(int rowsCount)
        {
            queryExecution.rowsCount = rowsCount;
            return this;
        }

        public QueryExecutionBuilder setPrestoQueryId(String prestoQueryId)
        {
            queryExecution.prestoQueryId = Optional.of(prestoQueryId);
            return this;
        }

        public QueryExecution build()
        {
            return queryExecution;
        }
    }
}
