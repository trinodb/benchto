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
package com.teradata.benchto.driver.execution;

import com.google.common.base.MoreObjects;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.Measurable;

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
    public String getEnvironment()
    {
        return getBenchmark().getEnvironment();
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
