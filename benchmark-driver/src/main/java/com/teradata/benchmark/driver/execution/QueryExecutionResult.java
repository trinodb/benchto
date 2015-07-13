/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.execution;

import com.google.common.base.MoreObjects;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.Measurable;

import java.util.Optional;

import static java.util.Optional.empty;

public class QueryExecutionResult
        extends Measurable
{
    private final QueryExecution queryExecution;
    private int rowsCount;
    private Exception failureCause;

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

    @Override
    public Benchmark getBenchmark()
    {
        return queryExecution.getBenchmark();
    }

    @Override
    public boolean isSuccessful()
    {
        return failureCause == null;
    }

    public int getRowsCount()
    {
        return rowsCount;
    }

    public Exception getFailureCause()
    {
        return failureCause;
    }

    public Optional<String> getPrestoQueryId()
    {
        return prestoQueryId;
    }

    public String getQueryName()
    {
        return queryExecution.getQueryName();
    }

    public String getSql()
    {
        return queryExecution.getSql();
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
            extends MeasuredBuilder<QueryExecutionResult, QueryExecutionResultBuilder>
    {

        public QueryExecutionResultBuilder(QueryExecution queryExecution)
        {
            super(new QueryExecutionResult(queryExecution));
        }

        public QueryExecutionResultBuilder failed(Exception cause)
        {
            object.failureCause = cause;
            return this;
        }

        public QueryExecutionResultBuilder setRowsCount(int rowsCount)
        {
            object.rowsCount = rowsCount;
            return this;
        }

        public QueryExecutionResultBuilder setPrestoQueryId(String prestoQueryId)
        {
            object.prestoQueryId = Optional.of(prestoQueryId);
            return this;
        }
    }
}
