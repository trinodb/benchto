/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.utils;

import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static com.facebook.presto.jdbc.internal.guava.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Useful methods for dealing with Yaml files.
 */
public final class YamlUtils
{
    public static Object loadYamlFromString(String string, Class clazz)
    {
        Yaml yaml = new Yaml();
        return yaml.load(string);
    }

    public static List<String> stringifyList(List<Object> collection)
    {
        return collection.stream().map(Object::toString).collect(toList());
    }

    public static Map<String, List<String>> stringifyMultimap(Map<String, Object> variableMap)
    {
        return variableMap.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> stringifyList(asList(e.getValue()))));
    }

    public static <T> List<T> asList(T object)
    {
        return object instanceof Iterable ? newArrayList((Iterable) object) : newArrayList(object);
    }

    private YamlUtils()
    {
    }
}
