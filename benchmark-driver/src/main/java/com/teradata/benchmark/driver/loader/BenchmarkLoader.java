/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.utils.NaturalOrderComparator;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.facebook.presto.jdbc.internal.guava.collect.Sets.newLinkedHashSet;
import static com.teradata.benchmark.driver.utils.FilterUtils.benchmarkNameMatchesTo;
import static com.teradata.benchmark.driver.utils.ResourceUtils.asPath;
import static java.lang.String.format;
import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;

@Component
public class BenchmarkLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkLoader.class);

    private static final String BENCHMARK_FILE_SUFFIX = "yaml";

    private static final int DEFAULT_RUNS = 3;
    private static final int DEFAULT_CONCURRENCY = 1;
    private static final List<String> DEFAULT_BEFORE_BENCHMARK_MACROS = ImmutableList.of();
    private static final int DEFAULT_PREWARM_RUNS = 0;

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private QueryLoader queryLoader;

    public List<Benchmark> loadBenchmarks(String sequenceId)
    {
        try {
            List<Path> benchmarkFiles = findBenchmarkFiles();

            List<Benchmark> allBenchmarks = loadBenchmarks(sequenceId, benchmarkFiles);
            LOGGER.debug("All benchmarks: {}", allBenchmarks);

            List<Benchmark> allBenchmarksSorted = sortBenchmarksByName(allBenchmarks);
            LOGGER.debug("All benchmarks sorted: {}", allBenchmarks);

            List<Benchmark> includedBenchmarks = filterBenchmarks(allBenchmarksSorted);
            Set<Benchmark> excludedBenchmarks = newLinkedHashSet(allBenchmarksSorted);
            excludedBenchmarks.removeAll(includedBenchmarks);

            String formatString = createFormatString(allBenchmarks);
            LOGGER.info("Excluded Benchmarks:");
            printFormattedBenchmarksInfo(formatString, excludedBenchmarks);

            LOGGER.info("Selected Benchmarks:");
            printFormattedBenchmarksInfo(formatString, includedBenchmarks);

            return includedBenchmarks;
        }
        catch (IOException e) {
            throw new BenchmarkExecutionException("could not load benchmarks", e);
        }
    }

    private List<Path> findBenchmarkFiles()
            throws IOException
    {
        LOGGER.info("Searching for benchmarks in classpath ...");

        List<Path> benchmarkFiles = Files
                .walk(benchmarksFilesPath())
                .filter(file -> isRegularFile(file) && file.toString().endsWith(BENCHMARK_FILE_SUFFIX))
                .collect(toList());
        benchmarkFiles.stream().forEach((path) -> LOGGER.info("Benchmark found: {}", path.toString()));

        return benchmarkFiles;
    }

    private List<Benchmark> loadBenchmarks(String sequenceId, List<Path> benchmarkFiles)
    {
        return benchmarkFiles.stream()
                .flatMap(file -> loadBenchmarks(sequenceId, file).stream())
                .collect(toList());
    }

    private List<Benchmark> sortBenchmarksByName(List<Benchmark> benchmarks)
    {
        return benchmarks.stream()
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
            BenchmarkDescriptor descriptor = BenchmarkDescriptor.loadFromFile(benchmarkFile);
            List<Map<String, String>> variableMapList = descriptor.getVariableMapList();

            return variableMapList
                    .stream()
                    .map(variables -> createBenchmark(sequenceId, descriptor, variables))
                    .collect(toList());
        }
        catch (IOException e) {
            throw new BenchmarkExecutionException("could not load benchmark: " + benchmarkFile, e);
        }
    }

    private Benchmark createBenchmark(String sequenceId, BenchmarkDescriptor descriptor, Map<String, String> variables)
    {
        String benchmarkName = generateBenchmarkName(descriptor, variables);
        List<Query> queries = loadQueries(descriptor.getQueryNames(), variables);
        return new Benchmark(
                benchmarkName, sequenceId, descriptor.getDataSource(), properties.getEnvironmentName(), queries,
                descriptor.getRuns().orElse(DEFAULT_RUNS),
                descriptor.getPrewarmRepeats().orElse(DEFAULT_PREWARM_RUNS),
                descriptor.getConcurrency().orElse(DEFAULT_CONCURRENCY),
                descriptor.getBeforeBenchmarkMacros().orElse(DEFAULT_BEFORE_BENCHMARK_MACROS),
                variables);
    }

    private List<Query> loadQueries(List<String> queryNames, Map<String, String> variables)
    {
        return queryNames
                .stream()
                .map(queryName -> queryLoader.loadFromFile(sqlFilesPath().resolve(queryName), variables))
                .collect(toList());
    }

    private Path benchmarksFilesPath()
    {
        return asPath(properties.getBenchmarksDir());
    }

    private Path sqlFilesPath()
    {
        return asPath(properties.getSqlDir());
    }

    private String generateBenchmarkName(BenchmarkDescriptor descriptor, Map<String, String> variables)
    {
        String relativePath = benchmarksFilesPath().relativize(descriptor.getDescriptorPath()).toString();
        String pathWithoutExtension = removeExtension(relativePath);
        StringBuilder benchmarkName = new StringBuilder(pathWithoutExtension);

        for (Map.Entry<String, String> variablesEntry : variables.entrySet()) {
            benchmarkName.append('_');
            benchmarkName.append(variablesEntry.getKey());
            benchmarkName.append('=');
            benchmarkName.append(variablesEntry.getValue());
        }

        benchmarkName.append("_env=");
        benchmarkName.append(properties.getEnvironmentName());

        return sanitizeBenchmarkName(benchmarkName.toString());
    }

    /**
     * Leaves in benchmark name only alphanumerics, underscores and dashes
     * <p/>
     * TODO: We should better do that where we passing benchmark name into REST URL
     */
    private String sanitizeBenchmarkName(String benchmarkName)
    {
        return benchmarkName.replaceAll("[^A-Za-z0-9_=-]", "_");
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

    private void printFormattedBenchmarksInfo(String formatString, Collection<Benchmark> benchmarks)
    {
        LOGGER.info(format(formatString, "Benchmark Name", "Data Source", "Runs", "Prewarm Runs", "Concurrency"));
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
        return "\t| %-" + (nameMaxLength + indent) + "s | %-" + (dataSourceMaxLength + indent) + "s | %-4s | %-12s | %-11s |";
    }
}
