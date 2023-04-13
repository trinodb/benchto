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
package io.trino.benchto.driver.loader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.BenchmarkExecutionException;
import io.trino.benchto.driver.BenchmarkProperties;
import io.trino.benchto.driver.Query;
import io.trino.benchto.driver.service.BenchmarkServiceClient;
import io.trino.benchto.driver.utils.CartesianProductUtils;
import io.trino.benchto.driver.utils.NaturalOrderComparator;
import io.trino.benchto.driver.utils.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static io.trino.benchto.driver.loader.BenchmarkDescriptor.NAME_KEY;
import static java.lang.String.format;
import static java.nio.file.Files.isRegularFile;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

@Component
public class BenchmarkLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkLoader.class);

    private static final Pattern VALUE_SUBSTITUTION_PATTERN = Pattern.compile(".*\\$\\{.+}.*");

    private static final String BENCHMARK_FILE_SUFFIX = "yaml";

    private static final int DEFAULT_RUNS = 3;
    private static final int DEFAULT_CONCURRENCY = 1;
    private static final int DEFAULT_SUITE_PREWARM_RUNS = 0;
    private static final int DEFAULT_BENCHMARK_PREWARM_RUNS = 2;

    @Autowired
    private Environment environment;

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private BenchmarkServiceClient benchmarkServiceClient;

    @Autowired
    private QueryLoader queryLoader;

    @Autowired
    private Configuration freemarkerConfiguration;

    private Map<Object, Object> overrides;

    @PostConstruct
    public void setup()
            throws IOException
    {
        if (properties.getOverridesPath().isPresent()) {
            overrides = YamlUtils.loadYamlFromPath(properties.getOverridesPath().get());
        }
        else {
            overrides = emptyMap();
        }
    }

    public List<Benchmark> loadBenchmarks(String sequenceId)
    {
        try {
            List<Path> benchmarkFiles = findBenchmarkFiles();

            benchmarkFiles = benchmarkFiles.stream()
                    .filter(activeBenchmarks())
                    .collect(toList());

            verifyNoDuplicateBenchmarks(benchmarkFiles);

            benchmarkFiles.forEach(path -> LOGGER.info("Benchmark file to be read: {}", path));

            List<Benchmark> allBenchmarks = loadBenchmarks(sequenceId, benchmarkFiles);
            LOGGER.debug("All benchmarks: {}", allBenchmarks);

            List<Benchmark> includedBenchmarks = allBenchmarks.stream()
                    .filter(new BenchmarkByActiveVariablesFilter(properties))
                    .collect(toList());

            Set<Benchmark> excludedBenchmarks = newLinkedHashSet(allBenchmarks);
            excludedBenchmarks.removeAll(includedBenchmarks);

            String formatString = createFormatString(allBenchmarks);
            LOGGER.info("Excluded Benchmarks:");
            printFormattedBenchmarksInfo(formatString, excludedBenchmarks);

            fillUniqueBenchmarkNames(includedBenchmarks);

            List<Benchmark> freshBenchmarks = ImmutableList.of();
            if (properties.isFrequencyCheckEnabled()) {
                freshBenchmarks = filterFreshBenchmarks(includedBenchmarks);
                LOGGER.info("Recently tested benchmarks:");
                printFormattedBenchmarksInfo(formatString, freshBenchmarks);
            }

            LOGGER.info("Selected Benchmarks:");
            includedBenchmarks.removeAll(freshBenchmarks);
            printFormattedBenchmarksInfo(formatString, includedBenchmarks);

            checkState(allBenchmarks.size() == includedBenchmarks.size() + excludedBenchmarks.size() + freshBenchmarks.size());

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

        verifyNoNestedBenchmarkDirs();

        ImmutableList.Builder<Path> benchmarkFilesBuilder = ImmutableList.builder();
        for (Path benchmarkFilesPath : properties.benchmarksFilesDirs()) {
            Files.walk(benchmarkFilesPath)
                    .filter(file -> isRegularFile(file) && file.toString().endsWith(BENCHMARK_FILE_SUFFIX))
                    .forEach(benchmarkFilesBuilder::add);
        }

        List<Path> benchmarkFiles = benchmarkFilesBuilder.build();
        benchmarkFiles.forEach(path -> LOGGER.info("Benchmark found: {}", path.toString()));
        return benchmarkFiles;
    }

    private void verifyNoNestedBenchmarkDirs()
    {
        for (Path benchmarkFilesPath : properties.benchmarksFilesDirs()) {
            for (Path otherBenchmarkFilesPath : properties.benchmarksFilesDirs()) {
                if (!benchmarkFilesPath.equals(otherBenchmarkFilesPath) && benchmarkFilesPath.startsWith(otherBenchmarkFilesPath)) {
                    throw new BenchmarkExecutionException("Benchmark directories contain nested paths");
                }
            }
        }
    }

    private void verifyNoDuplicateBenchmarks(List<Path> benchmarkFiles)
    {
        Set<String> benchmarkNames = new HashSet<>();
        for (Path benchmarkFile : benchmarkFiles) {
            String benchmarkName = benchmarkName(benchmarkFile);
            if (!benchmarkNames.add(benchmarkName)) {
                throw new BenchmarkExecutionException("Benchmark with name \"" + benchmarkName + "\" in multiple locations");
            }
        }
    }

    private List<Benchmark> loadBenchmarks(String sequenceId, List<Path> benchmarkFiles)
    {
        return benchmarkFiles.stream()
                .flatMap(file -> loadBenchmarks(sequenceId, file).stream())
                .sorted((left, right) -> NaturalOrderComparator.forStrings().compare(left.getName(), right.getName()))
                .collect(toList());
    }

    private List<Benchmark> loadBenchmarks(String sequenceId, Path benchmarkFile)
    {
        try {
            Map<Object, Object> yaml = YamlUtils.loadYamlFromPath(benchmarkFile);
            yaml = mergeTopLevelVariables(yaml);

            checkArgument(yaml.containsKey(BenchmarkDescriptor.DATA_SOURCE_KEY), "Mandatory variable %s not present in file %s", BenchmarkDescriptor.DATA_SOURCE_KEY, benchmarkFile);
            checkArgument(yaml.containsKey(BenchmarkDescriptor.QUERY_NAMES_KEY), "Mandatory variable %s not present in file %s", BenchmarkDescriptor.QUERY_NAMES_KEY, benchmarkFile);

            String defaultName = benchmarkName(benchmarkFile);
            List<BenchmarkDescriptor> benchmarkDescriptors = createBenchmarkDescriptors(defaultName, yaml);

            List<Benchmark> benchmarks = newArrayListWithCapacity(benchmarkDescriptors.size());
            for (BenchmarkDescriptor benchmarkDescriptor : benchmarkDescriptors) {
                String benchmarkName = benchmarkDescriptor.getName();
                List<Query> queries = queryLoader.loadFromFiles(benchmarkDescriptor.getQueryNames());

                Benchmark benchmark = new Benchmark.BenchmarkBuilder(benchmarkName, sequenceId, queries)
                        .withDataSource(benchmarkDescriptor.getDataSource())
                        .withEnvironment(properties.getEnvironmentName())
                        .withRuns(benchmarkDescriptor.getRuns().orElse(DEFAULT_RUNS))
                        .withSuitePrewarmRuns(benchmarkDescriptor.getSuitePrewarmRuns().orElse(DEFAULT_SUITE_PREWARM_RUNS))
                        .withBenchmarkPrewarmRuns(benchmarkDescriptor.getBenchmarkPrewarmRuns().orElse(DEFAULT_BENCHMARK_PREWARM_RUNS))
                        .withConcurrency(benchmarkDescriptor.getConcurrency().orElse(DEFAULT_CONCURRENCY))
                        .withFrequency(benchmarkDescriptor.getFrequency().map(Duration::ofDays))
                        .withThroughputTest(benchmarkDescriptor.getThroughputTest())
                        .withBeforeBenchmarkMacros(benchmarkDescriptor.getBeforeBenchmarkMacros())
                        .withAfterBenchmarkMacros(benchmarkDescriptor.getAfterBenchmarkMacros())
                        .withBeforeExecutionMacros(benchmarkDescriptor.getBeforeExecutionMacros())
                        .withAfterExecutionMacros(benchmarkDescriptor.getAfterExecutionMacros())
                        .withQueryResults(benchmarkDescriptor.getResults())
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

    private Map<Object, Object> mergeTopLevelVariables(Map<Object, Object> baseYaml)
    {
        ImmutableMap.Builder<Object, Object> result = ImmutableMap.builder();
        for (Map.Entry<Object, Object> entry : baseYaml.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (!(value instanceof Map) && overrides.containsKey(key)) {
                result.put(key, overrides.get(key));
            }
            else {
                result.put(key, value);
            }
        }
        for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (!baseYaml.containsKey(key)) {
                result.put(key, value);
            }
        }
        return result.build();
    }

    private List<BenchmarkDescriptor> createBenchmarkDescriptors(String defaultName, Map<Object, Object> yaml)
    {
        List<Map<String, String>> variablesCombinations = extractVariableMapList(yaml);
        Map<String, String> globalVariables = extractGlobalVariables(yaml);
        globalVariables.putIfAbsent(NAME_KEY, defaultName);

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
        Path benchmarkFilesDir = properties.benchmarksFilesDirs().stream()
                .filter(benchmarkFile::startsWith)
                .findFirst().get();
        String relativePath = benchmarkFilesDir.relativize(benchmarkFile).toString();
        return removeExtension(relativePath);
    }

    private Map<String, String> extractGlobalVariables(Map<Object, Object> yaml)
    {
        return yaml.entrySet().stream()
                .filter(entry -> !entry.getKey().toString().equals(BenchmarkDescriptor.VARIABLES_KEY))
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue() == null ? null : entry.getValue().toString()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractVariableMapList(Map<Object, Object> yaml)
    {
        Map<Object, Map<Object, Object>> variableMaps = (Map) yaml.getOrDefault(BenchmarkDescriptor.VARIABLES_KEY, newHashMap());
        List<Map<String, String>> variableMapList = variableMaps.values()
                .stream()
                .map(YamlUtils::stringifyMultimap)
                .flatMap(variableMap -> CartesianProductUtils.cartesianProduct(variableMap).stream())
                .collect(toList());

        if (variableMapList.isEmpty()) {
            variableMapList.add(newHashMap());
        }

        return variableMapList;
    }

    private Predicate<Path> activeBenchmarks()
    {
        Optional<List<String>> activeBenchmarks = properties.getActiveBenchmarks();
        return activeBenchmarks.map(this::benchmarkNameIn).orElseGet(() -> path -> true);
    }

    private Predicate<Path> benchmarkNameIn(List<String> activeBenchmarks)
    {
        List<String> names = ImmutableList.copyOf(activeBenchmarks);

        return benchmarkFile -> {
            String benchmarkName = benchmarkName(benchmarkFile);
            return names.contains(benchmarkName);
        };
    }

    private void fillUniqueBenchmarkNames(List<Benchmark> benchmarks)
    {
        List<BenchmarkServiceClient.GenerateUniqueNamesRequestItem> namesRequestItems = benchmarks.stream()
                .map(benchmark -> BenchmarkServiceClient.GenerateUniqueNamesRequestItem.generateUniqueNamesRequestItem(benchmark.getName(), benchmark.getNonReservedKeywordVariables()))
                .collect(toList());
        List<String> uniqueBenchmarkNames = benchmarkServiceClient.generateUniqueBenchmarkNames(namesRequestItems);

        checkState(uniqueBenchmarkNames.size() == benchmarks.size());
        for (int i = 0; i < uniqueBenchmarkNames.size(); i++) {
            benchmarks.get(i).setUniqueName(uniqueBenchmarkNames.get(i));
        }
    }

    private List<Benchmark> filterFreshBenchmarks(List<Benchmark> benchmarks)
    {
        List<Benchmark> benchmarksWithFrequencySet = benchmarks.stream()
                .filter(benchmark -> benchmark.getFrequency().isPresent())
                .collect(toList());

        if (benchmarksWithFrequencySet.isEmpty()) {
            return ImmutableList.of();
        }

        List<String> benchmarkUniqueNames = benchmarksWithFrequencySet.stream()
                .map(Benchmark::getUniqueName)
                .collect(toList());
        List<Duration> successfulExecutionAges = benchmarkServiceClient.getBenchmarkSuccessfulExecutionAges(benchmarkUniqueNames);

        return IntStream.range(0, benchmarksWithFrequencySet.size())
                .mapToObj(i -> {
                    Benchmark benchmark = benchmarksWithFrequencySet.get(i);
                    if (successfulExecutionAges.get(i).compareTo(benchmark.getFrequency().get()) <= 0) {
                        return benchmark;
                    }
                    else {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(toList());
    }

    private void printFormattedBenchmarksInfo(String formatString, Collection<Benchmark> benchmarks)
    {
        LOGGER.info(format(formatString, "Benchmark Name", "Data Source", "Runs", "SuitePrewarms", "BenchmarkPrewarms", "Concurrency", "Throughput Test"));
        benchmarks.stream()
                .map(benchmark -> format(formatString,
                        benchmark.getName(),
                        benchmark.getDataSource(),
                        benchmark.getRuns() + "",
                        benchmark.getSuitePrewarmRuns() + "",
                        benchmark.getBenchmarkPrewarmRuns() + "",
                        benchmark.getConcurrency() + "",
                        benchmark.isThroughputTest() + ""))
                .distinct()
                .forEach(LOGGER::info);
    }

    private String createFormatString(Collection<Benchmark> benchmarks)
    {
        int nameMaxLength = benchmarks.stream().mapToInt((benchmark) -> benchmark.getName().length()).max().orElse(10);
        int dataSourceMaxLength = benchmarks.stream().mapToInt((benchmark) -> benchmark.getDataSource().length()).max().orElse(10);
        int indent = 3;
        return "\t| %-" + (nameMaxLength + indent) + "s | %-" + Math.max(dataSourceMaxLength + indent, 11) + "s | %-4s | %-8s | %-11s | %-15s |";
    }
}
