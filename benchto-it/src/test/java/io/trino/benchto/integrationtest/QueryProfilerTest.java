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

import io.trino.benchto.driver.DriverApp;
import io.trino.benchto.driver.execution.ExecutionDriver;
import io.trino.benchto.driver.listeners.profiler.ProfilerProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DriverApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("profiler")
public class QueryProfilerTest
        extends TrinoIntegrationTest
{
    @Autowired
    private ExecutionDriver executionDriver;

    @Autowired
    protected ApplicationContext context;

    @Autowired
    private ProfilerProperties profilerProperties;

    @BeforeClass
    public static void setup() throws IOException, InterruptedException
    {
        Network network = Network.newNetwork();
        startBenchtoService(network);
        startTrino(network);
    }

    @Test
    public void testJFRQueryProfiler() throws IOException, InterruptedException
    {
        setBenchmark("test_benchmark");
        executionDriver.execute();
        verifyBenchmark("test_benchmark", "ENDED");
        Container.ExecResult result = trino.execInContainer("ls", "%s/%s".formatted(profilerProperties.getOutputPath(), "test_benchmark"));
        assertEquals(Arrays.stream(result.getStdout().split("\n")).filter(it -> it.contains(".jfr")).count(), 2);
    }
}
