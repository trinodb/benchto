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

import com.google.common.collect.ImmutableList;
import io.trino.benchto.driver.DriverApp;
import io.trino.benchto.driver.FailedBenchmarkExecutionException;
import io.trino.benchto.driver.execution.ExecutionDriver;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DriverApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class BenchtoTrinoIntegrationTest
        extends TrinoIntegrationTest
{
    @Autowired
    private ExecutionDriver executionDriver;

    @Autowired
    protected ApplicationContext context;

    @BeforeClass
    public static void setup()
            throws IOException, InterruptedException
    {
        Network network = Network.newNetwork();
        startBenchtoService(network);
        startTrino(network, ImmutableList.of());
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
}
