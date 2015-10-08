/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.execution;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.BenchmarkProperties;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult.BenchmarkExecutionResultBuilder;
import com.teradata.benchto.driver.loader.BenchmarkLoader;
import com.teradata.benchto.driver.macro.MacroService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.teradata.benchto.driver.utils.TimeUtils.sleep;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
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

    @InjectMocks
    ExecutionDriver driver;

    @Before
    public void setUp()
    {
        when(benchmarkLoader.loadBenchmarks(anyString()))
                .thenReturn(ImmutableList.of(mock(Benchmark.class)));
        when(benchmarkProperties.getBeforeAllMacros())
                .thenReturn(Optional.of(ImmutableList.of("before-macro")));
        when(benchmarkProperties.getAfterAllMacros())
                .thenReturn(Optional.of(ImmutableList.of("after-macro")));
        when(benchmarkProperties.getHealthCheckMacros())
                .thenReturn(Optional.of(ImmutableList.of("health-check-macro")));
        when(benchmarkProperties.getExecutionSequenceId())
                .thenReturn(Optional.of("sequence-id"));
        when(benchmarkExecutionDriver.execute(any(Benchmark.class), anyInt(), anyInt()))
                .thenReturn(successfulBenchmarkExecution());
    }

    private BenchmarkExecutionResult successfulBenchmarkExecution() {return new BenchmarkExecutionResultBuilder(null).withExecutions(ImmutableList.of()).build();}

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
        when(benchmarkProperties.getTimeLimit())
                .thenReturn(Optional.empty());

        sleepOnSecondDuringMacroExecution();

        driver.execute();

        verify(benchmarkExecutionDriver).execute(any(Benchmark.class), anyInt(), anyInt());
        verifyNoMoreInteractions(benchmarkExecutionDriver);
    }

    private void sleepOnSecondDuringMacroExecution()
    {
        doAnswer(invocationOnMock -> {
            sleep(1, TimeUnit.SECONDS);
            return null;
        }).when(macroService).runBenchmarkMacros(anyList(), any(Optional.class));
    }
}
