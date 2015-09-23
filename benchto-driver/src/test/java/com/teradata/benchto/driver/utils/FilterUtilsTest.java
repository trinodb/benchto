/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchto.driver.utils;

import com.google.common.collect.ImmutableList;
import com.teradata.benchto.driver.Benchmark;
import org.junit.Test;

import java.util.function.Predicate;

import static com.teradata.benchto.driver.utils.FilterUtils.benchmarkNameMatchesTo;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class FilterUtilsTest
{
    @Test
    public void test()
    {
        Predicate<Benchmark> pathPredicate = benchmarkNameMatchesTo(ImmutableList.of("simple", "YACK"));

        assertThat(pathPredicate.test(benchmarkWithName("simple"))).isTrue();
        assertThat(pathPredicate.test(benchmarkWithName("dir/simple"))).isTrue();
        assertThat(pathPredicate.test(benchmarkWithName("dir/simple.yaml"))).isTrue();
        assertThat(pathPredicate.test(benchmarkWithName("simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(benchmarkWithName("dir/simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(benchmarkWithName("dir/YACK/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(benchmarkWithName("dir/YA-CK/file.yaml"))).isFalse();
    }

    private Benchmark benchmarkWithName(String name)
    {
        return new Benchmark.BenchmarkBuilder(name, "", emptyList())
                .build();
    }
}
