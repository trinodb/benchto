/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service;

import com.teradata.benchmark.service.category.IntegrationTest;
import com.teradata.benchmark.service.model.BenchmarkRun;
import com.teradata.benchmark.service.model.BenchmarkRunExecution;
import com.teradata.benchmark.service.model.Environment;
import com.teradata.benchmark.service.repo.BenchmarkRunRepo;
import com.teradata.benchmark.service.repo.EnvironmentRepo;
import com.teradata.benchmark.service.utils.TimeUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.teradata.benchmark.service.model.MeasurementUnit.BYTES;
import static com.teradata.benchmark.service.model.MeasurementUnit.MILLISECONDS;
import static com.teradata.benchmark.service.model.Status.ENDED;
import static com.teradata.benchmark.service.model.Status.FAILED;
import static com.teradata.benchmark.service.utils.TimeUtils.currentDateTime;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Category(IntegrationTest.class)
public class BenchmarkControllerTest
        extends IntegrationTestBase
{

    @Autowired
    private BenchmarkRunRepo benchmarkRunRepo;

    @Autowired
    private EnvironmentRepo environmentRepo;

    @Test
    public void benchmarkStartEndHappyPath()
            throws Exception
    {
        String environmentName = "environmentName";
        String benchmarkName = "benchmarkName";
        String benchmarkSequenceId = "benchmarkSequenceId";
        String executionSequenceId = "executionSequenceId";
        ZonedDateTime testStart = currentDateTime();

        // create environment
        mvc.perform(post("/v1/environment/{environmentName}", environmentName)
                .contentType(APPLICATION_JSON)
                .content("{\"attribute1\": \"value1\", \"attribute2\": \"value2\"}"))
                .andExpect(status().isOk());

        // get environment
        mvc.perform(get("/v1/environment/{environmentName}", environmentName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(environmentName)))
                .andExpect(jsonPath("$.attributes.attribute1", is("value1")))
                .andExpect(jsonPath("$.attributes.attribute2", is("value2")));

        // start benchmark
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", benchmarkName, benchmarkSequenceId)
                .contentType(APPLICATION_JSON)
                .content("{\"environmentName\": \"" + environmentName + "\"}"))
                .andExpect(status().isOk());

        // get benchmark - no measurements, no executions
        mvc.perform(get("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}", benchmarkName, benchmarkSequenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.status", is("STARTED")))
                .andExpect(jsonPath("$.sequenceId", is(benchmarkSequenceId)))
                .andExpect(jsonPath("$.environment.name", is(environmentName)))
                .andExpect(jsonPath("$.measurements", hasSize(0)))
                .andExpect(jsonPath("$.executions", hasSize(0)));

        // start execution
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start",
                benchmarkName, benchmarkSequenceId, executionSequenceId)
                .contentType(APPLICATION_JSON)
                .content("{\"attributes\": {}}"))
                .andExpect(status().isOk());

        // get benchmark - no measurements, single execution without measurements
        mvc.perform(get("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}", benchmarkName, benchmarkSequenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.status", is("STARTED")))
                .andExpect(jsonPath("$.sequenceId", is(benchmarkSequenceId)))
                .andExpect(jsonPath("$.environment.name", is(environmentName)))
                .andExpect(jsonPath("$.measurements", hasSize(0)))
                .andExpect(jsonPath("$.executions", hasSize(1)))
                .andExpect(jsonPath("$.executions[0].status", is("STARTED")))
                .andExpect(jsonPath("$.executions[0].sequenceId", is(executionSequenceId)));

        // finish execution - post execution measurements
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish",
                benchmarkName, benchmarkSequenceId, executionSequenceId)
                .contentType(APPLICATION_JSON)
                .content("{\"measurements\":[{\"name\": \"duration\", \"value\": 12.34, \"unit\": \"MILLISECONDS\"},{\"name\": \"bytes\", \"value\": 56789.0, \"unit\": \"BYTES\"}]," +
                        "\"attributes\":{\"attribute1\": \"value1\"}, \"status\": \"FAILED\"}"))
                .andExpect(status().isOk());

        // finish benchmark - post benchmark measurements
        mvc.perform(post("/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", benchmarkName, benchmarkSequenceId)
                .contentType(APPLICATION_JSON)
                .content("{\"measurements\":[{\"name\": \"meanDuration\", \"value\": 12.34, \"unit\": \"MILLISECONDS\"},{\"name\": \"sumBytes\", \"value\": 56789.0, \"unit\": \"BYTES\"}]," +
                        "\"attributes\":{\"attribute1\": \"value1\"}, \"status\": \"ENDED\"}"))
                .andExpect(status().isOk());

        ZonedDateTime testEnd = currentDateTime();

        // get benchmark runs in given time range - measurements, single execution with measurements
        mvc.perform(get("/v1/benchmark/{benchmarkName}?from={from}&to={to}",
                benchmarkName, testStart.format(ISO_DATE_TIME), currentDateTime().format(ISO_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.runs[0].sequenceId", is(benchmarkSequenceId)))
                .andExpect(jsonPath("$.runs[0].status", is("ENDED")))
                .andExpect(jsonPath("$.runs[0].environment.name", is(environmentName)))
                .andExpect(jsonPath("$.runs[0].attributes.attribute1", is("value1")))
                .andExpect(jsonPath("$.runs[0].measurements", hasSize(2)))
                .andExpect(jsonPath("$.runs[0].measurements[*].name", containsInAnyOrder("meanDuration", "sumBytes")))
                .andExpect(jsonPath("$.runs[0].measurements[*].value", containsInAnyOrder(12.34, 56789.0)))
                .andExpect(jsonPath("$.runs[0].measurements[*].unit", containsInAnyOrder("MILLISECONDS", "BYTES")))
                .andExpect(jsonPath("$.runs[0].executions", hasSize(1)))
                .andExpect(jsonPath("$.runs[0].executions[0].sequenceId", is(executionSequenceId)))
                .andExpect(jsonPath("$.runs[0].executions[0].status", is("FAILED")))
                .andExpect(jsonPath("$.runs[0].executions[0].attributes.attribute1", is("value1")))
                .andExpect(jsonPath("$.runs[0].executions[0].measurements[*].name", containsInAnyOrder("duration", "bytes")))
                .andExpect(jsonPath("$.runs[0].executions[0].measurements[*].value", containsInAnyOrder(12.34, 56789.0)))
                .andExpect(jsonPath("$.runs[0].executions[0].measurements[*].unit", containsInAnyOrder("MILLISECONDS", "BYTES")));

        // check no benchmarks stored before starting test
        mvc.perform(get("/v1/benchmark/{benchmarkName}?from={from}&to={to}",
                benchmarkName, testStart.minusHours(1).format(ISO_DATE_TIME), testStart.format(ISO_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(benchmarkName)))
                .andExpect(jsonPath("$.runs", hasSize(0)));

        // assert database state
        withinTransaction(() -> {
            Environment environment = environmentRepo.findByName(environmentName);
            assertThat(environment).isNotNull();
            assertThat(environment.getName()).isEqualTo(environmentName);
            assertThat(environment.getAttributes().get("attribute1")).isEqualTo("value1");
            assertThat(environment.getAttributes().get("attribute2")).isEqualTo("value2");

            BenchmarkRun benchmarkRun = benchmarkRunRepo.findByNameAndSequenceId(benchmarkName, benchmarkSequenceId);
            assertThat(benchmarkRun).isNotNull();
            assertThat(benchmarkRun.getId()).isGreaterThan(0);
            assertThat(benchmarkRun.getName()).isEqualTo(benchmarkName);
            assertThat(benchmarkRun.getSequenceId()).isEqualTo(benchmarkSequenceId);
            assertThat(benchmarkRun.getStatus()).isEqualTo(ENDED);
            assertThat(benchmarkRun.getMeasurements())
                    .hasSize(2)
                    .extracting("unit").contains(BYTES, MILLISECONDS);
            assertThat(benchmarkRun.getStarted())
                    .isAfter(testStart)
                    .isBefore(testEnd);
            assertThat(benchmarkRun.getEnded())
                    .isAfter(testStart)
                    .isBefore(testEnd);
            assertThat(benchmarkRun.getExecutions())
                    .hasSize(1);

            BenchmarkRunExecution execution = benchmarkRun.getExecutions().iterator().next();
            assertThat(execution.getId()).isGreaterThan(0);
            assertThat(execution.getSequenceId()).isEqualTo(executionSequenceId);
            assertThat(execution.getStatus()).isEqualTo(FAILED);
            assertThat(execution.getMeasurements())
                    .hasSize(2)
                    .extracting("name").contains("duration", "bytes");
            assertThat(execution.getStarted())
                    .isAfter(testStart)
                    .isBefore(testEnd);
            assertThat(execution.getEnded())
                    .isAfter(testStart)
                    .isBefore(testEnd);
        });
    }
}
