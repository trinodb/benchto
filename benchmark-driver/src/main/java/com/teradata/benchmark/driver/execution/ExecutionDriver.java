/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.benchmark.driver.execution;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.FailedBenchmarkExecutionException;
import com.teradata.benchmark.driver.loader.BenchmarkLoader;
import com.teradata.benchmark.driver.macro.MacroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.teradata.benchmark.driver.utils.TimeUtils.nowUtc;
import static java.util.stream.Collectors.toList;

@Component
public class ExecutionDriver
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS");
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkExecutionDriver.class);

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private BenchmarkLoader benchmarkLoader;

    @Autowired
    private BenchmarkExecutionDriver benchmarkExecutionDriver;

    @Autowired
    private MacroService macroService;

    public void execute()
    {
        try {
            executeBeforeAllMacros();
            executeBenchmarks();
        }
        finally {
            try {
                executeAfterAllMacros();
            }
            catch (RuntimeException e) {
                LOG.error("Exception during execution of after-all macros: {}", e);
            }
        }
    }

    private void executeBeforeAllMacros()
    {
        runOptionalMacros(properties.getBeforeAllMacros(), "before all");
    }

    private void executeAfterAllMacros()
    {
        runOptionalMacros(properties.getAfterAllMacros(), "after all");
    }

    private void runOptionalMacros(Optional<List<String>> macros, String kind)
    {
        if (macros.isPresent()) {
            LOG.info("Running {} macros: {}", kind, macros.get());
            macroService.runBenchmarkMacros(macros.get());
        }
    }

    private void executeBenchmarks()
    {
        String executionSequenceId = benchmarkExecutionSequenceId();
        LOG.info("Running benchmarks(executionSequenceId={}) with properties: {}", executionSequenceId, properties);

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks(executionSequenceId);
        LOG.info("Loaded {} benchmarks", benchmarks.size());

        executeBenchmarks(benchmarks);
    }

    private String benchmarkExecutionSequenceId()
    {
        return properties.getExecutionSequenceId().orElse(nowUtc().format(DATE_TIME_FORMATTER));
    }

    private void executeBenchmarks(List<Benchmark> benchmarks)
    {
        List<BenchmarkExecutionResult> benchmarkExecutionResults = newArrayList();
        int benchmarkOrdinalNumber = 1;
        for (Benchmark benchmark : benchmarks) {
            executeHealthCheck();
            benchmarkExecutionResults.add(benchmarkExecutionDriver.execute(benchmark, benchmarkOrdinalNumber++, benchmarks.size()));
        }

        List<BenchmarkExecutionResult> failedBenchmarkResults = benchmarkExecutionResults.stream()
                .filter(benchmarkExecutionResult -> !benchmarkExecutionResult.isSuccessful())
                .collect(toList());

        if (!failedBenchmarkResults.isEmpty()) {
            throw new FailedBenchmarkExecutionException(failedBenchmarkResults);
        }
    }

    private void executeHealthCheck()
    {
        runOptionalMacros(properties.getHealthCheckMacros(), "health check");
    }
}
