/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.teradata.benchmark.driver.BenchmarkProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

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

        for (Map.Entry<String, String> variablesEntry : benchmarkDescriptor.getVariables().entrySet()) {
            if (RESERVED_KEYWORD.contains(variablesEntry.getKey())) {
                continue;
            }
            benchmarkName.append('_');
            benchmarkName.append(variablesEntry.getKey());
            benchmarkName.append('=');
            benchmarkName.append(variablesEntry.getValue());
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
