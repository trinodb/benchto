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
package io.prestodb.benchto.driver.listeners;

import com.google.common.collect.ImmutableList;
import com.google.common.math.LongMath;
import io.prestodb.benchto.driver.Benchmark;
import io.prestodb.benchto.driver.Measurable;
import io.prestodb.benchto.driver.execution.BenchmarkExecutionResult;
import io.prestodb.benchto.driver.execution.QueryExecution;
import io.prestodb.benchto.driver.execution.QueryExecutionResult;
import io.prestodb.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import io.prestodb.benchto.driver.listeners.measurements.PostExecutionMeasurementProvider;
import io.prestodb.benchto.driver.service.BenchmarkServiceClient;
import io.prestodb.benchto.driver.service.BenchmarkServiceClient.BenchmarkStartRequest.BenchmarkStartRequestBuilder;
import io.prestodb.benchto.driver.service.BenchmarkServiceClient.ExecutionStartRequest;
import io.prestodb.benchto.driver.service.BenchmarkServiceClient.ExecutionStartRequest.ExecutionStartRequestBuilder;
import io.prestodb.benchto.driver.service.BenchmarkServiceClient.FinishRequest;
import io.prestodb.benchto.driver.service.BenchmarkServiceClient.FinishRequest.FinishRequestBuilder;
import io.prestodb.benchto.driver.service.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static io.prestodb.benchto.driver.loader.BenchmarkDescriptor.RESERVED_KEYWORDS;
import static io.prestodb.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.ENDED;
import static io.prestodb.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.FAILED;
import static io.prestodb.benchto.driver.utils.ExceptionUtils.stackTraceToString;
import static java.lang.String.format;

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

        if (driftLowerBound.compareTo(MAX_CLOCK_DRIFT) > 1) {
            throw new RuntimeException(format("Detected driver and service clocks drift of at least %s, assumed sane maximum is %s", driftLowerBound, MAX_CLOCK_DRIFT));
        }
    }

    @Override
    public Future<?> benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        return CompletableFuture.supplyAsync(() -> getMeasurements(benchmarkExecutionResult), taskExecutor::execute)
                .thenCompose(future -> future)
                .thenApply(measurements -> {
                    return new FinishRequestBuilder()
                            .withStatus(benchmarkExecutionResult.isSuccessful() ? ENDED : FAILED)
                            .withEndTime(benchmarkExecutionResult.getUtcEnd().toInstant())
                            .addMeasurements(measurements)
                            .build();
                })
                .thenAccept(request -> {
                    benchmarkServiceClient.finishBenchmark(
                            benchmarkExecutionResult.getBenchmark().getUniqueName(),
                            benchmarkExecutionResult.getBenchmark().getSequenceId(),
                            request);
                });
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
        return CompletableFuture.supplyAsync(() -> getMeasurements(executionResult), taskExecutor::execute)
                .thenCompose(future -> future)
                .thenApply(measurements -> buildExecutionFinishedRequest(executionResult, measurements))
                .thenAccept(request -> {
                    benchmarkServiceClient.finishExecution(
                            executionResult.getBenchmark().getUniqueName(),
                            executionResult.getBenchmark().getSequenceId(),
                            executionSequenceId(executionResult.getQueryExecution()),
                            request);
                });
    }

    private FinishRequest buildExecutionFinishedRequest(QueryExecutionResult executionResult, List<Measurement> measurements)
    {
        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(executionResult.isSuccessful() ? ENDED : FAILED)
                .withEndTime(executionResult.getUtcEnd().toInstant())
                .addMeasurements(measurements);

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

    private CompletableFuture<List<Measurement>> getMeasurements(Measurable measurable)
    {
        List<CompletableFuture<?>> providerFutures = new ArrayList<>();
        List<Measurement> measurementsList = Collections.synchronizedList(new ArrayList<>());
        for (PostExecutionMeasurementProvider measurementProvider : measurementProviders) {
            CompletableFuture<?> future = measurementProvider.loadMeasurements(measurable)
                    .thenAccept(measurementsList::addAll);
            providerFutures.add(future);
        }

        return CompletableFuture.allOf(providerFutures.stream().toArray(CompletableFuture[]::new))
                .thenApply(aVoid -> ImmutableList.copyOf(measurementsList));
    }

    private String executionSequenceId(QueryExecution execution)
    {
        return "" + execution.getRun();
    }
}
