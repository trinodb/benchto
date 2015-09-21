/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.google.common.base.Joiner;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.BenchmarkProperties;
import com.teradata.benchto.driver.DriverApp;
import com.teradata.benchto.driver.Query;
import com.teradata.benchto.driver.service.BenchmarkServiceClient;
import freemarker.template.Configuration;
import org.assertj.core.api.MapAssert;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class BenchmarkDescriptorTest
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private BenchmarkProperties benchmarkProperties;

    private BenchmarkLoader benchmarkLoader;

    @Before
    public void setupBenchmarkLoader()
            throws Exception
    {
        QueryLoader queryLoader = mockQueryLoader();
        benchmarkProperties = new BenchmarkProperties();
        BenchmarkServiceClient benchmarkServiceClient = mockBenchmarkServiceClient();
        Configuration freemarkerConfiguration = new DriverApp().freemarkerConfiguration().createConfiguration();

        benchmarkLoader = new BenchmarkLoader();

        ReflectionTestUtils.setField(benchmarkLoader, "properties", benchmarkProperties);
        ReflectionTestUtils.setField(benchmarkLoader, "queryLoader", queryLoader);
        ReflectionTestUtils.setField(benchmarkLoader, "benchmarkServiceClient", benchmarkServiceClient);
        ReflectionTestUtils.setField(benchmarkLoader, "freemarkerConfiguration", freemarkerConfiguration);
    }

    private QueryLoader mockQueryLoader()
    {
        return new QueryLoader()
        {
            @Override
            public Query loadFromFile(String queryName)
            {
                return new Query(queryName, "");
            }
        };
    }

    private BenchmarkServiceClient mockBenchmarkServiceClient()
    {
        return new BenchmarkServiceClient()
        {
            @Override
            public String[] generateUniqueBenchmarkNames(List<GenerateUniqueNamesRequestItem> generateUniqueNamesRequestItems)
            {
                return generateUniqueNamesRequestItems.stream()
                        .map(requestItem -> requestItem.getName() + "_" + Joiner.on("_").withKeyValueSeparator("=").join(requestItem.getVariables().entrySet()))
                        .toArray(String[]::new);
            }
        };
    }

    @Test
    public void shouldLoadSimpleBenchmark()
            throws IOException
    {
        List<Benchmark> benchmarks = loadBenchmarkWithName("simple-benchmark");
        assertThat(benchmarks).hasSize(1);

        Benchmark benchmark = benchmarks.get(0);
        assertThat(benchmark.getQueries()).extracting("name").containsExactly("q1", "q2", "1", "2");
        assertThat(benchmark.getDataSource()).isEqualTo("foo");
        assertThat(benchmark.getRuns()).isEqualTo(3);
        assertThat(benchmark.getConcurrency()).isEqualTo(1);
        assertThat(benchmark.getBeforeBenchmarkMacros()).isEqualTo(ImmutableList.of("no-op", "no-op2"));
        assertThat(benchmark.getAfterBenchmarkMacros()).isEqualTo(ImmutableList.of("no-op2"));
        assertThat(benchmark.getPrewarmRuns()).isEqualTo(2);
    }

    @Test
    public void shouldLoadConcurrentBenchmark()
            throws IOException
    {
        List<Benchmark> benchmarks = loadBenchmarkWithName("concurrent-benchmark");
        assertThat(benchmarks).hasSize(1);

        Benchmark benchmark = benchmarks.get(0);
        assertThat(benchmark.getDataSource()).isEqualTo("foo");
        assertThat(benchmark.getQueries()).extracting("name").containsExactly("q1", "q2", "1", "2");
        assertThat(benchmark.getRuns()).isEqualTo(10);
        assertThat(benchmark.getConcurrency()).isEqualTo(20);
        assertThat(benchmark.getAfterBenchmarkMacros()).isEmpty();
        assertThat(benchmark.getBeforeBenchmarkMacros()).isEmpty();
    }

    @Test
    public void shouldLoadBenchmarkWithVariables()
            throws IOException
    {
        List<Benchmark> benchmarks = loadBenchmarkWithName("multi-variables-benchmark");
        assertThat(benchmarks).hasSize(5);

        for (Benchmark benchmark : benchmarks) {
            assertThat(benchmark.getDataSource()).isEqualTo("foo");
            assertThat(benchmark.getQueries()).extracting("name").containsExactly("q1", "q2", "1", "2");
        }

        assertThatBenchmarkWithEntries(benchmarks, entry("size", "1GB"), entry("format", "txt"))
                .containsOnly(entry("size", "1GB"), entry("format", "txt"), entry("pattern", "1GB-txt"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "1GB"), entry("format", "orc"))
                .containsOnly(entry("size", "1GB"), entry("format", "orc"), entry("pattern", "1GB-orc"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "2GB"), entry("format", "txt"))
                .containsOnly(entry("size", "2GB"), entry("format", "txt"), entry("pattern", "2GB-txt"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "2GB"), entry("format", "orc"))
                .containsOnly(entry("size", "2GB"), entry("format", "orc"), entry("pattern", "2GB-orc"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "10GB"), entry("format", "parquet"))
                .containsOnly(entry("size", "10GB"), entry("format", "parquet"), entry("pattern", "10GB-parquet"));
    }

    @Test
    public void benchmarkWithCycleVariables()
            throws IOException
    {
        thrown.expect(BenchmarkExecutionException.class);
        thrown.expectMessage("Recursive value substitution is not supported, invalid a: ${b}");

        loadBenchmarkWithName("cycle-variables-benchmark", "unit-benchmarks-invalid");
    }

    @Test
    public void quarantineBenchmark_no_quarantine_filtering()
            throws IOException
    {
        List<Benchmark> benchmarks = loadBenchmarkWithName("quarantine-benchmark");
        assertThat(benchmarks).hasSize(1);
    }

    @Test
    public void quarantineBenchmark_quarantine_false_filtering()
            throws IOException
    {
        ReflectionTestUtils.setField(benchmarkProperties, "activeVariables", "quarantine=false");

        List<Benchmark> benchmarks = loadBenchmarkWithName("quarantine-benchmark");
        assertThat(benchmarks).isEmpty();
    }

    @Test
    public void allBenchmarks_no_quarantine_filtering()
            throws IOException
    {
        ReflectionTestUtils.setField(benchmarkProperties, "benchmarksDir", "unit-benchmarks");

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks("sequenceId");
        assertThat(benchmarks).hasSize(8);
    }

    @Test
    public void allBenchmarks_quarantine_false_filtering()
            throws IOException
    {
        ReflectionTestUtils.setField(benchmarkProperties, "benchmarksDir", "unit-benchmarks");
        ReflectionTestUtils.setField(benchmarkProperties, "activeVariables", "quarantine=false");

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks("sequenceId");
        assertThat(benchmarks).hasSize(7);
    }

    private List<Benchmark> loadBenchmarkWithName(String benchmarkName)
    {
        return loadBenchmarkWithName(benchmarkName, "unit-benchmarks");
    }

    private List<Benchmark> loadBenchmarkWithName(String benchmarkName, String benchmarksDir)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "benchmarksDir", benchmarksDir);
        ReflectionTestUtils.setField(benchmarkProperties, "activeBenchmarks", benchmarkName);

        return benchmarkLoader.loadBenchmarks("sequenceId");
    }

    private MapAssert<String, String> assertThatBenchmarkWithEntries(List<Benchmark> benchmarks, MapEntry<String, String>... entries)
    {
        Benchmark searchBenchmark = benchmarks.stream()
                .filter(benchmark -> {
                    boolean containsAllEntries = true;
                    for (MapEntry mapEntry : entries) {
                        Object value = benchmark.getNonReservedKeywordVariables().get(mapEntry.key);
                        if (!mapEntry.value.equals(value)) {
                            containsAllEntries = false;
                            break;
                        }
                    }
                    return containsAllEntries;
                })
                .findFirst().get();

        return assertThat(searchBenchmark.getNonReservedKeywordVariables());
    }
}
