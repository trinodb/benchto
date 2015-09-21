/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchto.driver.loader;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.BenchmarkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

class BenchmarkByActiveVariablesFilter
        implements Predicate<Benchmark>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkByActiveVariablesFilter.class);

    private final BenchmarkProperties properties;

    public BenchmarkByActiveVariablesFilter(BenchmarkProperties properties)
    {
        this.properties = requireNonNull(properties, "properties is null");
    }

    @Override
    public boolean test(Benchmark benchmark)
    {
        Optional<Map<String, String>> activeVariables = properties.getActiveVariables();
        if (!activeVariables.isPresent()) {
            return true;
        }
        Map<String, String> benchmarkVariables = benchmark.getVariables();
        for (String variableKey : activeVariables.get().keySet()) {
            if (benchmarkVariables.containsKey(variableKey)) {
                String benchmarkVariableValue = benchmarkVariables.get(variableKey);
                String activeVariableValue = activeVariables.get().get(variableKey);
                if (!benchmarkVariableValue.equals(activeVariableValue)) {
                    LOGGER.debug("Benchmark '{}' is EXCLUDED because mismatches on variable '{}', have '{}' while expected '{}'",
                            benchmark.getName(), variableKey, benchmarkVariableValue, activeVariableValue);
                    return false;
                }
            }
        }
        return true;
    }
}
