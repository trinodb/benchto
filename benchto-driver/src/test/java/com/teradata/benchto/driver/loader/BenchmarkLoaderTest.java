/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
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
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class BenchmarkLoaderTest
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private BenchmarkProperties benchmarkProperties;

    private BenchmarkLoader loader;

    private Duration benchmarkExecutionAge = Duration.ofDays(Integer.MAX_VALUE);

    @Before
    public void setupBenchmarkLoader()
            throws Exception
    {
        QueryLoader queryLoader = mockQueryLoader();
        benchmarkProperties = new BenchmarkProperties();
        BenchmarkServiceClient benchmarkServiceClient = mockBenchmarkServiceClient();
        Configuration freemarkerConfiguration = new DriverApp().freemarkerConfiguration().createConfiguration();

        loader = new BenchmarkLoader();
        ReflectionTestUtils.setField(loader, "properties", benchmarkProperties);
        ReflectionTestUtils.setField(loader, "queryLoader", queryLoader);
        ReflectionTestUtils.setField(loader, "benchmarkServiceClient", benchmarkServiceClient);
        ReflectionTestUtils.setField(loader, "freemarkerConfiguration", freemarkerConfiguration);

        withBenchmarksDir("unit-benchmarks");
        withFrequencyCheckEnabled(true);
    }

    private QueryLoader mockQueryLoader()
    {
        return new QueryLoader()
        {
            @Override
            public Query loadFromFile(String queryName)
            {
                return new Query(queryName, ImmutableList.of("test query"), ImmutableMap.of());
            }
        };
    }

    private BenchmarkServiceClient mockBenchmarkServiceClient()
    {
        return new BenchmarkServiceClient()
        {
            @Override
            public List<String> generateUniqueBenchmarkNames(List<GenerateUniqueNamesRequestItem> generateUniqueNamesRequestItems)
            {
                return generateUniqueNamesRequestItems.stream()
                        .map(requestItem -> requestItem.getName() + "_" + Joiner.on("_").withKeyValueSeparator("=").join(requestItem.getVariables().entrySet()))
                        .collect(toList());
            }

            @Override
            public List<Duration> getBenchmarkSuccessfulExecutionAges(List<String> benchmarkUniqueNames)
            {
                return benchmarkUniqueNames.stream()
                        .map(benchmark -> benchmarkExecutionAge)
                        .collect(toList());
            }
        };
    }

    @Test
    public void shouldLoadSimpleBenchmark()
            throws IOException
    {
        withActiveBenchmarks("simple-benchmark");

        Benchmark benchmark = assertLoadedBenchmarksCount(1).get(0);
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
        withActiveBenchmarks("concurrent-benchmark");

        Benchmark benchmark = assertLoadedBenchmarksCount(1).get(0);
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
        withActiveBenchmarks("multi-variables-benchmark");

        List<Benchmark> benchmarks = assertLoadedBenchmarksCount(5);

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

        withBenchmarksDir("unit-benchmarks-invalid");
        withActiveBenchmarks("cycle-variables-benchmark");

        loader.loadBenchmarks("sequenceId");
    }

    @Test
    public void quarantineBenchmark_no_quarantine_filtering()
            throws IOException
    {
        withActiveBenchmarks("quarantine-benchmark");

        assertLoadedBenchmarksCount(1);
    }

    @Test
    public void quarantineBenchmark_quarantine_false_filtering()
            throws IOException
    {
        withActiveBenchmarks("quarantine-benchmark");
        withActiveVariables("quarantine=false");

        assertLoadedBenchmarksCount(0);
    }

    @Test
    public void allBenchmarks_no_quarantine_filtering()
            throws IOException
    {
        assertLoadedBenchmarksCount(8);
    }

    @Test
    public void allBenchmarks_quarantine_false_filtering()
            throws IOException
    {
        withActiveVariables("quarantine=false");

        assertLoadedBenchmarksCount(7);
    }

    @Test
    public void getAllBenchmarks_activeVariables_with_regex()
    {
        withActiveVariables("format=(rc)|(tx)");

        assertLoadedBenchmarksCount(4).forEach(benchmark ->
                        assertThat(benchmark.getVariables().get("format")).isIn("orc", "txt")
        );
    }

    @Test
    public void allBenchmarks_load_only_not_executed_within_two_days() {
        Duration executionAge = Duration.ofDays(2);
        withBenchmarkExecutionAge(executionAge);
        withFrequencyCheckEnabled(true);

        assertLoadedBenchmarksCount(6).forEach(benchmark -> {
                    Optional<Duration> frequency = benchmark.getFrequency();
                    if (frequency.isPresent()) {
                        assertThat(frequency.get()).isLessThanOrEqualTo(executionAge);
                    }
                }
        );
    }

    @Test
    public void allBenchmarks_frequency_check_is_disabled() {
        withBenchmarkExecutionAge(Duration.ofDays(2));
        withFrequencyCheckEnabled(false);

        assertLoadedBenchmarksCount(8);
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

    private List<Benchmark> assertLoadedBenchmarksCount(int expected)
    {
        List<Benchmark> benchmarks = loader.loadBenchmarks("sequenceId");

        assertThat(benchmarks).hasSize(expected);

        return benchmarks;
    }

    private void withBenchmarksDir(String benchmarksDir)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "benchmarksDir", benchmarksDir);
    }

    private void withActiveBenchmarks(String benchmarkName)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "activeBenchmarks", benchmarkName);
    }

    private void withActiveVariables(String activeVariables)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "activeVariables", activeVariables);
    }

    private void withFrequencyCheckEnabled(boolean enabled)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "frequencyCheckEnabled", Boolean.toString(enabled));
    }

    private void withBenchmarkExecutionAge(Duration executionAge) {
        this.benchmarkExecutionAge = executionAge;
    }
}
