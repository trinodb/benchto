/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.BenchmarkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

class BenchmarkByActiveVariablesFilter
        implements Predicate<Benchmark>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkByActiveVariablesFilter.class);

    private final Map<String, Pattern> variablePatterns;

    public BenchmarkByActiveVariablesFilter(BenchmarkProperties properties)
    {
        Optional<Map<String, String>> activeVariables = requireNonNull(properties, "properties is null").getActiveVariables();
        ImmutableMap.Builder<String, Pattern> builder = ImmutableMap.<String, Pattern>builder();
        if (activeVariables.isPresent()) {
            for (String variableKey : activeVariables.get().keySet()) {
                builder.put(variableKey, Pattern.compile(activeVariables.get().get(variableKey)));
            }
        }
        variablePatterns = builder.build();
    }

    @Override
    public boolean test(Benchmark benchmark)
    {
        Map<String, String> benchmarkVariables = benchmark.getVariables();
        for (String variableKey : variablePatterns.keySet()) {
            if (benchmarkVariables.containsKey(variableKey)) {
                Pattern valuePattern = variablePatterns.get(variableKey);
                String benchmarkVariableValue = benchmarkVariables.get(variableKey);
                if (!valuePattern.matcher(benchmarkVariableValue).find()) {
                    LOGGER.debug("Benchmark '{}' is EXCLUDED because mismatches on variable '{}', have '{}' does not match to '{}'",
                            benchmark.getName(), variableKey, valuePattern, benchmarkVariableValue);
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
