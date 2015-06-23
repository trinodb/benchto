/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.domain.Benchmark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.collect.Maps.newHashMap;
import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;

@Component
public class BenchmarkLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkLoader.class);

    private static final String BENCHMARK_FILE_SUFFIX = "yaml";

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private QueryLoader queryLoader;

    public List<Benchmark> loadBenchmarks(String sequenceId)
    {
        try {
            return Files.walk(benchmarksFilesPath())
                    .filter(file -> isRegularFile(file) && file.toString().endsWith(BENCHMARK_FILE_SUFFIX))
                    .sorted((p1, p2) -> p1.toString().compareTo(p2.toString()))
                    .filter(pathIsListedInBenchmarksListIfProvided())
                    .flatMap(file -> loadBenchmarks(sequenceId, file).stream())
                    .collect(toList());
        }
        catch (IOException e) {
            throw new BenchmarkExecutionException("could not load benchmarks", e);
        }
    }

    public List<Benchmark> loadBenchmarks(String sequenceId, Path benchmarkFile)
    {
        try {
            BenchmarkDescriptor descriptor = BenchmarkDescriptor.loadFromFile(benchmarkFile);
            List<Map<String, String>> variableMapList = descriptor.getVariableMapList();
            if (variableMapList.isEmpty()) {
                variableMapList.add(newHashMap());
            }

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
        String benchmarkName = benchmarkName(descriptor, variables);
        List<Query> queries = loadQueries(descriptor.getQueryNames(), variables);
        return new Benchmark(benchmarkName, sequenceId, descriptor.getDataSource(), properties.getEnvironmentName(), queries,
                descriptor.getRuns(), descriptor.getConcurrency(), variables);
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

    private Path asPath(String resourcePath)
    {
        URL resourceUrl = BenchmarkLoader.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            try {
                return Paths.get(resourceUrl.toURI());
            }
            catch (URISyntaxException e) {
                throw new BenchmarkExecutionException("Cant resolve URL", e);
            }
        }
        return FileSystems.getDefault().getPath(resourcePath);
    }

    private String benchmarkName(BenchmarkDescriptor descriptor, Map<String, String> variables)
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
     * <p>
     * TODO: We should better do that where we passing benchmark name into REST URL
     */
    private String sanitizeBenchmarkName(String benchmarkName)
    {
        return benchmarkName.replaceAll("[^A-Za-z0-9_=-]", "_");
    }

    private Predicate<Path> pathIsListedInBenchmarksListIfProvided()
    {
        return path -> {
            Optional<List<String>> benchmarks = properties.getActiveBenchmarks();
            if (benchmarks.isPresent()) {
                boolean included = benchmarks.get().contains(path.getFileName().toString());
                LOGGER.info("Benchmark: '{}' will be {}.", path.toString(), included ? "included" : "EXCLUDED");
                return included;
            }
            return true;
        };
    }
}
