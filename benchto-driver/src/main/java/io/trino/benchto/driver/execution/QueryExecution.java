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

import com.google.common.collect.ImmutableMap;
import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.Query;
import io.trino.benchto.driver.loader.SqlStatementGenerator;

import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class QueryExecution
{
    private final Benchmark benchmark;

    private final Query query;
    private final int sequenceId;

    private final String statement;

    public QueryExecution(Benchmark benchmark, Query query, int sequenceId, SqlStatementGenerator sqlStatementGenerator)
    {
        this.benchmark = requireNonNull(benchmark);
        this.query = requireNonNull(query);
        this.sequenceId = sequenceId;

        this.statement = generateQuerySqlStatement(sqlStatementGenerator);
    }

    public Benchmark getBenchmark()
    {
        return benchmark;
    }

    public String getQueryName()
    {
        return query.getName();
    }

    public Query getQuery()
    {
        return query;
    }

    public int getSequenceId()
    {
        return sequenceId;
    }

    public String getStatement()
    {
        return statement;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("query", query)
                .add("run", sequenceId)
                .toString();
    }

    private String generateQuerySqlStatement(SqlStatementGenerator sqlStatementGenerator)
    {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put("execution_sequence_id", Integer.toString(getSequenceId()))
                .putAll(getBenchmark().getNonReservedKeywordVariables())
                .build();
        List<String> sqlQueries = sqlStatementGenerator.generateQuerySqlStatement(getQuery(), variables);
        checkState(sqlQueries.size() == 1, "Multiple statements in one query file are not supported");
        return sqlQueries.get(0);
    }
}
