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
package com.teradata.benchto.driver.listeners;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.teradata.benchto.driver.Benchmark;
import com.teradata.benchto.driver.Measurable;
import com.teradata.benchto.driver.execution.BenchmarkExecutionResult;
import com.teradata.benchto.driver.execution.QueryExecution;
import com.teradata.benchto.driver.execution.QueryExecutionResult;
import com.teradata.benchto.driver.listeners.benchmark.BenchmarkExecutionListener;
import com.teradata.benchto.driver.listeners.measurements.PostExecutionMeasurementProvider;
import com.teradata.benchto.driver.service.BenchmarkServiceClient;
import com.teradata.benchto.driver.service.BenchmarkServiceClient.BenchmarkStartRequest.BenchmarkStartRequestBuilder;
import com.teradata.benchto.driver.service.BenchmarkServiceClient.ExecutionStartRequest;
import com.teradata.benchto.driver.service.BenchmarkServiceClient.ExecutionStartRequest.ExecutionStartRequestBuilder;
import com.teradata.benchto.driver.service.BenchmarkServiceClient.FinishRequest;
import com.teradata.benchto.driver.service.BenchmarkServiceClient.FinishRequest.FinishRequestBuilder;
import com.teradata.benchto.driver.service.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.teradata.benchto.driver.loader.BenchmarkDescriptor.RESERVED_KEYWORDS;
import static com.teradata.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.ENDED;
import static com.teradata.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.FAILED;
import static com.teradata.benchto.driver.utils.ExceptionUtils.stackTraceToString;

@Component
public class BenchmarkServiceExecutionListener
        implements BenchmarkExecutionListener
{
    @Autowired
    private TaskExecutor taskExecutor;

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
    public void benchmarkStarted(Benchmark benchmark)
    {
        taskExecutor.execute(() -> {
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

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        taskExecutor.execute(() -> {
            getMeasurements(benchmarkExecutionResult)
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
        });
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        taskExecutor.execute(() -> {
            ExecutionStartRequest request = new ExecutionStartRequestBuilder()
                    .build();

            benchmarkServiceClient.startExecution(execution.getBenchmark().getUniqueName(), execution.getBenchmark().getSequenceId(), executionSequenceId(execution), request);
        });
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {
        taskExecutor.execute(() -> {
            getMeasurements(executionResult)
                    .thenApply(measurements -> buildExecutionFinishedRequest(executionResult, measurements))
                    .thenAccept(request -> {
                        benchmarkServiceClient.finishExecution(
                                executionResult.getBenchmark().getUniqueName(),
                                executionResult.getBenchmark().getSequenceId(),
                                executionSequenceId(executionResult.getQueryExecution()),
                                request);
                    });
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
