/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.utils;

import java.util.List;
import java.util.Map;

import static com.facebook.presto.jdbc.internal.guava.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Utility class for computing cartesian product form a map of lists (multimap).
 */
public final class CartesianProductUtils
{
    /**
     * Computes cartesian product from a multimap. For each combination of multimap values a map is created.
     */
    public static <K, V> List<Map<K, V>> cartesianProduct(Map<K, List<V>> map)
    {
        List<Map<K, V>> cartesianProducts = newArrayList();

        if (map.size() > 0) {
            cartesianProductRecursion(map, newArrayList(map.keySet()), 0, newHashMap(), cartesianProducts);
        }

        return cartesianProducts;
    }

    private static <K, V> void cartesianProductRecursion(
            Map<K, List<V>> map, List<K> keys, int index,
            Map<K, V> accumulator, List<Map<K, V>> cartesianProducts)
    {
        K key = keys.get(index);
        List<V> values = map.get(key);

        for (V value : values) {
            accumulator.put(key, value);

            if (index == keys.size() - 1) {
                cartesianProducts.add(newHashMap(accumulator));
            }
            else {
                cartesianProductRecursion(map, keys, index + 1, accumulator, cartesianProducts);
            }
        }
    }

    private CartesianProductUtils()
    {
    }
}
