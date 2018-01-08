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
package io.prestodb.benchto.driver;

import com.google.common.collect.ImmutableList;
import io.prestodb.benchto.driver.execution.ExecutionDriver;
import io.prestodb.benchto.driver.macro.MacroService;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.RequestMatcher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
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
        verifyBenchmarkStart("simple_select_benchmark", "simple_select_benchmark_schema=INFORMATION_SCHEMA");
        verifySerialExecution("simple_select_benchmark_schema=INFORMATION_SCHEMA", "simple_select", 1);
        verifySerialExecution("simple_select_benchmark_schema=INFORMATION_SCHEMA", "simple_select", 2);
        verifyBenchmarkFinish("simple_select_benchmark_schema=INFORMATION_SCHEMA", ImmutableList.of());
        verifyComplete();
    }

    @Test
    public void testBenchmark()
    {
        setBenchmark("test_benchmark");
        verifyBenchmarkStart("test_benchmark", "test_benchmark");
        verifySerialExecution("test_benchmark", "test_query", 1);
        verifySerialExecution("test_benchmark", "test_query", 2);
        verifyBenchmarkFinish("test_benchmark", ImmutableList.of());
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

        verifyBenchmarkStart("test_concurrent_benchmark", "test_concurrent_benchmark");
        for (int i = preWarmRuns; i < allRuns; i++) {
            verifyExecutionStarted("test_concurrent_benchmark", i);
            verifyExecutionFinished("test_concurrent_benchmark", i, concurrentQueryMeasurementName);
        }
        verifyGetGraphiteMeasurements();
        verifyBenchmarkFinish("test_concurrent_benchmark", concurrentBenchmarkMeasurementNames);

        verifyComplete(allRuns);
    }

    private void setBenchmark(String s)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "activeBenchmarks", s);
    }

    private void verifyBenchmarkStart(String benchmarkName, String uniqueBenchmarkName)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/generate-unique-names"),
                method(HttpMethod.POST),
                jsonPath("$.[0].name", is(benchmarkName))
        )).andRespond(withSuccess().contentType(APPLICATION_JSON).body("[\"" + uniqueBenchmarkName + "\"]"));

        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/time/current-time-millis"),
                method(HttpMethod.POST))
        ).andRespond(request -> withSuccess().contentType(APPLICATION_JSON).body("" + System.currentTimeMillis()).createResponse(request));

        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + uniqueBenchmarkName + "/BEN_SEQ_ID/start"),
                method(HttpMethod.POST),
                jsonPath("$.name", is(benchmarkName)),
                jsonPath("$.environmentName", is("TEST_ENV"))
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + uniqueBenchmarkName + " started")),
                jsonPath("$.tags", is("benchmark started TEST_ENV")),
                jsonPath("$.data", is(""))
        )).andRespond(withSuccess());
    }

    private void verifyBenchmarkFinish(String uniqueBenchmarkName, List<String> measurementNames)
    {
        restServiceServer.expect(matchAll(
                requestTo("http://benchmark-service:8080/v1/benchmark/" + uniqueBenchmarkName + "/BEN_SEQ_ID/finish"),
                method(HttpMethod.POST),
                jsonPath("$.status", ENDED_STATUS_MATCHER),
                jsonPath("$.measurements.[*].name", containsInAnyOrder(measurementNames.toArray()))
        )).andRespond(withSuccess());

        restServiceServer.expect(matchAll(
                requestTo("http://graphite:18088/events/"),
                method(HttpMethod.POST),
                jsonPath("$.what", is("Benchmark " + uniqueBenchmarkName + " ended")),
                jsonPath("$.tags", is("benchmark ended TEST_ENV")),
                jsonPath("$.data", startsWith("successful"))
        )).andRespond(withSuccess());
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
        verifyComplete(3);
    }

    private void verifyComplete(int runs)
    {
        int expectedMacroCallCount = runs * /* macros per query */ 2
                + /* before, after benchmark */ 2 + 1
                + /* health check, before, after all */ 3;

        executionDriver.execute();

        ArgumentCaptor<String> macroArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(macroService, times(expectedMacroCallCount)).runBenchmarkMacro(macroArgumentCaptor.capture(), any(Optional.class), any(Optional.class));

        ImmutableList.Builder<String> expected = ImmutableList.builder();
        expected.add(
                "no-op-before-all",
                "no-op-health-check",
                "no-op-before-benchmark",
                "test_query_before_benchmark.sql");
        for (int i = 0; i < runs; i++) {
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
