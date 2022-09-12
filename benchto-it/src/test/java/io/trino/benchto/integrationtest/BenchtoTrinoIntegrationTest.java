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
package io.trino.benchto.integrationtest;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.trino.benchto.driver.BenchmarkProperties;
import io.trino.benchto.driver.DriverApp;
import io.trino.benchto.driver.FailedBenchmarkExecutionException;
import io.trino.benchto.driver.execution.ExecutionDriver;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DriverApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class BenchtoTrinoIntegrationTest
{
    static PostgreSQLContainer<?> postgres;
    static GenericContainer<?> service;
    static GenericContainer<?> trino;

    @Autowired
    private ExecutionDriver executionDriver;

    @Autowired
    private BenchmarkProperties benchmarkProperties;

    @Autowired
    protected ApplicationContext context;

    @BeforeClass
    public static void setup()
    {
        Network network = Network.newNetwork();
        startBenchtoService(network);
        startTrino(network);
    }

    @Test
    public void testBenchmark()
    {
        setBenchmark("test_benchmark");
        executionDriver.execute();
        verifyBenchmark("test_benchmark", "ENDED");
    }

    @Test
    public void testFailure()
    {
        setBenchmark("test_query_failure");
        assertThatThrownBy(() -> executionDriver.execute())
                .isInstanceOf(FailedBenchmarkExecutionException.class)
                .hasMessageContaining("does not exist");
        verifyBenchmark("test_query_failure", "FAILED");
    }

    @Test
    public void testVerifyResults()
    {
        setBenchmark("test_results");
        executionDriver.execute();
        verifyBenchmark("test_results_query=test_results", "test_results", "ENDED");
    }

    @Test
    public void testVerifyResultsFailure()
    {
        setBenchmark("test_results_failure");
        assertThatThrownBy(() -> executionDriver.execute())
                .isInstanceOf(FailedBenchmarkExecutionException.class)
                .hasMessageContaining("ResultComparisonException: Incorrect result at row 4");
        verifyBenchmark("test_results_failure", "FAILED", 0);
    }

    @Test
    public void testVerifyResultsMissing()
    {
        setBenchmark("test_results_missing");
        assertThatThrownBy(() -> executionDriver.execute())
                .isInstanceOf(FailedBenchmarkExecutionException.class)
                .hasMessageContaining("Error opening result file");
        verifyBenchmark("test_results_missing", "FAILED", 0);
    }

    @Test
    public void testVerifyInsertResults()
    {
        setBenchmark("insert_test_results");
        executionDriver.execute();
        verifyBenchmark("insert_test_results_query=insert_test_query", "insert_test_results", "ENDED", 1);
    }

    @Test
    public void testVerifyInsertResultsFailure()
    {
        setBenchmark("insert_test_results_failure");
        assertThatThrownBy(() -> executionDriver.execute())
                .isInstanceOf(FailedBenchmarkExecutionException.class)
                .hasMessageContaining("Incorrect row count, expected 22, got 25");
        verifyBenchmark("insert_test_results_failure_query=insert_test_query", "insert_test_results_failure", "FAILED", 1);
    }

    @Test
    public void testVerifyInsertResultsFailureWithPreWarm()
    {
        setBenchmark("insert_test_results_failure_with_prewarm");
        assertThatThrownBy(() -> executionDriver.execute())
                .isInstanceOf(FailedBenchmarkExecutionException.class)
                .hasMessageContaining("Incorrect row count, expected 22, got 25");
        verifyBenchmark("insert_test_results_failure_with_prewarm_query=insert_test_query", "insert_test_results_failure_with_prewarm", "FAILED", 0);
    }

    @Test
    public void testThroughputTest()
    {
        setBenchmark("test_throughput_test");
        executionDriver.execute();
        verifyBenchmark(
                "test_throughput_test_query=test_results",
                "test_throughput_test",
                "ENDED",
                4,
                document -> {
                    // It checks whether results for throughput test was saved and queries succeeded
                    List<Map<String, Object>> successfulQueries = document.read("$['executions'][*]['measurements'][*][?(@.name==\"queries_successful\")]");
                    return successfulQueries.stream().allMatch(it -> it.get("value").equals(1.0));
                });
    }

    private static void startBenchtoService(Network network)
    {
        postgres = new PostgreSQLContainer<>("postgres:11")
                .withNetwork(network)
                .withNetworkAliases("postgres");
        postgres.start();
        String jdbcUrl = format("jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                "postgres",
                postgres.getExposedPorts().get(0),
                postgres.getDatabaseName(),
                postgres.getUsername(),
                postgres.getPassword());
        service = new GenericContainer<>("trinodev/benchto-service:latest")
                .withNetwork(network)
                .withNetworkAliases("benchto-service")
                .withEnv(ImmutableMap.of("SPRING_DATASOURCE_URL", jdbcUrl))
                .withExposedPorts(8080)
                .waitingFor(new HttpWaitStrategy()
                        .forPort(8080)
                        .forStatusCode(200));
        service.start();
        createEnvironment("TEST_ENV");
        createTag("TEST_TAG", "TEST_ENV");
        System.setProperty("test.service.host", service.getHost());
        System.setProperty("test.service.port", service.getMappedPort(8080).toString());
    }

    private static void startTrino(Network network)
    {
        trino = new GenericContainer<>("trinodb/trino:388")
                .withNetwork(network)
                .withNetworkAliases("trino")
                .withExposedPorts(8080)
                .waitingFor(new HttpWaitStrategy()
                        .forPort(8080)
                        .forPath("/v1/info")
                        .forStatusCode(200)
                        .forResponsePredicate(response -> response.contains("\"starting\":false")));
        trino.start();
        // We sometimes get "No nodes available to run query"
        sleepUninterruptibly(1, TimeUnit.SECONDS);

        System.setProperty("test.trino.host", trino.getHost());
        System.setProperty("test.trino.port", trino.getMappedPort(8080).toString());
    }

    private static void createEnvironment(String environment)
    {
        UriComponents url = UriComponentsBuilder.fromHttpUrl(getServiceUrl()).path("/v1/environment").pathSegment(environment).build();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForLocation(url.toUri(), ImmutableMap.of());
    }

    private static void createTag(String tag, String environment)
    {
        UriComponents url = UriComponentsBuilder.fromHttpUrl(getServiceUrl()).path("/v1/tag").pathSegment(environment).build();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForLocation(url.toUri(), ImmutableMap.of("name", tag));
    }

    private static String getServiceUrl()
    {
        return format("http://%s:%s",
                service.getHost(),
                service.getMappedPort(8080));
    }

    private void setBenchmark(String s)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "activeBenchmarks", s);
        ReflectionTestUtils.setField(benchmarkProperties, "executionSequenceId", s);
    }

    private void verifyBenchmark(String benchmark, String status)
    {
        verifyBenchmark(benchmark, status, 2);
    }

    private void verifyBenchmark(String benchmark, String status, int expectedRuns)
    {
        verifyBenchmark(benchmark, benchmark, status, expectedRuns);
    }

    private void verifyBenchmark(String benchmark, String sequenceId, String status)
    {
        verifyBenchmark(benchmark, sequenceId, status, 2);
    }

    private void verifyBenchmark(String benchmark, String sequenceId, String status, int expectedRuns)
    {
        verifyBenchmark(benchmark, sequenceId, status, expectedRuns, documentContext -> true);
    }

    private void verifyBenchmark(String benchmark, String sequenceId, String status, int expectedRuns, Predicate<DocumentContext> predicate)
    {
        UriComponents url = UriComponentsBuilder.fromHttpUrl(getServiceUrl())
                .path("v1/benchmark")
                .pathSegment(benchmark)
                .pathSegment(sequenceId)
                .build();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url.toUri(), String.class);
        DocumentContext json = JsonPath.parse(response.getBody());
        assertThat(json.read("$.status", String.class)).isEqualTo(status);
        assertThat(json.read(format("$.executions[?(@.status == '%s')]", status), List.class)).hasSize(expectedRuns);
        assertThat(predicate.test(json));
    }
}
