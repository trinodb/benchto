/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.teradata.benchmark.driver.graphite.GraphiteProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.teradata.benchmark.driver.utils.TimeUtils.nowUtc;

@Component
public class BenchmarkProperties
{

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS");

    @Value("${sql:sql}")
    private String sqlDir;

    @Value("${benchmarks:benchmarks}")
    private String benchmarksDir;

    @Value("${activeBenchmarks:#{null}}")
    private String activeBenchmarks;

    @Value("${executionSequenceId:}")
    private String executionSequenceId;

    @Value("${environment.name}")
    private String environmentName;

    @Autowired
    private GraphiteProperties graphiteProperties;

    @PostConstruct
    public void initExecutionSequenceId()
    {
        if (isNullOrEmpty(executionSequenceId)) {
            executionSequenceId = nowUtc().format(DATE_TIME_FORMATTER);
        }
    }

    public String getSqlDir()
    {
        return sqlDir;
    }

    public String getBenchmarksDir() {
        return benchmarksDir;
    }

    public String getExecutionSequenceId()
    {
        return executionSequenceId;
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
        if (isNullOrEmpty(activeBenchmarks)) {
            return Optional.empty();
        }
        Iterable<String> splittedBenchmarks = Splitter.on(",").trimResults().split(activeBenchmarks);
        return Optional.of(ImmutableList.copyOf(splittedBenchmarks));
    }

    @Override
    public String toString()
    {
        MoreObjects.ToStringHelper toStringHelper = toStringHelper(this)
                .add("sqlDir", sqlDir)
                .add("benchmarksDir", benchmarksDir)
                .add("executionSequenceId", executionSequenceId)
                .add("environmentName", environmentName)
                .add("graphiteProperties", graphiteProperties)
                .add("graphiteProperties", graphiteProperties);
        Optional<List<String>> benchmarks = getActiveBenchmarks();
        if (benchmarks.isPresent()) {
            toStringHelper.add("activeBenchmarks", benchmarks.get());
        }
        return toStringHelper.toString();
    }
}
