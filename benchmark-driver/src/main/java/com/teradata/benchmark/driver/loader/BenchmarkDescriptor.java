/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.utils.YamlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static com.teradata.benchmark.driver.utils.CartesianProductUtils.cartesianProduct;
import static com.teradata.benchmark.driver.utils.YamlUtils.asList;
import static com.teradata.benchmark.driver.utils.YamlUtils.loadYamlFromString;
import static com.teradata.benchmark.driver.utils.YamlUtils.stringifyList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.util.stream.Collectors.toList;

/**
 * Describes benchmark parameters.
 */
public class BenchmarkDescriptor
{
    private static final String DATA_SOURCE_KEY = "datasource";
    private static final String QUERY_NAMES_KEY = "query-names";
    private static final String RUNS_KEY = "runs";
    private static final String CONCURRENCY_KEY = "concurrency";
    private static final String BEFORE_BENCHMARK_MACROS_KEY = "before-benchmark";
    private static final String PREWARM_REPEATS_KEY = "prewarm-repeats";
    private static final String VARIABLES_KEY = "variables";

    private static final int DEFAULT_RUNS = 3;
    private static final int DEFAULT_CONCURRENCY = 1;
    private static final List<String> DEFAULT_BEFORE_BENCHMARK_MACROS = ImmutableList.of();
    private static final int DEFAULT_PREWARM_REPEATS = 0;

    public static BenchmarkDescriptor loadFromFile(Path file)
            throws IOException
    {
        return loadFromString(file, new String(readAllBytes(file), UTF_8));
    }

    public static BenchmarkDescriptor loadFromString(Path file, String content)
    {
        Map<String, Object> yaml = (Map) loadYamlFromString(content, BenchmarkDescriptor.class);

        int concurrency = (int) yaml.getOrDefault(CONCURRENCY_KEY, DEFAULT_CONCURRENCY);
        int runs = (int) yaml.getOrDefault(RUNS_KEY, yaml.containsKey(CONCURRENCY_KEY) ? concurrency : DEFAULT_RUNS);
        List<String> beforeBenchmarkMacros = stringifyList(asList(yaml.getOrDefault(BEFORE_BENCHMARK_MACROS_KEY, DEFAULT_BEFORE_BENCHMARK_MACROS)));
        int prewarmRepeats = (int) yaml.getOrDefault(PREWARM_REPEATS_KEY, DEFAULT_PREWARM_REPEATS);

        checkArgument(yaml.containsKey(DATA_SOURCE_KEY), "Benchmark yaml must contain '%s' key", DATA_SOURCE_KEY);

        return new BenchmarkDescriptor(
                file,
                yaml.get(DATA_SOURCE_KEY).toString(),
                stringifyList(asList(yaml.get(QUERY_NAMES_KEY))),
                runs, concurrency,
                beforeBenchmarkMacros, prewarmRepeats,
                extractVariableMaps(yaml));
    }

    private static List<Map<String, String>> extractVariableMaps(Map<String, Object> yaml)
    {
        Map<String, Map<String, Object>> variableMaps = (Map) yaml.getOrDefault(VARIABLES_KEY, newHashMap());
        return variableMaps.values()
                .stream()
                .map(YamlUtils::stringifyMultimap)
                .flatMap(variableMap -> cartesianProduct(variableMap).stream())
                .collect(toList());
    }

    private final Path descriptorPath;
    private final String dataSource;
    private final List<String> queryNames;
    private final int runs;
    private final int concurrency;
    private final List<String> beforeBenchmarkMacros;
    private final int prewarmRepeats;
    private final List<Map<String, String>> variableMapList;

    public BenchmarkDescriptor(Path descriptorPath, String dataSource, List<String> queryNames, int runs, int concurrency,
            List<String> beforeBenchmarkMacros, int prewarmRepeats, List<Map<String, String>> variableMapList)
    {
        this.descriptorPath = descriptorPath;
        this.dataSource = dataSource;
        this.queryNames = queryNames;
        this.runs = runs;
        this.concurrency = concurrency;
        this.beforeBenchmarkMacros = beforeBenchmarkMacros;
        this.prewarmRepeats = prewarmRepeats;
        this.variableMapList = variableMapList;
    }

    public Path getDescriptorPath()
    {
        return descriptorPath;
    }

    public String getDataSource()
    {
        return dataSource;
    }

    public List<String> getQueryNames()
    {
        return queryNames;
    }

    public int getRuns()
    {
        return runs;
    }

    public int getConcurrency()
    {
        return concurrency;
    }

    public List<String> getBeforeBenchmarkMacros()
    {
        return beforeBenchmarkMacros;
    }

    public int getPrewarmRepeats()
    {
        return prewarmRepeats;
    }

    public List<Map<String, String>> getVariableMapList()
    {
        return variableMapList;
    }
}
