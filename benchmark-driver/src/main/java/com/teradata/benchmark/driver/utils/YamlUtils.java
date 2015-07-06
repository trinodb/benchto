/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Useful methods for dealing with Yaml files.
 */
public final class YamlUtils
{
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYamlFromString(String string)
    {
        Yaml yaml = new Yaml();
        return (Map<String, Object>) yaml.load(string);
    }

    public static Map<String, List<String>> stringifyMultimap(Map<String, Object> variableMap)
    {
        return variableMap.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> asStringList(e.getValue())));
    }

    public static List<String> asStringList(Object object)
    {
        if (!(object instanceof Iterable<?>)) {
            return ImmutableList.copyOf(Splitter.on(",").trimResults().omitEmptyStrings().split(object.toString()));
        }
        else {
            Iterable<?> iterable = (Iterable<?>) object;
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(Object::toString)
                    .collect(toList());
        }
    }

    private YamlUtils()
    {
    }
}
