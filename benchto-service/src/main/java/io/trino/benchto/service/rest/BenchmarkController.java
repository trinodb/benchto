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
package io.trino.benchto.service.rest;

import io.trino.benchto.service.BenchmarkService;
import io.trino.benchto.service.model.BenchmarkRun;
import io.trino.benchto.service.rest.requests.BenchmarkStartRequest;
import io.trino.benchto.service.rest.requests.ExecutionStartRequest;
import io.trino.benchto.service.rest.requests.FinishRequest;
import io.trino.benchto.service.rest.requests.GenerateBenchmarkNamesRequestItem;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.trino.benchto.service.utils.CollectionUtils.failSafeEmpty;
import static java.util.stream.Collectors.toList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class BenchmarkController
{
    @Autowired
    private BenchmarkService benchmarkService;

    @RequestMapping(value = "/v1/benchmark/generate-unique-names", method = POST)
    public List<String> generateUniqueBenchmarkNames(@RequestBody List<GenerateBenchmarkNamesRequestItem> generateItems)
    {
        return generateItems.stream()
                .map(requestItem -> benchmarkService.generateUniqueBenchmarkName(requestItem.getName(), requestItem.getVariables()))
                .collect(toList());
    }

    @RequestMapping(value = "/v1/benchmark/get-successful-execution-ages", method = POST)
    public List<Duration> getExecutionAges(@RequestBody List<String> uniqueBenchmarkNames)
    {
        return uniqueBenchmarkNames.stream()
                .map(uniqueName -> benchmarkService.getSuccessfulExecutionAge(uniqueName))
                .collect(toList());
    }

    @RequestMapping(value = "/v1/benchmark/{uniqueName}/{benchmarkSequenceId}/start", method = POST)
    public String startBenchmark(
            @PathVariable("uniqueName") String uniqueName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @RequestBody @Valid BenchmarkStartRequest startRequest)
    {
        return benchmarkService.startBenchmarkRun(uniqueName,
                startRequest.getName(),
                benchmarkSequenceId,
                Optional.ofNullable(startRequest.getEnvironmentName()),
                failSafeEmpty(startRequest.getVariables()),
                failSafeEmpty(startRequest.getAttributes()));
    }

    @RequestMapping(value = "/v1/benchmark/{uniqueName}/{benchmarkSequenceId}/finish", method = POST)
    public void finishBenchmark(
            @PathVariable("uniqueName") String uniqueName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @RequestBody @Valid FinishRequest finishRequest)
    {
        benchmarkService.finishBenchmarkRun(uniqueName,
                benchmarkSequenceId,
                finishRequest.getStatus(),
                Optional.ofNullable(finishRequest.getEndTime()),
                failSafeEmpty(finishRequest.getMeasurements()),
                failSafeEmpty(finishRequest.getAttributes()));
    }

    @RequestMapping(value = "/v1/benchmark/{uniqueName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", method = POST)
    public void startExecution(
            @PathVariable("uniqueName") String uniqueName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId,
            @RequestBody @Valid ExecutionStartRequest startRequest)
    {
        benchmarkService.startExecution(uniqueName,
                benchmarkSequenceId,
                executionSequenceId,
                failSafeEmpty(startRequest.getAttributes()));
    }

    @RequestMapping(value = "/v1/benchmark/{uniqueName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", method = POST)
    public void finishExecution(
            @PathVariable("uniqueName") String uniqueName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId,
            @RequestBody @Valid FinishRequest finishRequest)
    {
        benchmarkService.finishExecution(uniqueName,
                benchmarkSequenceId,
                executionSequenceId,
                finishRequest.getStatus(),
                Optional.ofNullable(finishRequest.getEndTime()),
                failSafeEmpty(finishRequest.getMeasurements()),
                failSafeEmpty(finishRequest.getAttributes()),
                finishRequest.getQueryInfo(),
                finishRequest.getQueryCompletionEvent());
    }

    @RequestMapping(value = "/v1/benchmark/{uniqueName}/{benchmarkSequenceId}", method = GET)
    public BenchmarkRun findBenchmark(
            @PathVariable("uniqueName") String uniqueName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId)
    {
        return benchmarkService.findBenchmarkRun(uniqueName, benchmarkSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{uniqueName}", method = GET)
    public List<BenchmarkRun> findBenchmarks(
            @PathVariable("uniqueName") String uniqueName,
            @RequestParam("environment") String environmentName)
    {
        return benchmarkService.findBenchmark(uniqueName, environmentName);
    }

    @RequestMapping(value = "/v1/benchmark/latest/{environmentName}", method = GET)
    public List<BenchmarkRun> findLatestBenchmarkRuns(
            @PathVariable("environmentName") String environmentName)
    {
        return benchmarkService.findLatest(environmentName);
    }
}
