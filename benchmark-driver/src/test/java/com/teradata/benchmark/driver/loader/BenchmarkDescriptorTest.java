/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.IOException;

import static com.teradata.benchmark.driver.loader.BenchmarkDescriptor.loadFromString;
import static org.assertj.core.api.Assertions.assertThat;

public class BenchmarkDescriptorTest
{
    private static final String DEFAULT_NAME = "xyz";

    private static final String SIMPLE_BENCHMARK = "" +
            "datasource: foo\n" +
            "query-names: [q1, q2, 1, 2]";

    private static final String BENCHMARK_NO_DATA_SOURCE = "query-names: [q1, q2, 1, 2]";

    private static final String BENCHMARK_WITH_VARIABLES = "" +
            "datasource: foo\n" +
            "query-names: [q1, q2, 1, 2]\n" +
            "variables:\n" +
            "  combinations1:\n" +
            "    size: [1GB, 2GB]\n" +
            "    format: [txt, orc]\n" +
            "  combinations2:\n" +
            "    size: 10GB\n" +
            "    format: parquet\n";

    private static final String CONCURRENT_BENCHMARK = "" +
            "datasource: foo\n" +
            "query-names: [q1, q2, 1, 2]\n" +
            "runs: 10\n" +
            "concurrency: 20";

    private static final String CONCURRENT_BENCHMARK_NO_RUNS = "" +
            "datasource: foo\n" +
            "query-names: [q1, q2, 1, 2]\n" +
            "concurrency: 20";

    @Test
    public void shouldLoadSimpleBenchmark()
            throws IOException
    {
        BenchmarkDescriptor descriptor = descriptorFromString(SIMPLE_BENCHMARK);
        assertThat(descriptor.getQueryNames()).containsExactly("q1", "q2", "1", "2");
        assertThat(descriptor.getDataSource()).isEqualTo("foo");
        assertThat(descriptor.getRuns()).isEqualTo(3);
        assertThat(descriptor.getConcurrency()).isEqualTo(1);
    }

    @Test
    public void shouldLoadConcurrentBenchmark()
            throws IOException
    {
        BenchmarkDescriptor descriptor = descriptorFromString(CONCURRENT_BENCHMARK);
        assertThat(descriptor.getDataSource()).isEqualTo("foo");
        assertThat(descriptor.getQueryNames()).containsExactly("q1", "q2", "1", "2");
        assertThat(descriptor.getRuns()).isEqualTo(10);
        assertThat(descriptor.getConcurrency()).isEqualTo(20);
    }

    @Test
    public void shouldUseConcurrencyAsRuns()
            throws IOException
    {
        BenchmarkDescriptor descriptor = descriptorFromString(CONCURRENT_BENCHMARK_NO_RUNS);
        assertThat(descriptor.getConcurrency()).isEqualTo(20);
        assertThat(descriptor.getRuns()).isEqualTo(descriptor.getConcurrency());
    }

    @Test
    public void shouldLoadBenchmarkWithVariables()
            throws IOException
    {
        BenchmarkDescriptor descriptor = descriptorFromString(BENCHMARK_WITH_VARIABLES);
        assertThat(descriptor.getVariableMapList()).containsExactly(
                ImmutableMap.of("size", "1GB", "format", "txt"),
                ImmutableMap.of("size", "1GB", "format", "orc"),
                ImmutableMap.of("size", "2GB", "format", "txt"),
                ImmutableMap.of("size", "2GB", "format", "orc"),
                ImmutableMap.of("size", "10GB", "format", "parquet")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBenchmarkNoDataSource()
            throws IOException
    {
        descriptorFromString(BENCHMARK_NO_DATA_SOURCE);
    }

    private BenchmarkDescriptor descriptorFromString(String string)
            throws IOException
    {
        return loadFromString(null, string);
    }
}
