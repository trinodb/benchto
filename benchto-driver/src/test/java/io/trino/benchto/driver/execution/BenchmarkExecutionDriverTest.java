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
package io.trino.benchto.driver.execution;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.BenchmarkProperties;
import io.trino.benchto.driver.concurrent.ExecutorServiceFactory;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkStatusReporter;
import io.trino.benchto.driver.macro.MacroService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BenchmarkExecutionDriverTest
{
    @Mock
    MacroService macroService;

    @Mock
    ExecutorServiceFactory executorServiceFactory;

    @Mock
    ListeningExecutorService executorService;

    @Mock
    BenchmarkStatusReporter statusReporter;

    @Mock
    ExecutionSynchronizer executionSynchronizer;

    @Mock
    BenchmarkProperties benchmarkProperties;

    @InjectMocks
    BenchmarkExecutionDriver driver;

    @Before
    public void setUp()
    {
        when(executorServiceFactory.create(anyInt())).thenReturn(executorService);
        when(benchmarkProperties.isWarmup())
                .thenReturn(false);
    }

    @Test
    public void successfulRun()
    {
        BenchmarkExecutionResult benchmarkExecutionResult = driver.execute(mock(Benchmark.class), 0, 0, Optional.empty());

        assertThat(benchmarkExecutionResult.getFailureCauses()).isEmpty();
        assertThat(benchmarkExecutionResult.isSuccessful()).isTrue();
    }

    @Test
    public void afterMacroFailureCausesBenchmarkExecutionToFail()
    {
        RuntimeException afterMacroException = new RuntimeException();
        doNothing().doThrow(afterMacroException)
                .when(macroService).runBenchmarkMacros(anyList(), any(Benchmark.class));

        BenchmarkExecutionResult benchmarkExecutionResult = driver.execute(mock(Benchmark.class), 0, 0, Optional.empty());

        assertThat(benchmarkExecutionResult.isSuccessful()).isFalse();
        assertThat(benchmarkExecutionResult.getFailureCauses()).containsExactly(afterMacroException);
    }

    @Test
    public void afterMacroFailureCausesDoNotOverrideBenchmarkExecutionFailure()
    {
        IllegalArgumentException executorServiceException = new IllegalArgumentException();
        doThrow(executorServiceException)
                .when(executorServiceFactory).create(anyInt());

        BenchmarkExecutionResult benchmarkExecutionResult = driver.execute(mock(Benchmark.class), 0, 0, Optional.empty());

        assertThat(benchmarkExecutionResult.isSuccessful()).isFalse();
        assertThat(benchmarkExecutionResult.getFailureCauses()).containsExactly(executorServiceException);
    }
}
