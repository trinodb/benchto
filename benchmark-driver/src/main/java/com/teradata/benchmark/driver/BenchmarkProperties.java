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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;
import static com.teradata.benchmark.driver.utils.PropertiesUtils.splitProperty;
import static com.teradata.benchmark.driver.utils.ResourceUtils.asPath;
import static java.util.stream.Collectors.toMap;

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
     * Active variables. If this property is set benchmarks will be filtered by their variable content.
     */
    @Value("${activeVariables:#{null}}")
    private String activeVariables;

    /**
     * Execution identifier. Should be unique between runs. If not set, it will be automatically set based on timestamp.
     */
    @Value("${executionSequenceId:#{null}}")
    private String executionSequenceId;

    @Value("${environment.name}")
    private String environmentName;

    @Value("${macroExecutions.healthCheck:#{null}}")
    private String healthCheckMacros;

    @Value("${macroExecutions.beforeAll:#{null}}")
    private String beforeAllMacros;

    @Value("${macroExecutions.afterAll:#{null}}")
    private String afterAllMacros;

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

    public Path benchmarksFilesPath()
    {
        return asPath(getBenchmarksDir());
    }

    public Optional<String> getExecutionSequenceId()
    {
        return Optional.ofNullable(executionSequenceId);
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public Optional<List<String>> getActiveBenchmarks()
    {
        return splitProperty(activeBenchmarks);
    }

    public Optional<Map<String, String>> getActiveVariables()
    {
        Optional<List<String>> variables = splitProperty(activeVariables);
        if (!variables.isPresent()) {
            return Optional.empty();
        }
        Map<String, String> variablesMap = variables.get().stream()
                .map(variable -> {
                    List<String> variablePairList = ImmutableList.copyOf(Splitter.on("=").trimResults().split(variable));
                    checkState(variablePairList.size() == 2,
                            "Incorrect format of variable: '%s', while proper format is 'key=value'", variable);
                    return variablePairList;
                }).collect(toMap(variableList -> variableList.get(0), variableList -> variableList.get(1)));
        return Optional.of(variablesMap);
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
        addForToStringOptionalField(toStringHelper, "activeBenchmarks", getActiveBenchmarks());
        addForToStringOptionalField(toStringHelper, "activeVariables", getActiveVariables());
        addForToStringOptionalField(toStringHelper, "beforeAllMacros", getBeforeAllMacros());
        addForToStringOptionalField(toStringHelper, "afterAllMacros", getAfterAllMacros());
        addForToStringOptionalField(toStringHelper, "healthCheckMacros", getHealthCheckMacros());
        return toStringHelper.toString();
    }

    private void addForToStringOptionalField(MoreObjects.ToStringHelper toStringHelper, String fieldName, Optional optionalField)
    {
        optionalField.ifPresent(value -> toStringHelper.add(fieldName, value));
    }

    public Optional<List<String>> getHealthCheckMacros()
    {
        return splitProperty(this.healthCheckMacros);
    }

    public Optional<List<String>> getBeforeAllMacros()
    {
        return splitProperty(beforeAllMacros);
    }

    public Optional<List<String>> getAfterAllMacros()
    {
        return splitProperty(afterAllMacros);
    }
}
