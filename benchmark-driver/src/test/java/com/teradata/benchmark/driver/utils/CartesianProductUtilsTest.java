/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.utils;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.facebook.presto.jdbc.internal.guava.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;

public class CartesianProductUtilsTest
{
    @Test
    public void shouldComputeCartesianProduct()
    {
        List<Map<Integer, String>> product = CartesianProductUtils.cartesianProduct(ImmutableMap.<Integer, List<String>>builder()
                .put(1, newArrayList("1", "2", "3"))
                .put(2, newArrayList("foo", "bar"))
                .build());

        assertThat(product).containsExactly(
                ImmutableMap.of(1, "1", 2, "foo"),
                ImmutableMap.of(1, "1", 2, "bar"),
                ImmutableMap.of(1, "2", 2, "foo"),
                ImmutableMap.of(1, "2", 2, "bar"),
                ImmutableMap.of(1, "3", 2, "foo"),
                ImmutableMap.of(1, "3", 2, "bar")
        );
    }

    @Test
    public void shouldComputeEmptyCartesianProduct()
    {
        assertThat(CartesianProductUtils.cartesianProduct(newHashMap())).isEmpty();
    }
}
