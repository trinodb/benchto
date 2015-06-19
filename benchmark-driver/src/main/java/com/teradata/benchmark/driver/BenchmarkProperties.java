/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.base.MoreObjects;
import com.teradata.benchmark.driver.graphite.GraphiteProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.teradata.benchmark.driver.utils.PropertiesUtils.splitProperty;

@Component
public class BenchmarkProperties
{

    @Value("${sql:sql}")
    private String sqlDir;

    @Value("${benchmarks:benchmarks}")
    private String benchmarksDir;

    /**
     * Active benchmarks. If this property is set benchmarks will be filtered by name.
     */
    @Value("${activeBenchmarks:#{null}}")
    private String activeBenchmarks;

    /**
     * Execution identifier. Should be unique between runs. If not set, it will be automatically set based on timestamp.
     */
    @Value("${executionSequenceId:#{null}}")
    private String executionSequenceId;

    /**
     * List of macros to be executed before benchmark.
     */
    @Value("${execution.before-benchmark:#{null}}")
    private String beforeBenchmarkMacros;

    @Value("${environment.name}")
    private String environmentName;

    @Autowired
    private GraphiteProperties graphiteProperties;

    public String getSqlDir()
    {
        return sqlDir;
    }

    public String getBenchmarksDir()
    {
        return benchmarksDir;
    }

    public Optional<String> getExecutionSequenceId()
    {
        return Optional.ofNullable(executionSequenceId);
    }

    public Optional<List<String>> getBeforeBenchmarkMacros()
    {
        return splitProperty(beforeBenchmarkMacros);
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public GraphiteProperties getGraphiteProperties()
    {
        return graphiteProperties;
    }

    public Optional<List<String>> getActiveBenchmarks()
    {
        return splitProperty(activeBenchmarks);
    }

    @Override
    public String toString()
    {
        MoreObjects.ToStringHelper toStringHelper = toStringHelper(this)
                .add("sqlDir", sqlDir)
                .add("benchmarksDir", benchmarksDir)
                .add("executionSequenceId", executionSequenceId)
                .add("environmentName", environmentName)
                .add("graphiteProperties", graphiteProperties);
        Optional<List<String>> benchmarks = getActiveBenchmarks();
        if (benchmarks.isPresent()) {
            toStringHelper.add("activeBenchmarks", benchmarks.get());
        }
        return toStringHelper.toString();
    }
}
