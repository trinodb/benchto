/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.Benchmark.BenchmarkBuilder;
import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.BenchmarkProperties;
import com.teradata.benchto.driver.Query;
import com.teradata.benchto.driver.service.BenchmarkServiceClient;
import com.teradata.benchto.driver.service.BenchmarkServiceClient.GenerateUniqueNamesRequestItem;
import com.teradata.benchto.driver.utils.NaturalOrderComparator;
import com.teradata.benchto.driver.utils.YamlUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.facebook.presto.jdbc.internal.guava.base.Preconditions.checkState;
import static com.facebook.presto.jdbc.internal.guava.collect.Lists.newArrayListWithCapacity;
import static com.facebook.presto.jdbc.internal.guava.collect.Sets.newLinkedHashSet;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static com.teradata.benchto.driver.loader.BenchmarkDescriptor.DATA_SOURCE_KEY;
import static com.teradata.benchto.driver.loader.BenchmarkDescriptor.QUERY_NAMES_KEY;
import static com.teradata.benchto.driver.loader.BenchmarkDescriptor.VARIABLES_KEY;
import static com.teradata.benchto.driver.service.BenchmarkServiceClient.GenerateUniqueNamesRequestItem.generateUniqueNamesRequestItem;
import static com.teradata.benchto.driver.utils.CartesianProductUtils.cartesianProduct;
import static com.teradata.benchto.driver.utils.FilterUtils.benchmarkNameMatchesTo;
import static com.teradata.benchto.driver.utils.YamlUtils.loadYamlFromString;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.readAllBytes;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

@Component
public class BenchmarkLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkLoader.class);

    private static final Pattern VALUE_SUBSTITUTION_PATTERN = Pattern.compile(".*\\$\\{.+\\}.*");

    private static final String BENCHMARK_FILE_SUFFIX = "yaml";

    private static final int DEFAULT_RUNS = 3;
    private static final int DEFAULT_CONCURRENCY = 1;
    private static final int DEFAULT_PREWARM_RUNS = 0;

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private BenchmarkServiceClient benchmarkServiceClient;

    @Autowired
    private QueryLoader queryLoader;

    @Autowired
    private Configuration freemarkerConfiguration;

    public List<Benchmark> loadBenchmarks(String sequenceId)
    {
        try {
            List<Path> benchmarkFiles = findBenchmarkFiles();

            List<Benchmark> allBenchmarks = loadBenchmarks(sequenceId, benchmarkFiles);
            LOGGER.debug("All benchmarks: {}", allBenchmarks);

            List<Benchmark> includedBenchmarks = filterBenchmarks(allBenchmarks);
            Set<Benchmark> excludedBenchmarks = newLinkedHashSet(allBenchmarks);
            excludedBenchmarks.removeAll(includedBenchmarks);

            String formatString = createFormatString(allBenchmarks);
            LOGGER.info("Excluded Benchmarks:");
            printFormattedBenchmarksInfo(formatString, excludedBenchmarks);

            fillUniqueBenchmarkNames(includedBenchmarks);

            LOGGER.info("Selected Benchmarks:");
            printFormattedBenchmarksInfo(formatString, includedBenchmarks);

            return includedBenchmarks;
        }
        catch (IOException e) {
            throw new BenchmarkExecutionException("Could not load benchmarks", e);
        }
    }

    private List<Path> findBenchmarkFiles()
            throws IOException
    {
        LOGGER.info("Searching for benchmarks in classpath ...");

        List<Path> benchmarkFiles = Files
                .walk(properties.benchmarksFilesPath())
                .filter(file -> isRegularFile(file) && file.toString().endsWith(BENCHMARK_FILE_SUFFIX))
                .collect(toList());
        benchmarkFiles.stream().forEach((path) -> LOGGER.info("Benchmark found: {}", path.toString()));

        return benchmarkFiles;
    }

    private List<Benchmark> loadBenchmarks(String sequenceId, List<Path> benchmarkFiles)
    {
        return benchmarkFiles.stream()
                .flatMap(file -> loadBenchmarks(sequenceId, file).stream())
                .sorted((left, right) -> NaturalOrderComparator.forStrings().compare(left.getName(), right.getName()))
                .collect(toList());
    }

    private List<Benchmark> filterBenchmarks(List<Benchmark> benchmarks)
    {
        return benchmarks.stream()
                .filter(activeBenchmarksByName())
                .filter(activeBenchmarksByProperties())
                .collect(toList());
    }

    private List<Benchmark> loadBenchmarks(String sequenceId, Path benchmarkFile)
    {
        try {
            String content = new String(readAllBytes(benchmarkFile), UTF_8);
            Map<String, Object> yaml = loadYamlFromString(content);

            checkArgument(yaml.containsKey(DATA_SOURCE_KEY), "Mandatory variable %s not present in file %s", DATA_SOURCE_KEY, benchmarkFile);
            checkArgument(yaml.containsKey(QUERY_NAMES_KEY), "Mandatory variable %s not present in file %s", QUERY_NAMES_KEY, benchmarkFile);

            List<BenchmarkDescriptor> benchmarkDescriptors = createBenchmarkDescriptors(yaml);

            List<Benchmark> benchmarks = newArrayListWithCapacity(benchmarkDescriptors.size());
            for (BenchmarkDescriptor benchmarkDescriptor : benchmarkDescriptors) {
                String benchmarkName = benchmarkName(benchmarkFile);
                List<Query> queries = queryLoader.loadFromFiles(benchmarkDescriptor.getQueryNames());

                Benchmark benchmark = new BenchmarkBuilder(benchmarkName, sequenceId, queries)
                        .withDataSource(benchmarkDescriptor.getDataSource())
                        .withEnvironment(properties.getEnvironmentName())
                        .withRuns(benchmarkDescriptor.getRuns().orElse(DEFAULT_RUNS))
                        .withPrewarmRuns(benchmarkDescriptor.getPrewarmRepeats().orElse(DEFAULT_PREWARM_RUNS))
                        .withConcurrency(benchmarkDescriptor.getConcurrency().orElse(DEFAULT_CONCURRENCY))
                        .withBeforeBenchmarkMacros(benchmarkDescriptor.getBeforeBenchmarkMacros())
                        .withAfterBenchmarkMacros(benchmarkDescriptor.getAfterBenchmarkMacros())
                        .withBeforeExecutionMacros(benchmarkDescriptor.getBeforeExecutionMacros())
                        .withAfterExecutionMacros(benchmarkDescriptor.getAfterExecutionMacros())
                        .withVariables(benchmarkDescriptor.getVariables())
                        .build();
                benchmarks.add(benchmark);
            }

            return benchmarks;
        }
        catch (IOException e) {
            throw new BenchmarkExecutionException("Could not load benchmark: " + benchmarkFile, e);
        }
    }

    private List<BenchmarkDescriptor> createBenchmarkDescriptors(Map<String, Object> yaml)
    {
        List<Map<String, String>> variablesCombinations = extractVariableMapList(yaml);
        Map<String, String> globalVariables = extractGlobalVariables(yaml);

        for (Map<String, String> variablesMap : variablesCombinations) {
            for (Entry<String, String> globalVariableEntry : globalVariables.entrySet()) {
                variablesMap.putIfAbsent(globalVariableEntry.getKey(), globalVariableEntry.getValue());
            }

            evaluateValueExpressions(variablesMap);
        }

        return variablesCombinations.stream()
                .map(BenchmarkDescriptor::new)
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    private void evaluateValueExpressions(Map<String, String> variablesMap)
    {
        for (Entry<String, String> variableEntry : variablesMap.entrySet()) {
            String variableValue = variableEntry.getValue();

            try {
                if (VALUE_SUBSTITUTION_PATTERN.matcher(variableValue).matches()) {
                    Template valueTemplate = new Template(randomUUID().toString(), variableValue, freemarkerConfiguration);
                    String evaluatedValue = processTemplateIntoString(valueTemplate, variablesMap);

                    if (VALUE_SUBSTITUTION_PATTERN.matcher(evaluatedValue).matches()) {
                        throw new BenchmarkExecutionException("Recursive value substitution is not supported, invalid " + variableEntry.getKey() + ": " + variableValue);
                    }

                    variableEntry.setValue(evaluatedValue);
                }
            }
            catch (IOException | TemplateException e) {
                throw new BenchmarkExecutionException("Could not evaluate value " + variableValue, e);
            }
        }
    }

    private String benchmarkName(Path benchmarkFile)
    {
        String relativePath = properties.benchmarksFilesPath().relativize(benchmarkFile).toString();
        return removeExtension(relativePath);
    }

    private Map<String, String> extractGlobalVariables(Map<String, Object> yaml)
    {
        return yaml.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(VARIABLES_KEY))
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue() == null ? null : entry.getValue().toString()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractVariableMapList(Map<String, Object> yaml)
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

    private Predicate<Benchmark> activeBenchmarksByName()
    {
        Optional<List<String>> activeBenchmarks = properties.getActiveBenchmarks();
        if (activeBenchmarks.isPresent()) {
            return benchmarkNameMatchesTo(activeBenchmarks.get());
        }
        return path -> true;
    }

    private Predicate<Benchmark> activeBenchmarksByProperties()
    {
        return new BenchmarkByActiveVariablesFilter(properties);
    }

    private void fillUniqueBenchmarkNames(List<Benchmark> includedBenchmarks)
    {
        List<GenerateUniqueNamesRequestItem> namesRequestItems = includedBenchmarks.stream()
                .map(benchmark -> generateUniqueNamesRequestItem(benchmark.getName(), benchmark.getNonReservedKeywordVariables()))
                .collect(toList());
        String[] uniqueBenchmarkNames = benchmarkServiceClient.generateUniqueBenchmarkNames(namesRequestItems);

        checkState(uniqueBenchmarkNames.length == includedBenchmarks.size());
        for (int i = 0; i < uniqueBenchmarkNames.length; i++) {
            includedBenchmarks.get(i).setUniqueName(uniqueBenchmarkNames[i]);
        }
    }

    private void printFormattedBenchmarksInfo(String formatString, Collection<Benchmark> benchmarks)
    {
        LOGGER.info(format(formatString, "Benchmark Name", "Data Source", "Runs", "Prewarms", "Concurrency"));
        for (Benchmark benchmark : benchmarks) {
            LOGGER.info(format(formatString,
                    benchmark.getName(),
                    benchmark.getDataSource(),
                    benchmark.getRuns() + "",
                    benchmark.getPrewarmRuns() + "",
                    benchmark.getConcurrency() + ""));
        }
    }

    private String createFormatString(Collection<Benchmark> benchmarks)
    {
        int nameMaxLength = benchmarks.stream().mapToInt((benchmark) -> benchmark.getName().length()).max().getAsInt();
        int dataSourceMaxLength = benchmarks.stream().mapToInt((benchmark) -> benchmark.getDataSource().length()).max().getAsInt();
        int indent = 3;
        return "\t| %-" + (nameMaxLength + indent) + "s | %-" + (dataSourceMaxLength + indent) + "s | %-4s | %-8s | %-11s |";
    }
}
