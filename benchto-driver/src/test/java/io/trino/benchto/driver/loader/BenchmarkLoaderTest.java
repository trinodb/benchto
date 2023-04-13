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
package io.trino.benchto.driver.loader;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.BenchmarkExecutionException;
import io.trino.benchto.driver.BenchmarkProperties;
import io.trino.benchto.driver.DriverApp;
import io.trino.benchto.driver.Query;
import io.trino.benchto.driver.service.BenchmarkServiceClient;
import org.assertj.core.api.MapAssert;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
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

        withBenchmarksDirs("unit-benchmarks");
        withFrequencyCheckEnabled(true);
    }

    private QueryLoader mockQueryLoader()
    {
        return new QueryLoader()
        {
            @Override
            public Query loadFromFile(String queryName)
            {
                return new Query(queryName, "test query", ImmutableMap.of());
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
        withOverridesPath("unit-overrides/simple-overrides.yaml");
        withActiveBenchmarks("simple-benchmark");

        Benchmark benchmark = assertLoadedBenchmarksCount(1).get(0);
        assertThat(benchmark.getName()).isEqualTo("different-than-filename");
        assertThat(benchmark.getQueries()).extracting("name").containsExactly("q1", "q2", "1", "2");
        assertThat(benchmark.getDataSource()).isEqualTo("foo");
        assertThat(benchmark.getRuns()).isEqualTo(3);
        assertThat(benchmark.getConcurrency()).isEqualTo(1);
        assertThat(benchmark.getBeforeBenchmarkMacros()).isEqualTo(ImmutableList.of("no-op", "no-op2"));
        assertThat(benchmark.getAfterBenchmarkMacros()).isEqualTo(ImmutableList.of("no-op2"));
        assertThat(benchmark.getSuitePrewarmRuns()).isEqualTo(2);

        // variable overridden by profile
        assertThat(benchmark.getVariables().get("to_be_overridden")).isEqualTo("bar");

        // added by profile
        assertThat(benchmark.getVariables().get("additional")).isEqualTo("foo");

        // name is in attributes, so it will be persisted in results
        assertThat(benchmark.getVariables().get("name")).isEqualTo("different-than-filename");
    }

    @Test
    public void shouldLoadBenchmarksFromMultiplePaths()
            throws IOException
    {
        withActiveBenchmarks("test_benchmark,concurrent-benchmark");
        withBenchmarksDirs("benchmarks", "unit-benchmarks");

        List<Benchmark> benchmarks = assertLoadedBenchmarksCount(2);
        Set<String> benchmarkNames = benchmarks.stream()
                .map(Benchmark::getName)
                .collect(toSet());
        assertThat(benchmarkNames).containsExactly("test_benchmark", "concurrent-benchmark");
    }

    @Test
    public void shouldFailDuplicatedBenchmarkInMultiplePaths()
            throws IOException
    {
        loader.setup();

        thrown.expect(BenchmarkExecutionException.class);
        thrown.expectMessage("Benchmark with name \"duplicate_benchmark\" in multiple locations");

        withActiveBenchmarks("duplicate_benchmark");
        withBenchmarksDirs("duplicate_benchmark_dir1", "duplicate_benchmark_dir2");

        loader.loadBenchmarks("sequenceId");
    }

    @Test
    public void shouldFailNestedBenchmarkDirs()
            throws IOException
    {
        loader.setup();

        thrown.expect(BenchmarkExecutionException.class);
        thrown.expectMessage("Benchmark directories contain nested paths");

        withBenchmarksDirs("benchmark_dir", "benchmark_dir/nested");

        loader.loadBenchmarks("sequenceId");
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
        loader.setup();

        thrown.expect(BenchmarkExecutionException.class);
        thrown.expectMessage("Recursive value substitution is not supported, invalid a: ${b}");

        withBenchmarksDirs("unit-benchmarks-invalid");
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
            throws IOException
    {
        withActiveVariables("format=(.rc)|(tx.)");

        assertLoadedBenchmarksCount(4).forEach(benchmark ->
                assertThat(benchmark.getVariables().get("format")).isIn("orc", "txt"));
    }

    @Test
    public void allBenchmarks_load_only_not_executed_within_two_days()
            throws IOException
    {
        Duration executionAge = Duration.ofDays(2);
        withBenchmarkExecutionAge(executionAge);
        withFrequencyCheckEnabled(true);

        assertLoadedBenchmarksCount(6).forEach(benchmark -> {
            Optional<Duration> frequency = benchmark.getFrequency();
            frequency.ifPresent(duration -> assertThat(duration).isLessThanOrEqualTo(executionAge));
        });
    }

    @Test
    public void allBenchmarks_frequency_check_is_disabled()
            throws IOException
    {
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
            throws IOException
    {
        loader.setup();
        List<Benchmark> benchmarks = loader.loadBenchmarks("sequenceId");

        assertThat(benchmarks).hasSize(expected);

        return benchmarks;
    }

    private void withOverridesPath(String overridesPath)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "overridesPath", "src/test/resources/" + overridesPath);
    }

    private void withBenchmarksDirs(String... benchmarksDirs)
    {
        List<String> benchmarkDirsList = Arrays.stream(benchmarksDirs)
                .map(dir -> "src/test/resources/" + dir)
                .collect(toList());
        ReflectionTestUtils.setField(benchmarkProperties, "benchmarksDirs", Joiner.on(',').join(benchmarkDirsList));
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

    private void withBenchmarkExecutionAge(Duration executionAge)
    {
        this.benchmarkExecutionAge = executionAge;
    }
}
