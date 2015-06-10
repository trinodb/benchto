/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

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

    @Value("${executionSequenceId:}")
    private String executionSequenceId;

    @Value("${environment.name}")
    private String environmentName;

    @Value("${graphite.metrics.cpu:#{null}}")
    private String cpuGraphiteExpr;

    @Value("${graphite.metrics.memory:#{null}}")
    private String memoryGraphiteExpr;

    @Value("${graphite.metrics.network:#{null}}")
    private String networkGraphiteExpr;

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

    public String getExecutionSequenceId()
    {
        return executionSequenceId;
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public Optional<String> getCpuGraphiteExpr()
    {
        return Optional.ofNullable(cpuGraphiteExpr);
    }

    public Optional<String> getMemoryGraphiteExpr()
    {
        return Optional.ofNullable(memoryGraphiteExpr);
    }

    public Optional<String> getNetworkGraphiteExpr()
    {
        return Optional.ofNullable(networkGraphiteExpr);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("runs", runs)
                .add("sqlDir", sqlDir)
                .add("executionSequenceId", executionSequenceId)
                .toString();
    }
}
