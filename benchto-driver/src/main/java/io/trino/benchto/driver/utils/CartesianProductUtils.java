/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.benchto.driver.utils;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
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
