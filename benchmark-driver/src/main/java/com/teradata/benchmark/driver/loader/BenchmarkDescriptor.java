/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.teradata.benchmark.driver.utils.YamlUtils.asStringList;

/**
 * Wrapper class around benchmark variables map with helper access methods.
 */
public class BenchmarkDescriptor
{
    public static final String DATA_SOURCE_KEY = "datasource";
    public static final String QUERY_NAMES_KEY = "query-names";
    public static final String RUNS_KEY = "runs";
    public static final String PREWARM_RUNS_KEY = "prewarm-runs";
    public static final String CONCURRENCY_KEY = "concurrency";
    public static final String BEFORE_BENCHMARK_MACROS_KEY = "before-benchmark";
    public static final String AFTER_BENCHMARK_MACROS_KEY = "after-benchmark";
    public static final String VARIABLES_KEY = "variables";
    public static final String QUARANTINE_KEY = "quarantine";

    public static final Set<String> RESERVED_KEYWORDS = ImmutableSet.of(
            DATA_SOURCE_KEY,
            QUERY_NAMES_KEY,
            RUNS_KEY,
            PREWARM_RUNS_KEY,
            CONCURRENCY_KEY,
            BEFORE_BENCHMARK_MACROS_KEY,
            AFTER_BENCHMARK_MACROS_KEY,
            VARIABLES_KEY,
            QUARANTINE_KEY
    );

    private final Map<String, String> variables;

    public BenchmarkDescriptor(Map<String, String> variables)
    {
        this.variables = variables;
        this.variables.putIfAbsent(QUARANTINE_KEY, "false");
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }

    public String getDataSource()
    {
        return variables.get(DATA_SOURCE_KEY);
    }

    public List<String> getQueryNames()
    {
        return asStringList(variables.get(QUERY_NAMES_KEY));
    }

    public Optional<Integer> getRuns()
    {
        return getIntegerOptional(RUNS_KEY);
    }

    public Optional<Integer> getPrewarmRepeats()
    {
        return getIntegerOptional(PREWARM_RUNS_KEY);
    }

    public Optional<Integer> getConcurrency()
    {
        return getIntegerOptional(CONCURRENCY_KEY);
    }

    public List<String> getBeforeBenchmarkMacros()
    {
        return asStringList(variables.getOrDefault(BEFORE_BENCHMARK_MACROS_KEY, ""));
    }

    public List<String> getAfterBenchmarkMacros()
    {
        return asStringList(variables.getOrDefault(AFTER_BENCHMARK_MACROS_KEY, ""));
    }

    private Optional<Integer> getIntegerOptional(String key)
    {
        if (variables.containsKey(key)) {
            return Optional.of(Integer.valueOf(variables.get(key)));
        }
        return Optional.empty();
    }
}
