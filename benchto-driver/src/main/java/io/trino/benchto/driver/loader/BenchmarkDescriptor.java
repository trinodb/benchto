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

import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.trino.benchto.driver.utils.YamlUtils.asStringList;

/**
 * Wrapper class around benchmark variables map with helper access methods.
 */
public class BenchmarkDescriptor
{
    public static final String NAME_KEY = "name";
    public static final String DATA_SOURCE_KEY = "datasource";
    public static final String QUERY_NAMES_KEY = "query-names";
    public static final String RUNS_KEY = "runs";
    public static final String SUITE_PREWARM_RUNS_KEY = "suite-prewarm-runs";
    public static final String BENCHMARK_PREWARM_RUNS_KEY = "benchmark-prewarm-runs";
    public static final String CONCURRENCY_KEY = "concurrency";
    public static final String BEFORE_BENCHMARK_MACROS_KEY = "before-benchmark";
    public static final String AFTER_BENCHMARK_MACROS_KEY = "after-benchmark";
    public static final String BEFORE_EXECUTION_MACROS_KEY = "before-execution";
    public static final String AFTER_EXECUTION_MACROS_KEY = "after-execution";
    public static final String VARIABLES_KEY = "variables";
    public static final String QUARANTINE_KEY = "quarantine";
    public static final String FREQUENCY_KEY = "frequency";
    public static final String THROUGHPUT_TEST_KEY = "throughput-test";
    public static final String QUERY_RESULTS_KEY = "query-results";

    public static final Set<String> RESERVED_KEYWORDS = ImmutableSet.of(
            NAME_KEY,
            DATA_SOURCE_KEY,
            QUERY_NAMES_KEY,
            RUNS_KEY,
            SUITE_PREWARM_RUNS_KEY,
            BENCHMARK_PREWARM_RUNS_KEY,
            CONCURRENCY_KEY,
            BEFORE_BENCHMARK_MACROS_KEY,
            AFTER_BENCHMARK_MACROS_KEY,
            BEFORE_EXECUTION_MACROS_KEY,
            AFTER_EXECUTION_MACROS_KEY,
            VARIABLES_KEY,
            QUARANTINE_KEY,
            FREQUENCY_KEY,
            THROUGHPUT_TEST_KEY,
            QUERY_RESULTS_KEY);

    private final Map<String, String> variables;

    public BenchmarkDescriptor(Map<String, String> variables)
    {
        this.variables = variables;
        this.variables.putIfAbsent(QUARANTINE_KEY, "false");
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }

    public String getName()
    {
        return variables.get(NAME_KEY);
    }

    public String getDataSource()
    {
        return variables.get(DATA_SOURCE_KEY);
    }

    public List<String> getQueryNames()
    {
        return asStringList(variables.get(QUERY_NAMES_KEY));
    }

    public Optional<Integer> getRuns()
    {
        return getIntegerOptional(RUNS_KEY);
    }

    public Optional<Integer> getSuitePrewarmRuns()
    {
        return getIntegerOptional(SUITE_PREWARM_RUNS_KEY);
    }

    public Optional<Integer> getBenchmarkPrewarmRuns()
    {
        return getIntegerOptional(BENCHMARK_PREWARM_RUNS_KEY);
    }

    public Optional<Integer> getConcurrency()
    {
        return getIntegerOptional(CONCURRENCY_KEY);
    }

    public Optional<Integer> getFrequency()
    {
        return getIntegerOptional(FREQUENCY_KEY);
    }

    public List<String> getBeforeBenchmarkMacros()
    {
        return asStringList(variables.getOrDefault(BEFORE_BENCHMARK_MACROS_KEY, ""));
    }

    public List<String> getAfterBenchmarkMacros()
    {
        return asStringList(variables.getOrDefault(AFTER_BENCHMARK_MACROS_KEY, ""));
    }

    public List<String> getBeforeExecutionMacros()
    {
        return asStringList(variables.getOrDefault(BEFORE_EXECUTION_MACROS_KEY, ""));
    }

    public List<String> getAfterExecutionMacros()
    {
        return asStringList(variables.getOrDefault(AFTER_EXECUTION_MACROS_KEY, ""));
    }

    public boolean getThroughputTest()
    {
        return variables.getOrDefault(THROUGHPUT_TEST_KEY, "false").equalsIgnoreCase("true");
    }

    public Optional<String> getResults()
    {
        return getStringOptional(QUERY_RESULTS_KEY);
    }

    private Optional<Integer> getIntegerOptional(String key)
    {
        if (variables.containsKey(key)) {
            return Optional.of(Integer.valueOf(variables.get(key)));
        }
        return Optional.empty();
    }

    private Optional<String> getStringOptional(String key)
    {
        if (variables.containsKey(key)) {
            return Optional.of(variables.get(key));
        }
        return Optional.empty();
    }
}
