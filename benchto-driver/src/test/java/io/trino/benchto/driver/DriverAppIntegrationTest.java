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
package io.trino.benchto.driver;

import com.google.common.collect.ImmutableList;
import io.trino.benchto.driver.execution.ExecutionDriver;
import io.trino.benchto.driver.macro.MacroService;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.RequestMatcher;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class DriverAppIntegrationTest
        extends IntegrationTest
{
    private static final String GRAPHITE_METRICS_RESPONSE = "[" +
            "{\"target\":\"cpu\",\"datapoints\":[[10, 10],[10, 10]]}" +
            ",{\"target\":\"memory\",\"datapoints\":[[10, 10],[10, 10]]}" +
            ",{\"target\":\"network\",\"datapoints\":[[10, 10],[10, 10]]}" +
            ",{\"target\":\"network_total\",\"datapoints\":[[10, 10],[10, 10]]}" +
            "]";
    private static final List<String> GRAPHITE_MEASUREMENT_NAMES = ImmutableList.of(
            "cluster-memory_max", "cluster-memory_mean", "cluster-cpu_max", "cluster-cpu_mean", "cluster-network_max", "cluster-network_mean", "cluster-network_total");

    private static final Matcher<String> ENDED_STATUS_MATCHER = is("ENDED");

    @Autowired
    private ExecutionDriver executionDriver;

    @Autowired
    private BenchmarkProperties benchmarkProperties;

    @Autowired
    private MacroService macroService;

    @Test
    public void simpleSelectBenchmark()
    {
        setBenchmark("simple_select_benchmark");
        verifyBenchmarkStart("simple_select_benchmark", ImmutableList.of("simple_select_benchmark_schema=INFORMATION_SCHEMA"));
        verifySerialExecution("simple_select_benchmark_schema=INFORMATION_SCHEMA", "simple_select", 1);
        verifySerialExecution("simple_select_benchmark_schema=INFORMATION_SCHEMA", "simple_select", 2);
        verifyBenchmarkFinish(ImmutableList.of("simple_select_benchmark_schema=INFORMATION_SCHEMA"), ImmutableList.of());
        verifyComplete();
    }

    @Test
    public void testBenchmark()
    {
        setBenchmark("test_benchmark");
        verifyBenchmarkStart("test_benchmark", ImmutableList.of("test_benchmark"));
        verifySerialExecution("test_benchmark", "test_query", 1);
        verifySerialExecution("test_benchmark", "test_query", 2);
        verifyBenchmarkFinish(ImmutableList.of("test_benchmark"), ImmutableList.of());
        verifyComplete();
    }

    @Test
    public void testConcurrentBenchmark()
    {
        ImmutableList<String> concurrentQueryMeasurementName = ImmutableList.of("duration");
        ImmutableList<String> concurrentBenchmarkMeasurementNames = ImmutableList.<String>builder()
                .addAll(GRAPHITE_MEASUREMENT_NAMES)
                .add("throughput")
                .add("duration")
                .build();

        setBenchmark("test_concurrent_benchmark");
        int preWarmRuns = 1;
        int runs = 1000;
        int allRuns = preWarmRuns + runs;

        verifyBenchmarkStart("test_concurrent_benchmark", ImmutableList.of("test_concurrent_benchmark"));
        for (int i = preWarmRuns; i < allRuns; i++) {
            verifyExecutionStarted("test_concurrent_benchmark", i);
            verifyExecutionFinished("test_concurrent_benchmark", i, concurrentQueryMeasurementName);
        }
        verifyGetGraphiteMeasurements();
        verifyBenchmarkFinish(ImmutableList.of("test_concurrent_benchmark"), concurrentBenchmarkMeasurementNames);

        verifyComplete(allRuns, 1, 1, 2);
    }

    @Test
    public void simpleSelectBenchmarkWithQueryRepetitionScopeSuite()
    {
        setBenchmark("benchmark_with_2_queries");
        setQueryRepetitionScope(BenchmarkProperties.QueryRepetitionScope.SUITE);
        List<String> benchmarks = ImmutableList.of("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2");
        verifyBenchmarkStart("benchmark_with_2_queries", benchmarks);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select_2", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select", 2);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select_2", 2);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 2);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 2);
        verifyBenchmarkFinish(benchmarks, ImmutableList.of());
        verifyComplete(2, 2, 2 * 2, 2 * 2);
    }

    @Test
    public void simpleSelectBenchmarkWithQueryRepetitionScopeBenchmark()
    {
        setBenchmark("benchmark_with_2_queries");
        setQueryRepetitionScope(BenchmarkProperties.QueryRepetitionScope.BENCHMARK);
        List<String> benchmarks = ImmutableList.of("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2");
        verifyBenchmarkStart("benchmark_with_2_queries", benchmarks);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select", 2);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select_2", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select_2", 2);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 2);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 1);
        verifySerialExecution("benchmark_with_2_queries_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 2);
        verifyBenchmarkFinish(benchmarks, ImmutableList.of());
        verifyComplete(2, 2, 2 * 2, 2);
    }

    @Test
    public void benchmarkWith2Prewarms()
    {
        setBenchmark("benchmark_with_2_prewarms");
        setQueryRepetitionScope(BenchmarkProperties.QueryRepetitionScope.BENCHMARK);
        List<String> benchmarks = ImmutableList.of("benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select", "benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select_2");
        verifyBenchmarkStart("benchmark_with_2_prewarms", benchmarks);
        verifySerialExecution("benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select", 1);
        verifySerialExecution("benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select", 2);
        verifySerialExecution("benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select", "simple_select", 3);
        verifySerialExecution("benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 1);
        verifySerialExecution("benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 2);
        verifySerialExecution("benchmark_with_2_prewarms_schema=INFORMATION_SCHEMA_query=simple_select_2", "simple_select_2", 3);
        verifyBenchmarkFinish(benchmarks, ImmutableList.of());
        verifyComplete(3, 2, 3, 3);
    }

    private void setBenchmark(String s)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "activeBenchmarks", s);
    }

    private void setQueryRepetitionScope(BenchmarkProperties.QueryRepetitionScope queryRepetitionScope)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "queryRepetitionScope", queryRepetitionScope);
    }

    private void verifyBenchmarkStart(String benchmarkName, List<String> uniqueBenchmarkNames)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/generate-unique-names"),
                method(HttpMethod.POST),
                jsonPath("$.[0].name", is(benchmarkName))
        )).andRespond(withSuccess().contentType(APPLICATION_JSON).body(
                "[%s]".formatted(uniqueBenchmarkNames.stream().map("\"%s\""::formatted).collect(Collectors.joining(",")))));

        List<String> startingUrls = uniqueBenchmarkNames.stream().map("http://benchmark-service:8080/v1/benchmark/%s/BEN_SEQ_ID/start"::formatted).toList();
        for (int i = 0; i < uniqueBenchmarkNames.size(); i++) {
            restServiceServer.expect(matchAll(
                    requestTo("http://benchmark-service:8080/v1/time/current-time-millis"),
                    method(HttpMethod.POST))
            ).andRespond(request -> withSuccess().contentType(APPLICATION_JSON).body("" + System.currentTimeMillis()).createResponse(request));

            restServiceServer.expect(matchAll(
                    requestTo(is(in(startingUrls))),
                    method(HttpMethod.POST),
                    jsonPath("$.name", is(benchmarkName)),
                    jsonPath("$.environmentName", is("TEST_ENV"))
            )).andRespond(withSuccess());

            restServiceServer.expect(matchAll(
                    requestTo("http://graphite:18088/events/"),
                    method(HttpMethod.POST),
                    jsonPath("$.what", is(in(uniqueBenchmarkNames.stream().map("Benchmark %s started"::formatted).toList()))),
                    jsonPath("$.tags", is("benchmark started TEST_ENV")),
                    jsonPath("$.data", is(""))
            )).andRespond(withSuccess());
        }
    }

    private void verifyBenchmarkFinish(List<String> uniqueBenchmarkNames, List<String> measurementNames)
    {
        List<String> finishingUrls = uniqueBenchmarkNames.stream().map("http://benchmark-service:8080/v1/benchmark/%s/BEN_SEQ_ID/finish"::formatted).toList();
        for (int i = 0; i < uniqueBenchmarkNames.size(); i++) {
            restServiceServer.expect(matchAll(
                    requestTo(is(in(finishingUrls))),
                    method(HttpMethod.POST),
                    jsonPath("$.status", ENDED_STATUS_MATCHER),
                    jsonPath("$.measurements.[*].name", containsInAnyOrder(measurementNames.toArray()))
            )).andRespond(withSuccess());

            restServiceServer.expect(matchAll(
                    requestTo("http://graphite:18088/events/"),
                    method(HttpMethod.POST),
                    jsonPath("$.what", is(in(uniqueBenchmarkNames.stream().map("Benchmark %s ended"::formatted).toList()))),
                    jsonPath("$.tags", is("benchmark ended TEST_ENV")),
                    jsonPath("$.data", startsWith("successful"))
            )).andRespond(withSuccess());
        }
    }

    private void verifySerialExecution(String uniqueBenchmarkName, String queryName, int executionNumber)
    {
        ImmutableList<String> serialQueryMeasurementNames = ImmutableList.<String>builder()
                .addAll(GRAPHITE_MEASUREMENT_NAMES)
                .add("duration")
                .build();
        verifySerialExecutionStarted(uniqueBenchmarkName, queryName, executionNumber);
        verifyGetGraphiteMeasurements();
        verifySerialExecutionFinished(uniqueBenchmarkName, queryName, executionNumber, serialQueryMeasurementNames);
    }

    private void verifySerialExecutionStarted(String uniqueBenchmarkName, String queryName, int executionNumber)
    {
        verifyExecutionStarted(uniqueBenchmarkName, executionNumber);

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + uniqueBenchmarkName + ", query " + queryName + " (" + executionNumber + ") started")),
                jsonPath("$.tags", is("execution started TEST_ENV")),
                jsonPath("$.data", is(""))
        )).andRespond(withSuccess());
    }

    private void verifyExecutionStarted(String benchmarkName, int executionNumber)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + benchmarkName + "/BEN_SEQ_ID/execution/" + executionNumber + "/start"),
                method(HttpMethod.POST)
        )).andRespond(withSuccess());
    }

    private void verifySerialExecutionFinished(String uniqueBenchmarkName, String queryName, int executionNumber, List<String> measurementNames)
    {
        verifyExecutionFinished(uniqueBenchmarkName, executionNumber, measurementNames);

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + uniqueBenchmarkName + ", query " + queryName + " (" + executionNumber + ") ended")),
                jsonPath("$.tags", is("execution ended TEST_ENV")),
                jsonPath("$.data", startsWith("duration: "))
        )).andRespond(withSuccess());
    }

    private void verifyExecutionFinished(String uniqueBenchmarkName, int executionNumber, List<String> measurementNames)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + uniqueBenchmarkName + "/BEN_SEQ_ID/execution/" + executionNumber + "/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", ENDED_STATUS_MATCHER),
                jsonPath("$.measurements.[*].name", containsInAnyOrder(measurementNames.toArray()))
        )).andRespond(withSuccess());
    }

    private void verifyGetGraphiteMeasurements()
    {
        restServiceServer.expect(matchAll(
                requestTo(startsWith("http://graphite:18088/render?format=json")),
                requestTo(containsString("&target=alias(TARGET_CPU,'cpu')")),
                requestTo(containsString("&target=alias(TARGET_MEMORY,'memory')")),
                requestTo(containsString("&target=alias(TARGET_NETWORK,'network')")),
                requestTo(containsString("&target=alias(integral(TARGET_NETWORK),'network_total')")),
                method(HttpMethod.GET)
        )).andRespond(withSuccess().contentType(APPLICATION_JSON).body(GRAPHITE_METRICS_RESPONSE));
    }

    private void verifyComplete()
    {
        verifyComplete(3, 1, 1, 2);
    }

    private void verifyComplete(int runs, int benchmarksCount, int numberOfQueriesInTotal, int localWarmup)
    {
        int expectedMacroCallCount = (runs + localWarmup) * /* macros per query */ 2 * numberOfQueriesInTotal /* number of queries */
                + /* before, after benchmark */ 2 + 1
                + /* before, after all */ 2 + /* number of benchmarks * health check */ benchmarksCount;

        executionDriver.execute();

        ArgumentCaptor<String> macroArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(macroService, times(expectedMacroCallCount)).runBenchmarkMacro(macroArgumentCaptor.capture(), any(Optional.class), any(Optional.class));

        ImmutableList.Builder<String> expected = ImmutableList.builder();
        expected.add("no-op-before-all");
        for (int i = 0; i < benchmarksCount; i++) {
            expected.add("no-op-health-check");
        }
        expected.add(
                "no-op-before-benchmark",
                "test_query_before_benchmark.sql");
        for (int i = 0; i < (runs + localWarmup) * numberOfQueriesInTotal; i++) {
            expected.add(
                    "no-op-before-execution",
                    "no-op-after-execution");
        }
        expected.add(
                "no-op-after-benchmark",
                "no-op-after-all");

        assertThat(macroArgumentCaptor.getAllValues())
                .isEqualTo(expected.build());
    }

    private RequestMatcher matchAll(RequestMatcher... matchers)
    {
        return request -> {
            for (RequestMatcher matcher : matchers) {
                matcher.match(request);
            }
        };
    }
}
