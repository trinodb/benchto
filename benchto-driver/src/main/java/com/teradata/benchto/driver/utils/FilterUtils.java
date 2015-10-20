/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchto.driver.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class FilterUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterUtils.class);

    /**
     * @return Predicate which returns true when path contains any of given string
     */
    public static Predicate<Path> pathContainsAny(List<String> strings)
    {
        return path -> {
            boolean included = strings.stream()
                    .anyMatch(wildcardMatcher -> path.toString().contains(wildcardMatcher));
            return included;
        };
    }
}
