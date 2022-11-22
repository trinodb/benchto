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
import io.trino.benchto.driver.Query;
import io.trino.benchto.driver.concurrent.ExecutorServiceFactory;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkStatusReporter;
import io.trino.benchto.driver.loader.SqlStatementGenerator;
import io.trino.benchto.driver.macro.MacroService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
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

    @Mock
    SqlStatementGenerator sqlStatementGenerator;

    @Before
    public void setUp()
    {
        when(executorServiceFactory.create(anyInt())).thenReturn(executorService);
        when(benchmarkProperties.isWarmup())
                .thenReturn(false);
        when(sqlStatementGenerator.generateQuerySqlStatement(any(Query.class), anyMap())).thenReturn(List.of("SELECT 1"));
    }

    @Test
    public void successfulRun()
    {
        Benchmark benchmark = mock(Benchmark.class);
        when(benchmark.getRuns()).thenReturn(1);
        when(benchmark.getConcurrency()).thenReturn(1);
        when(benchmark.getQueries()).thenReturn(List.of(new Query("fake-query", "SELECT 1", Map.of())));
        List<BenchmarkExecutionResult> results = driver.execute(List.of(benchmark), 0, 0, Optional.empty());

        results.forEach(result -> {
            assertThat(result.getFailureCauses()).isEmpty();
            assertThat(result.isSuccessful()).isTrue();
        });
    }

    @Test
    public void afterMacroFailureCausesBenchmarkExecutionToFail()
    {
        RuntimeException afterMacroException = new RuntimeException();
        doNothing().doThrow(afterMacroException)
                .when(macroService).runBenchmarkMacros(anyList(), any(Benchmark.class));

        List<BenchmarkExecutionResult> results = driver.execute(List.of(mock(Benchmark.class)), 0, 0, Optional.empty());

        results.forEach(result -> {
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getFailureCauses()).containsExactly(afterMacroException);
        });
    }

    @Test
    public void afterMacroFailureCausesDoNotOverrideBenchmarkExecutionFailure()
    {
        IllegalArgumentException executorServiceException = new IllegalArgumentException();
        doThrow(executorServiceException)
                .when(executorServiceFactory).create(anyInt());

        List<BenchmarkExecutionResult> results = driver.execute(List.of(mock(Benchmark.class)), 0, 0, Optional.empty());

        results.forEach(result -> {
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getFailureCauses()).containsExactly(executorServiceException);
        });
    }
}
