/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import com.teradata.benchmark.driver.graphite.GraphiteProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.time.format.DateTimeFormatter;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.teradata.benchmark.driver.utils.TimeUtils.nowUtc;

@Component
public class BenchmarkProperties
{

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS");

    @Value("${runs:3}")
    private int runs;

    @Value("${sql:sql}")
    private String sqlDir;

    @Value("${benchmarks:benchmarks}")
    private String benchmarksDir;

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

    public int getRuns()
    {
        return runs;
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

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("runs", runs)
                .add("sqlDir", sqlDir)
                .add("benchmarksDir", benchmarksDir)
                .add("executionSequenceId", executionSequenceId)
                .add("environmentName", environmentName)
                .add("graphiteProperties", graphiteProperties)
                .toString();
    }
}
