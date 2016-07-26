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
import com.teradata.benchto.driver.service.BenchmarkServiceClient.FinishRequest.FinishRequestBuilder;
import com.teradata.benchto.driver.service.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.teradata.benchto.driver.loader.BenchmarkDescriptor.RESERVED_KEYWORDS;
import static com.teradata.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.ENDED;
import static com.teradata.benchto.driver.service.BenchmarkServiceClient.FinishRequest.Status.FAILED;
import static com.teradata.benchto.driver.utils.ExceptionUtils.stackTraceToString;

@Component
public class BenchmarkServiceExecutionListener
        implements BenchmarkExecutionListener
{

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Autowired
    private BenchmarkServiceClient benchmarkServiceClient;

    @Autowired
    private List<PostExecutionMeasurementProvider> measurementProviders;

    @Override
    public void benchmarkStarted(Benchmark benchmark)
    {
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

        benchmarkServiceClient.startBenchmark(benchmark.getUniqueName(), benchmark.getSequenceId(), requestBuilder.build());
    }

    @Override
    public void benchmarkFinished(BenchmarkExecutionResult benchmarkExecutionResult)
    {
        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(benchmarkExecutionResult.isSuccessful() ? ENDED : FAILED)
                .addMeasurements(getMeasurements(benchmarkExecutionResult));

        benchmarkServiceClient.finishBenchmark(benchmarkExecutionResult.getBenchmark().getUniqueName(), benchmarkExecutionResult.getBenchmark().getSequenceId(), requestBuilder.build());
    }

    @Override
    public void executionStarted(QueryExecution execution)
    {
        ExecutionStartRequest request = new ExecutionStartRequestBuilder()
                .build();

        benchmarkServiceClient.startExecution(execution.getBenchmark().getUniqueName(), execution.getBenchmark().getSequenceId(), executionSequenceId(execution), request);
    }

    @Override
    public void executionFinished(QueryExecutionResult executionResult)
    {
        FinishRequestBuilder requestBuilder = new FinishRequestBuilder()
                .withStatus(executionResult.isSuccessful() ? ENDED : FAILED)
                .addMeasurements(getMeasurements(executionResult));

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

        benchmarkServiceClient.finishExecution(executionResult.getBenchmark().getUniqueName(), executionResult.getBenchmark().getSequenceId(),
                executionSequenceId(executionResult.getQueryExecution()), requestBuilder.build());
    }

    private List<Measurement> getMeasurements(Measurable measurable)
    {
        ImmutableList.Builder<Measurement> measurementsList = ImmutableList.builder();
        for (PostExecutionMeasurementProvider measurementProvider : measurementProviders) {
            measurementsList.addAll(measurementProvider.loadMeasurements(measurable));
        }
        return measurementsList.build();
    }

    private String executionSequenceId(QueryExecution execution)
    {
        return "" + execution.getRun();
    }
}
