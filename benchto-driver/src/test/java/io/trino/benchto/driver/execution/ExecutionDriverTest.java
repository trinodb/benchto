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

import com.google.common.collect.ImmutableList;
import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.BenchmarkProperties;
import io.trino.benchto.driver.concurrent.ExecutorServiceFactory;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkStatusReporter;
import io.trino.benchto.driver.listeners.benchmark.DefaultBenchmarkExecutionListener;
import io.trino.benchto.driver.loader.BenchmarkLoader;
import io.trino.benchto.driver.macro.MacroService;
import io.trino.benchto.driver.utils.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExecutionDriverTest
{
    @Mock
    BenchmarkExecutionDriver benchmarkExecutionDriver;

    @Mock
    BenchmarkProperties benchmarkProperties;

    @Mock
    MacroService macroService;

    @Mock
    BenchmarkLoader benchmarkLoader;

    @Mock
    BenchmarkStatusReporter benchmarkStatusReporter;

    @InjectMocks
    ExecutionDriver driver;

    @Before
    public void setUp()
    {
        Benchmark benchmark = mock(Benchmark.class);
        when(benchmark.getName()).thenReturn("mock");
        when(benchmark.getConcurrency()).thenReturn(1);

        when(benchmarkLoader.loadBenchmarks(anyString()))
                .thenReturn(ImmutableList.of(benchmark));
        when(benchmarkProperties.getBeforeAllMacros())
                .thenReturn(Optional.of(ImmutableList.of("before-macro")));
        when(benchmarkProperties.getAfterAllMacros())
                .thenReturn(Optional.of(ImmutableList.of("after-macro")));
        when(benchmarkProperties.getHealthCheckMacros())
                .thenReturn(Optional.of(ImmutableList.of("health-check-macro")));
        when(benchmarkProperties.getExecutionSequenceId())
                .thenReturn(Optional.of(List.of("sequence-id")));
        when(benchmarkExecutionDriver.execute(anyList(), anyInt(), anyInt(), any()))
                .thenReturn(successfulBenchmarkExecution());
        when(benchmarkProperties.getTimeLimit())
                .thenReturn(Optional.empty());
        when(benchmarkProperties.isWarmup())
                .thenReturn(false);
    }

    private List<BenchmarkExecutionResult> successfulBenchmarkExecution()
    {
        BenchmarkExecutionResult result = new BenchmarkExecutionResult.BenchmarkExecutionResultBuilder(null)
                .withExecutions(ImmutableList.of())
                .build();
        return List.of(result);
    }

    @Test
    public void finishWhenTimeLimitEnds()
    {
        when(benchmarkProperties.getTimeLimit())
                .thenReturn(Optional.of(Duration.ofMillis(100)));

        sleepOnSecondDuringMacroExecution();

        driver.execute();

        verifyNoMoreInteractions(benchmarkExecutionDriver);
    }

    @Test
    public void benchmarkIsExecutedWhenNoTimeLimitEnds()
    {
        sleepOnSecondDuringMacroExecution();

        driver.execute();

        verify(benchmarkExecutionDriver).execute(anyList(), anyInt(), anyInt(), any());
        verifyNoMoreInteractions(benchmarkExecutionDriver);
    }

    @Test
    public void failOnListenerFailure()
    {
        BenchmarkExecutionListener failingListener = new DefaultBenchmarkExecutionListener()
        {
            @Override
            public Future<?> benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
            {
                throw new IllegalStateException("programmatic listener failure in testFailingListener");
            }
        };

        BenchmarkStatusReporter statusReporter = new BenchmarkStatusReporter(singletonList(failingListener));
        /*
         * Listeners are called by BenchmarkExecutionDriver so we need to provide one.
         * Listeners results final check is invoked by ExecutionDriver, so this is tested here.
         */
        BenchmarkExecutionDriver benchmarkExecutionDriver = new BenchmarkExecutionDriver();
        ReflectionTestUtils.setField(benchmarkExecutionDriver, "macroService", mock(MacroService.class));
        ReflectionTestUtils.setField(benchmarkExecutionDriver, "executorServiceFactory", new ExecutorServiceFactory());
        ReflectionTestUtils.setField(benchmarkExecutionDriver, "executionSynchronizer", mock(ExecutionSynchronizer.class));
        ReflectionTestUtils.setField(benchmarkExecutionDriver, "statusReporter", statusReporter);
        ReflectionTestUtils.setField(benchmarkExecutionDriver, "properties", benchmarkProperties);
        ReflectionTestUtils.setField(driver, "benchmarkExecutionDriver", benchmarkExecutionDriver);
        ReflectionTestUtils.setField(driver, "benchmarkStatusReporter", statusReporter);

        assertThatThrownBy(() -> driver.execute())
                .hasMessageContaining("programmatic listener failure in testFailingListener");
    }

    private void sleepOnSecondDuringMacroExecution()
    {
        doAnswer(invocationOnMock -> {
            TimeUtils.sleep(1, TimeUnit.SECONDS);
            return null;
        }).when(macroService).runBenchmarkMacros(anyList());
    }
}
