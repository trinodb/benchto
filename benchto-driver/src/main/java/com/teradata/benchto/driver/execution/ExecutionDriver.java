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
package com.teradata.benchto.driver.execution;

import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.BenchmarkProperties;
import com.teradata.benchto.driver.FailedBenchmarkExecutionException;
import com.teradata.benchto.driver.listeners.benchmark.BenchmarkStatusReporter;
import com.teradata.benchto.driver.loader.BenchmarkLoader;
import com.teradata.benchto.driver.macro.MacroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.teradata.benchto.driver.utils.TimeUtils.nowUtc;
import static java.util.stream.Collectors.toList;

@Component
public class ExecutionDriver
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS");
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkExecutionDriver.class);

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private BenchmarkStatusReporter benchmarkStatusReporter;

    @Autowired
    private BenchmarkLoader benchmarkLoader;

    @Autowired
    private BenchmarkExecutionDriver benchmarkExecutionDriver;

    @Autowired
    private MacroService macroService;

    private final ZonedDateTime startTime = nowUtc();

    public void execute()
    {
        List<Benchmark> benchmarks = loadBenchmarks();
        if (benchmarks.isEmpty()) {
            LOG.warn("No benchmarks selected, exiting...");
            return;
        }

        executeBeforeAllMacros();
        try {
            executeBenchmarks(benchmarks);
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

    private List<Benchmark> loadBenchmarks()
    {
        String executionSequenceId = benchmarkExecutionSequenceId();
        LOG.info("Running benchmarks(executionSequenceId={}) with properties: {}", executionSequenceId, properties);

        List<Benchmark> benchmarks = benchmarkLoader.loadBenchmarks(executionSequenceId);
        LOG.info("Loaded {} benchmarks", benchmarks.size());
        return benchmarks;
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
            if (isTimeLimitEnded()) {
                LOG.warn("Time limit for running benchmarks has run out");
                break;
            }

            executeHealthCheck(benchmark);
            benchmarkExecutionResults.add(benchmarkExecutionDriver.execute(benchmark, benchmarkOrdinalNumber++, benchmarks.size()));
            benchmarkStatusReporter.processCompletedFutures();
        }

        List<BenchmarkExecutionResult> failedBenchmarkResults = benchmarkExecutionResults.stream()
                .filter(benchmarkExecutionResult -> !benchmarkExecutionResult.isSuccessful())
                .collect(toList());

        benchmarkStatusReporter.awaitAllFutures(10, TimeUnit.MINUTES);

        if (!failedBenchmarkResults.isEmpty()) {
            throw new FailedBenchmarkExecutionException(failedBenchmarkResults, benchmarkExecutionResults.size());
        }
    }

    private boolean isTimeLimitEnded()
    {
        Optional<Duration> timeLimit = properties.getTimeLimit();
        return timeLimit.isPresent() && timeLimit.get().compareTo(Duration.between(startTime, nowUtc())) < 0;
    }

    private void executeHealthCheck(Benchmark benchmark)
    {
        Optional<List<String>> macros = properties.getHealthCheckMacros();
        if (macros.isPresent()) {
            LOG.info("Running health check macros: {}", macros.get());
            macroService.runBenchmarkMacros(macros.get(), benchmark);
        }
    }
}
