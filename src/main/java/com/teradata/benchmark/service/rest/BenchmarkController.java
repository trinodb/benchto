/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.rest;

import com.teradata.benchmark.service.BenchmarkService;
import com.teradata.benchmark.service.model.Benchmark;
import com.teradata.benchmark.service.model.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class BenchmarkController
{

    @Autowired
    private BenchmarkService benchmarkService;

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", method = POST)
    public void startBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId)
    {
        benchmarkService.startBenchmark(benchmarkName, benchmarkSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", method = POST)
    public void finishBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @RequestBody List<Measurement> measurements)
    {
        benchmarkService.finishBenchmark(benchmarkName, benchmarkSequenceId, measurements);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", method = POST)
    public void startExecution(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId)
    {
        benchmarkService.startExecution(benchmarkName, benchmarkSequenceId, executionSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", method = POST)
    public void finishExecution(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId,
            @RequestBody List<Measurement> measurements)
    {
        benchmarkService.finishExecution(benchmarkName, benchmarkSequenceId, executionSequenceId, measurements);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}", method = GET)
    public Benchmark findBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId)
    {
        return benchmarkService.findBenchmark(benchmarkName, benchmarkSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}", method = GET)
    public List<Benchmark> findBenchmarks(
            @PathVariable("benchmarkName") String benchmarkName,
            Pageable pageable)
    {
        return benchmarkService.findBenchmarks(benchmarkName, pageable);
    }

    @RequestMapping(value = "/v1/benchmark/latest", method = GET)
    public List<Benchmark> findLatestBenchmarks(Pageable pageable)
    {
        return benchmarkService.findLatest(pageable);
    }
}
