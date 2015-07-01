/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

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
    private Map<String, String> variables;

    private Benchmark()
    {
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
                .add("dataSource", dataSource)
                .add("environment", environment)
                .add("queries", queries)
                .add("runs", runs)
                .add("prewarmRuns", prewarmRuns)
                .add("concurrency", concurrency)
                .add("beforeBenchmarkMacros", beforeBenchmarkMacros)
                .add("afterBenchmarkMacros", afterBenchmarkMacros)
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

        Benchmark that = (Benchmark) o;

        return Objects.equal(this.name, that.name) &&
                Objects.equal(this.sequenceId, that.sequenceId) &&
                Objects.equal(this.environment, that.environment);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, sequenceId, environment);
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

        public BenchmarkBuilder withVariables(Map<String, String> variables)
        {
            this.benchmark.variables = ImmutableMap.copyOf(variables);
            return this;
        }

        public Benchmark createBenchmark()
        {
            return benchmark;
        }
    }
}
