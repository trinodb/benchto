/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchmark.driver.utils;

import com.teradata.benchmark.driver.Benchmark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

public class FilterUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterUtils.class);

    /**
     * @return Predicate which returns true when benchmark name contains any of given string
     */
    public static Predicate<Benchmark> benchmarkNameMatchesTo(List<String> strings)
    {
        return benchmark -> {
            boolean included = strings.stream()
                    .anyMatch(wildcardMatcher -> benchmark.getName().contains(wildcardMatcher));
            LOGGER.debug("Benchmark: '{}' will be {}.", benchmark.getName(), included ? "included" : "excluded");
            return included;
        };
    }
}
