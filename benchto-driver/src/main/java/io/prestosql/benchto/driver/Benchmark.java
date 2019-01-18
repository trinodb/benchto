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
package io.prestosql.benchto.driver;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static io.prestosql.benchto.driver.loader.BenchmarkDescriptor.RESERVED_KEYWORDS;

public class Benchmark
{
    private String name;
    private String sequenceId;
    private String dataSource;
    private String environment;
    private List<Query> queries;
    private int runs;
    private int prewarmRuns;
    private int concurrency;
    private List<String> beforeBenchmarkMacros;
    private List<String> afterBenchmarkMacros;
    private List<String> beforeExecutionMacros;
    private List<String> afterExecutionMacros;
    private Map<String, String> variables;
    private String uniqueName;
    private Optional<Duration> frequency;

    private Benchmark()
    {
    }

    public String getName()
    {
        return name;
    }

    public String getUniqueName()
    {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName)
    {
        this.uniqueName = uniqueName;
    }

    public String getSequenceId()
    {
        return sequenceId;
    }

    public String getDataSource()
    {
        return dataSource;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public List<Query> getQueries()
    {
        return queries;
    }

    public int getRuns()
    {
        return runs;
    }

    public int getPrewarmRuns()
    {
        return prewarmRuns;
    }

    public int getConcurrency()
    {
        return concurrency;
    }

    public boolean isConcurrent()
    {
        return concurrency > 1;
    }

    public boolean isSerial()
    {
        return concurrency == 1;
    }

    public List<String> getBeforeBenchmarkMacros()
    {
        return beforeBenchmarkMacros;
    }

    public List<String> getAfterBenchmarkMacros()
    {
        return afterBenchmarkMacros;
    }

    public List<String> getBeforeExecutionMacros()
    {
        return beforeExecutionMacros;
    }

    public List<String> getAfterExecutionMacros()
    {
        return afterExecutionMacros;
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }

    public Map<String, String> getNonReservedKeywordVariables()
    {
        Map<String, String> nonReservedKeysVariables = newHashMap(getVariables());
        RESERVED_KEYWORDS.stream()
                .forEach(nonReservedKeysVariables::remove);
        return nonReservedKeysVariables;
    }

    public Optional<Duration> getFrequency()
    {
        return frequency;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("name", name)
                .add("uniqueName", uniqueName)
                .add("sequenceId", sequenceId)
                .add("dataSource", dataSource)
                .add("environment", environment)
                .add("queries", queries)
                .add("runs", runs)
                .add("prewarmRuns", prewarmRuns)
                .add("concurrency", concurrency)
                .add("frequency", frequency)
                .add("beforeBenchmarkMacros", beforeBenchmarkMacros)
                .add("afterBenchmarkMacros", afterBenchmarkMacros)
                .add("beforeExecutionMacros", beforeExecutionMacros)
                .add("afterExecutionMacros", afterExecutionMacros)
                .add("variables", variables)
                .toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Benchmark benchmark = (Benchmark) o;
        return Objects.equal(runs, benchmark.runs) &&
                Objects.equal(prewarmRuns, benchmark.prewarmRuns) &&
                Objects.equal(concurrency, benchmark.concurrency) &&
                Objects.equal(name, benchmark.name) &&
                Objects.equal(sequenceId, benchmark.sequenceId) &&
                Objects.equal(dataSource, benchmark.dataSource) &&
                Objects.equal(environment, benchmark.environment) &&
                Objects.equal(queries, benchmark.queries) &&
                Objects.equal(beforeBenchmarkMacros, benchmark.beforeBenchmarkMacros) &&
                Objects.equal(afterBenchmarkMacros, benchmark.afterBenchmarkMacros) &&
                Objects.equal(beforeExecutionMacros, benchmark.beforeExecutionMacros) &&
                Objects.equal(afterExecutionMacros, benchmark.afterExecutionMacros) &&
                Objects.equal(variables, benchmark.variables) &&
                Objects.equal(frequency, benchmark.frequency);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name,
                sequenceId,
                dataSource,
                environment,
                queries,
                runs,
                prewarmRuns,
                concurrency,
                beforeBenchmarkMacros,
                afterBenchmarkMacros,
                beforeExecutionMacros,
                afterExecutionMacros,
                variables,
                frequency);
    }

    public static class BenchmarkBuilder
    {
        private final Benchmark benchmark = new Benchmark();

        public BenchmarkBuilder(String name, String sequenceId, List<Query> queries)
        {
            this.benchmark.name = name;
            this.benchmark.sequenceId = sequenceId;
            this.benchmark.queries = ImmutableList.copyOf(queries);
        }

        public BenchmarkBuilder withDataSource(String dataSource)
        {
            this.benchmark.dataSource = dataSource;
            return this;
        }

        public BenchmarkBuilder withEnvironment(String environment)
        {
            this.benchmark.environment = environment;
            return this;
        }

        public BenchmarkBuilder withRuns(int runs)
        {
            checkArgument(runs >= 1, "Runs must be greater of equal 1");
            this.benchmark.runs = runs;
            return this;
        }

        public BenchmarkBuilder withPrewarmRuns(int prewarmRuns)
        {
            this.benchmark.prewarmRuns = prewarmRuns;
            return this;
        }

        public BenchmarkBuilder withConcurrency(int concurrency)
        {
            checkArgument(concurrency >= 1, "Concurrency must be greater of equal 1");
            this.benchmark.concurrency = concurrency;
            return this;
        }

        public BenchmarkBuilder withBeforeBenchmarkMacros(List<String> beforeBenchmarkMacros)
        {
            this.benchmark.beforeBenchmarkMacros = ImmutableList.copyOf(beforeBenchmarkMacros);
            return this;
        }

        public BenchmarkBuilder withAfterBenchmarkMacros(List<String> afterBenchmarkMacros)
        {
            this.benchmark.afterBenchmarkMacros = ImmutableList.copyOf(afterBenchmarkMacros);
            return this;
        }

        public BenchmarkBuilder withBeforeExecutionMacros(List<String> beforeExecutionMacros)
        {
            this.benchmark.beforeExecutionMacros = ImmutableList.copyOf(beforeExecutionMacros);
            return this;
        }

        public BenchmarkBuilder withAfterExecutionMacros(List<String> afterExecutionMacros)
        {
            this.benchmark.afterExecutionMacros = ImmutableList.copyOf(afterExecutionMacros);
            return this;
        }

        public BenchmarkBuilder withVariables(Map<String, String> variables)
        {
            this.benchmark.variables = ImmutableMap.copyOf(variables);
            return this;
        }

        public BenchmarkBuilder withFrequency(Optional<Duration> frequency)
        {
            this.benchmark.frequency = frequency;
            return this;
        }

        public Benchmark build()
        {
            return benchmark;
        }
    }
}
