/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchto.driver.loader;

import com.google.common.collect.ImmutableMap;
import com.teradata.benchto.driver.Benchmark;
import org.junit.Test;

import static com.teradata.benchto.driver.BenchmarkPropertiesTest.benchmarkPropertiesWithActiveVariables;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BenchmarkByActiveVariablesFilterTest
{
    @Test
    public void filter()
    {
        BenchmarkByActiveVariablesFilter filter = new BenchmarkByActiveVariablesFilter(benchmarkPropertiesWithActiveVariables("ala=k.t"));

        assertThat(filter.test(benchmarkWithVariable("ala", "pies"))).isFalse();
        assertThat(filter.test(benchmarkWithVariable("ala", "kot"))).isTrue();
        assertThat(filter.test(benchmarkWithVariable("ala", "kat"))).isTrue();
        assertThat(filter.test(benchmarkWithVariable("ala", "katar"))).isTrue();
        assertThat(filter.test(benchmarkWithVariable("tola", "kot"))).isFalse();
        assertThat(filter.test(benchmarkWithVariable("tola", "pies"))).isFalse();
    }

    private Benchmark benchmarkWithVariable(String key, String value)
    {
        Benchmark benchmark = mock(Benchmark.class);
        when(benchmark.getVariables())
                .thenReturn(ImmutableMap.of(key, value));

        return benchmark;
    }
}
