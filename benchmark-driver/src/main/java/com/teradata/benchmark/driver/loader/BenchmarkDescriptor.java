/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.teradata.benchmark.driver.utils.YamlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.teradata.benchmark.driver.utils.CartesianProductUtils.cartesianProduct;
import static com.teradata.benchmark.driver.utils.YamlUtils.asStringList;
import static com.teradata.benchmark.driver.utils.YamlUtils.loadYamlFromString;
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
    private static final String PREWARM_RUNS_KEY = "prewarm-runs";
    private static final String CONCURRENCY_KEY = "concurrency";
    private static final String BEFORE_BENCHMARK_MACROS_KEY = "before-benchmark";
    private static final String VARIABLES_KEY = "variables";

    public static BenchmarkDescriptor loadFromFile(Path file)
            throws IOException
    {
        return loadFromString(file, new String(readAllBytes(file), UTF_8));
    }

    public static BenchmarkDescriptor loadFromString(Path file, String content)
    {
        Map<String, Object> yaml = (Map) loadYamlFromString(content, BenchmarkDescriptor.class);

        Optional<Integer> concurrency = Optional.ofNullable((Integer) yaml.get(CONCURRENCY_KEY));
        Optional<Integer> runs = Optional.ofNullable((Integer) yaml.getOrDefault(RUNS_KEY, concurrency.isPresent() ? concurrency.get() : null));
        Optional<Integer> prewarmRuns = Optional.ofNullable((Integer) yaml.get(PREWARM_RUNS_KEY));

        Optional<List<String>> beforeBenchmarkMacros;
        if (yaml.containsKey(BEFORE_BENCHMARK_MACROS_KEY)) {
            beforeBenchmarkMacros = Optional.of(asStringList(yaml.get(BEFORE_BENCHMARK_MACROS_KEY)));
        }
        else {
            beforeBenchmarkMacros = Optional.empty();
        }

        checkArgument(yaml.containsKey(DATA_SOURCE_KEY), "Benchmark yaml must contain '%s' key", DATA_SOURCE_KEY);

        return new BenchmarkDescriptor(
                file,
                checkNotNull(yaml.get(DATA_SOURCE_KEY)).toString(),
                asStringList(checkNotNull(yaml.get(QUERY_NAMES_KEY))),
                runs, prewarmRuns, concurrency,
                beforeBenchmarkMacros,
                extractVariableMapList(yaml));
    }

    private static List<Map<String, String>> extractVariableMapList(Map<String, Object> yaml)
    {
        Map<String, Map<String, Object>> variableMaps = (Map) yaml.getOrDefault(VARIABLES_KEY, newHashMap());
        List<Map<String, String>> variableMapList = variableMaps.values()
                .stream()
                .map(YamlUtils::stringifyMultimap)
                .flatMap(variableMap -> cartesianProduct(variableMap).stream())
                .collect(toList());

        if (variableMapList.isEmpty()) {
            variableMapList.add(newHashMap());
        }

        return variableMapList;
    }

    private final Path descriptorPath;
    private final String dataSource;
    private final List<String> queryNames;
    private final Optional<Integer> runs;
    private final Optional<Integer> prewarmRepeats;
    private final Optional<Integer> concurrency;
    private final Optional<List<String>> beforeBenchmarkMacros;
    private final List<Map<String, String>> variableMapList;

    public BenchmarkDescriptor(
            Path descriptorPath, String dataSource, List<String> queryNames,
            Optional<Integer> runs, Optional<Integer> prewarmRepeats, Optional<Integer> concurrency,
            Optional<List<String>> beforeBenchmarkMacros, List<Map<String, String>> variableMapList)
    {
        this.descriptorPath = descriptorPath;
        this.dataSource = dataSource;
        this.queryNames = queryNames;
        this.runs = runs;
        this.prewarmRepeats = prewarmRepeats;
        this.concurrency = concurrency;
        this.beforeBenchmarkMacros = beforeBenchmarkMacros;
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

    public Optional<Integer> getRuns()
    {
        return runs;
    }

    public Optional<Integer> getPrewarmRepeats()
    {
        return prewarmRepeats;
    }

    public Optional<Integer> getConcurrency()
    {
        return concurrency;
    }

    public Optional<List<String>> getBeforeBenchmarkMacros()
    {
        return beforeBenchmarkMacros;
    }

    public List<Map<String, String>> getVariableMapList()
    {
        return variableMapList;
    }
}
