/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Benchmark
{
    private final String name;
    private final String sequenceId;
    private final String dataSource;
    private final String environment;
    private final List<Query> queries;
    private final int runs;
    private final int prewarmRuns;
    private final int concurrency;
    private final List<String> beforeBenchmarkMacros;
    private final List<String> afterBenchmarkMacros;
    private final Map<String, String> variables;

    public Benchmark(String name, String sequenceId, String dataSource, String environment, List<Query> queries, int runs, int prewarmRuns, int concurrency,
            List<String> beforeBenchmarkMacros, List<String> afterBenchmarkMacros, Map<String, String> variables)
    {
        this.name = name;
        this.sequenceId = sequenceId;
        this.dataSource = dataSource;
        this.environment = environment;
        this.queries = ImmutableList.copyOf(checkNotNull(queries));
        this.runs = runs;
        this.prewarmRuns = prewarmRuns;
        this.concurrency = concurrency;
        this.beforeBenchmarkMacros = beforeBenchmarkMacros;
        this.afterBenchmarkMacros = afterBenchmarkMacros;
        this.variables = variables;
    }

    public String getName()
    {
        return name;
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

    public List<String> getBeforeBenchmarkMacros()
    {
        return beforeBenchmarkMacros;
    }

    public List<String> getAfterBenchmarkMacros()
    {
        return afterBenchmarkMacros;
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("sequenceId", sequenceId)
                .add("environment", environment)
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

        Benchmark that = (Benchmark) o;

        return Objects.equal(this.name, that.name) &&
                Objects.equal(this.sequenceId, that.sequenceId) &&
                Objects.equal(this.environment, that.environment);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, sequenceId, dataSource, environment, queries, runs,
                prewarmRuns, concurrency, beforeBenchmarkMacros, variables);
    }
}
