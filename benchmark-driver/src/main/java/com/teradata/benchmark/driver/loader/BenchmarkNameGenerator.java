/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.Ordering;
import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.BenchmarkProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

import static com.teradata.benchmark.driver.loader.BenchmarkDescriptor.RESERVED_KEYWORD;
import static org.apache.commons.io.FilenameUtils.removeExtension;

@Component
public class BenchmarkNameGenerator
{

    @Autowired
    private BenchmarkProperties properties;

    public String generateBenchmarkName(Path benchmarkFilePath, BenchmarkDescriptor benchmarkDescriptor)
    {
        String relativePath = properties.benchmarksFilesPath().relativize(benchmarkFilePath).toString();
        String pathWithoutExtension = removeExtension(relativePath);
        StringBuilder benchmarkName = new StringBuilder(pathWithoutExtension);

        List<String> orderedVariableNames = ImmutableList.copyOf(Ordering.natural().sortedCopy(benchmarkDescriptor.getVariables().keySet()));
        for (String variableName : orderedVariableNames) {
            if (RESERVED_KEYWORD.contains(variableName)) {
                continue;
            }
            benchmarkName.append('_');
            benchmarkName.append(variableName);
            benchmarkName.append('=');
            benchmarkName.append(benchmarkDescriptor.getVariables().get(variableName));
        }

        if (benchmarkDescriptor.getConcurrency().isPresent() && benchmarkDescriptor.getConcurrency().get() > 1) {
            benchmarkName.append("_concurrency=");
            benchmarkName.append(benchmarkDescriptor.getConcurrency().get());
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
}
