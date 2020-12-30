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

import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.Query;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class QueryExecution
{
    private final Benchmark benchmark;
    private final Query query;
    private final int run;

    public QueryExecution(Benchmark benchmark, Query query, int run)
    {
        this.benchmark = requireNonNull(benchmark);
        this.query = requireNonNull(query);
        this.run = run;
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

    public int getRun()
    {
        return run;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("query", query)
                .add("run", run)
                .toString();
    }
}
