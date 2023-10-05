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
import io.trino.benchto.driver.execution.ExecutionDriver;
import io.trino.benchto.driver.listeners.profiler.async.AsyncProfilerProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DriverApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("async-profiler")
public class AsyncQueryProfilerTest
        extends TrinoIntegrationTest
{
    @Autowired
    private ExecutionDriver executionDriver;

    @Autowired
    protected ApplicationContext context;

    @Autowired
    private AsyncProfilerProperties profilerProperties;

    @BeforeClass
    public static void setup()
            throws IOException, InterruptedException
    {
        List<ResourceMapping> resourceMappings = ImmutableList.of(new ResourceMapping("async/amd64/libasyncProfiler.so", "/tmp/libasyncProfiler.so", BindMode.READ_ONLY));
        Network network = Network.newNetwork();
        startBenchtoService(network);
        startTrino(network, resourceMappings);
    }

    @Test
    public void testAsyncQueryProfiler()
            throws IOException, InterruptedException
    {
        setBenchmark("test_benchmark");
        executionDriver.execute();
        verifyBenchmark("test_benchmark", "ENDED");
        Container.ExecResult result = trino.execInContainer("ls", "%s/%s".formatted(profilerProperties.getOutputPath(), "test_benchmark"));
        assertEquals(Arrays.stream(result.getStdout().split("\n")).filter(it -> it.contains(".jfr")).count(), 2);
    }
}
