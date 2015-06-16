/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.Query;
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

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toList;

@Component
public class BenchmarkLoader
{
    private static final String BENCHMARK_FILE_SUFFIX = "yaml";

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private QueryLoader queryLoader;

    public List<Benchmark> loadBenchmarks()
    {
        try {
            return Files.walk(sqlFilesPath())
                    .filter(file -> isRegularFile(file) && file.toString().endsWith(BENCHMARK_FILE_SUFFIX))
                    .sorted((p1, p2) -> p1.toString().compareTo(p2.toString()))
                    .flatMap(file -> loadBenchmarks(file).stream())
                    .collect(toList());
        }
        catch (URISyntaxException | IOException e) {
            throw new BenchmarkExecutionException("could not load benchmarks", e);
        }
    }

    public List<Benchmark> loadBenchmarks(Path benchmarkFile)
    {
        try {
            BenchmarkDescriptor descriptor = BenchmarkDescriptor.loadFromFile(benchmarkFile);

            List<Map<String, String>> variableMapList = descriptor.getVariableMapList();
            if (variableMapList.isEmpty()) {
                variableMapList.add(newHashMap());
            }

            return variableMapList
                    .stream()
                    .map(variables -> createBenchmark(benchmarkFile, descriptor, variables))
                    .collect(toList());
        }
        catch (IOException e) {
            throw new BenchmarkExecutionException("could not load benchmark: " + benchmarkFile, e);
        }
    }

    private Benchmark createBenchmark(Path benchmarkFile, BenchmarkDescriptor descriptor, Map<String, String> variables)
    {
        List<Query> queries = loadQueries(benchmarkFile, descriptor.getQueryNames(), variables);
        return new Benchmark(descriptor.getName(), queries, descriptor.getRuns(), descriptor.getConcurrency());
    }

    private List<Query> loadQueries(Path benchmarkFile, List<String> queryNames, Map<String, String> variables)
    {
        return queryNames
                .stream()
                .map(queryName -> queryLoader.loadFromFile(benchmarkFile.getParent().resolve(queryName), variables))
                .collect(toList());
    }

    private Path sqlFilesPath()
            throws URISyntaxException
    {
        URL sqlDir = getSystemClassLoader().getResource(properties.getSqlDir());
        if (sqlDir != null) {
            return Paths.get(sqlDir.toURI());
        }
        return FileSystems.getDefault().getPath(properties.getSqlDir());
    }
}
