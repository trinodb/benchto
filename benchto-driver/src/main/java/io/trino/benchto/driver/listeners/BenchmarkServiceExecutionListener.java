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
package io.trino.benchto.driver.listeners;

import com.google.common.collect.ImmutableList;
import com.google.common.math.LongMath;
import io.trino.benchto.driver.Benchmark;
import io.trino.benchto.driver.Measurable;
import io.trino.benchto.driver.execution.BenchmarkExecutionResult;
import io.trino.benchto.driver.execution.QueryExecution;
import io.trino.benchto.driver.execution.QueryExecutionResult;
import io.trino.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import io.trino.benchto.driver.listeners.measurements.PostExecutionMeasurementProvider;
import io.trino.benchto.driver.listeners.queryinfo.QueryInfoProvider;
import io.trino.benchto.driver.service.BenchmarkServiceClient;
import io.trino.benchto.driver.service.BenchmarkServiceClient.BenchmarkStartRequest.BenchmarkStartRequestBuilder;
import io.trino.benchto.driver.service.BenchmarkServiceClient.ExecutionStartRequest;
import io.trino.benchto.driver.service.BenchmarkServiceClient.ExecutionStartRequest.ExecutionStartRequestBuilder;
import io.trino.benchto.driver.service.BenchmarkServiceClient.FinishRequest;
import io.trino.benchto.driver.service.BenchmarkServiceClient.FinishRequest.FinishRequestBuilder;
import io.trino.benchto.driver.service.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static io.trino.benchto.driver.loader.BenchmarkDescriptor.RESERVED_KEYWORDS;
import static io.trino.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.ENDED;
import static io.trino.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.FAILED;
import static io.trino.benchto.driver.utils.ExceptionUtils.stackTraceToString;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Component
public class BenchmarkServiceExecutionListener
        implements BenchmarkExecutionListener
{
    private static final Duration MAX_CLOCK_DRIFT = Duration.of(1, ChronoUnit.SECONDS);

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Autowired
    private BenchmarkServiceClient benchmarkServiceClient;

    @Autowired
    private List<PostExecutionMeasurementProvider> measurementProviders;

    @Autowired(required = false)
    private QueryInfoProvider queryInfoProvider;

    @Override
    public int getOrder()
    {
        return 0;
    }

    @Override
    public Future<?> benchmarkStarted(Benchmark benchmark)
    {
        checkClocksSync();

        return taskExecutor.submit(() -> {
            BenchmarkStartRequestBuilder requestBuilder = new BenchmarkStartRequestBuilder(benchmark.getName())
                    .environmentName(benchmark.getEnvironment());

            for (Map.Entry<String, String> variableEntry : benchmark.getVariables().entrySet()) {
                if (RESERVED_KEYWORDS.contains(variableEntry.getKey())) {
                    requestBuilder.addAttribute(variableEntry.getKey(), variableEntry.getValue());
                }
                else {
                    requestBuilder.addVariable(variableEntry.getKey(), variableEntry.getValue());
                }
            }

            BenchmarkServiceClient.BenchmarkStartRequest request = requestBuilder.build();

            benchmarkServiceClient.startBenchmark(benchmark.getUniqueName(), benchmark.getSequenceId(), request);
        });
    }

    private void checkClocksSync()
    {
        long timeBefore = System.currentTimeMillis();
        long serviceTime = benchmarkServiceClient.getServiceCurrentTime().toEpochMilli();
        long timeAfter = System.currentTimeMillis();

        long driftApproximation = Math.abs(LongMath.mean(timeBefore, timeAfter) - serviceTime);
        long approximationPrecision = timeAfter - LongMath.mean(timeBefore, timeAfter);

        Duration driftLowerBound = Duration.of(driftApproximation - approximationPrecision, ChronoUnit.MILLIS);

        if (driftLowerBound.compareTo(MAX_CLOCK_DRIFT) > 0) {
            throw new RuntimeException(format("Detected driver and service clocks drift of at least %s, assumed sane maximum is %s", driftLowerBound, MAX_CLOCK_DRIFT));
        }
    }

    @Override
    public Future<?> benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        return CompletableFuture.supplyAsync(() -> getMeasurements(benchmarkExecutionResult), taskExecutor::execute)
                .thenCompose(future -> future)
                .thenApply(measurements -> {
                    FinishRequestBuilder builder = new FinishRequestBuilder()
                            .withStatus(benchmarkExecutionResult.isSuccessful() ? ENDED : FAILED)
                            .addMeasurements(measurements);
                    if (benchmarkExecutionResult.getUtcEnd() != null) {
                        builder.withEndTime(benchmarkExecutionResult.getUtcEnd().toInstant());
                    }
                    // Throughput tests have a different query in every execution, but only one, aggregated execution is saved
                    // so don't save statements for them.
                    if (!benchmarkExecutionResult.getBenchmark().isThroughputTest()) {
                        benchmarkExecutionResult.getExecutions().stream()
                                .findFirst()
                                .ifPresent(e -> builder.addAttribute("statement", e.getQueryExecution().getStatement()));
                    }
                    return builder.build();
                })
                .thenAccept(request -> benchmarkServiceClient.finishBenchmark(
                        benchmarkExecutionResult.getBenchmark().getUniqueName(),
                        benchmarkExecutionResult.getBenchmark().getSequenceId(),
                        request));
    }

    @Override
    public Future<?> executionStarted(QueryExecution execution)
    {
        return taskExecutor.submit(() -> {
            ExecutionStartRequest request = new ExecutionStartRequestBuilder()
                    .build();

            benchmarkServiceClient.startExecution(execution.getBenchmark().getUniqueName(), execution.getBenchmark().getSequenceId(), executionSequenceId(execution), request);
        });
    }

    @Override
    public Future<?> executionFinished(QueryExecutionResult executionResult)
    {
        return CompletableFuture.supplyAsync(() -> getMeasurementsWithQueryInfo(executionResult), taskExecutor::execute)
                .thenCompose(future -> future)
                .thenApply(measurements -> buildExecutionFinishedRequest(executionResult, measurements))
                .thenAccept(request -> benchmarkServiceClient.finishExecution(
                        executionResult.getBenchmark().getUniqueName(),
                        executionResult.getBenchmark().getSequenceId(),
                        executionSequenceId(executionResult.getQueryExecution()),
                        request));
    }

    @Override
    public Future<?> concurrencyTestExecutionFinished(List<QueryExecutionResult> executions)
    {
        if (executions.isEmpty()) {
            return completedFuture(emptyList());
        }
        return taskExecutor.submit(() -> {
            FinishRequest finishRequest = new FinishRequestBuilder()
                    .withStatus(ENDED)
                    .withEndTime(
                            executions.stream()
                                    .filter(e -> e.getUtcEnd() != null)
                                    .map(e -> e.getUtcEnd().toInstant())
                                    .max(Comparator.comparing(Instant::toEpochMilli))
                                    .orElseThrow(NoSuchElementException::new))
                    .addMeasurement(Measurement.measurement(
                            "queries_successful",
                            "NONE",
                            executions.stream().filter(QueryExecutionResult::isSuccessful).count()))
                    .addMeasurement(Measurement.measurement(
                            "queries_failed",
                            "NONE",
                            executions.stream().filter(query -> !query.isSuccessful()).count()))
                    .addAttribute(
                            "queries_order",
                            executions.stream()
                                    .map(QueryExecutionResult::getQueryName)
                                    .collect(Collectors.joining(",")))
                    .build();

            benchmarkServiceClient.finishExecution(
                    executions.stream().findFirst().orElseThrow(NoSuchElementException::new).getBenchmark().getUniqueName(),
                    executions.stream().findFirst().orElseThrow(NoSuchElementException::new).getBenchmark().getSequenceId(),
                    executionSequenceId(executions.stream().findFirst().orElseThrow(NoSuchElementException::new).getQueryExecution()),
                    finishRequest);
        });
    }

    private FinishRequest buildExecutionFinishedRequest(QueryExecutionResult executionResult, MeasurementsWithQueryInfo measurementsWithQueryInfo)
    {
        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(executionResult.isSuccessful() ? ENDED : FAILED)
                .withEndTime(executionResult.getUtcEnd().toInstant())
                .addMeasurements(measurementsWithQueryInfo.getMeasurements());
        measurementsWithQueryInfo.getQueryInfo()
                .ifPresent(requestBuilder::addQueryInfo);

        if (executionResult.getPrestoQueryId().isPresent()) {
            requestBuilder.addAttribute("prestoQueryId", executionResult.getPrestoQueryId().get());
        }

        if (!executionResult.isSuccessful()) {
            requestBuilder.addAttribute("failureMessage", executionResult.getFailureCause().getMessage());
            requestBuilder.addAttribute("failureStackTrace", stackTraceToString(executionResult));

            if (executionResult.getFailureCause() instanceof SQLException) {
                requestBuilder.addAttribute("failureSQLErrorCode", "" + ((SQLException) executionResult.getFailureCause()).getErrorCode());
            }
        }
        return requestBuilder.build();
    }

    private CompletableFuture<MeasurementsWithQueryInfo> getMeasurementsWithQueryInfo(Measurable measurable)
    {
        CompletableFuture<List<Measurement>> measurementsFuture = getMeasurements(measurable);
        CompletableFuture<Optional<String>> queryInfoFuture = getQueryInfo(measurable);
        return measurementsFuture.thenCombine(queryInfoFuture, MeasurementsWithQueryInfo::new);
    }

    private CompletableFuture<List<Measurement>> getMeasurements(Measurable measurable)
    {
        List<CompletableFuture<?>> providerFutures = new ArrayList<>();
        List<Measurement> measurementsList = Collections.synchronizedList(new ArrayList<>());
        for (PostExecutionMeasurementProvider measurementProvider : measurementProviders) {
            CompletableFuture<?> future = measurementProvider.loadMeasurements(measurable)
                    .thenAccept(measurementsList::addAll);
            providerFutures.add(future);
        }

        return CompletableFuture.allOf(providerFutures.toArray(new CompletableFuture[0]))
                .thenApply(aVoid -> ImmutableList.copyOf(measurementsList));
    }

    private CompletableFuture<Optional<String>> getQueryInfo(Measurable measurable)
    {
        if (queryInfoProvider == null) {
            return completedFuture(Optional.empty());
        }

        return queryInfoProvider.loadQueryInfo(measurable);
    }

    private String executionSequenceId(QueryExecution execution)
    {
        return Integer.toString(execution.getSequenceId());
    }

    private static class MeasurementsWithQueryInfo
    {
        private final List<Measurement> measurements;
        private final Optional<String> queryInfo;

        private MeasurementsWithQueryInfo(List<Measurement> measurements, Optional<String> queryInfo)
        {
            this.measurements = requireNonNull(measurements, "measurements is null");
            this.queryInfo = requireNonNull(queryInfo, "queryInfo is null");
        }

        public List<Measurement> getMeasurements()
        {
            return measurements;
        }

        public Optional<String> getQueryInfo()
        {
            return queryInfo;
        }
    }
}
