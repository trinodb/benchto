/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service;

import com.teradata.benchmark.service.model.Benchmark;
import com.teradata.benchmark.service.model.Execution;
import com.teradata.benchmark.service.model.Measurement;
import com.teradata.benchmark.service.repo.BenchmarkRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BenchmarkService
{
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkService.class);

    @Autowired
    private BenchmarkRepo benchmarkRepo;

    @Transactional
    public void startBenchmark(String benchmarkName, String sequenceId)
    {
        Benchmark benchmark = benchmarkRepo.findByNameAndSequenceId(benchmarkName, sequenceId);
        if (benchmark == null) {
            benchmark = new Benchmark();
            benchmark.setName(benchmarkName);
            benchmark.setSequenceId(sequenceId);
            benchmarkRepo.save(benchmark);
        }
        LOG.debug("Starting benchmark - {}", benchmark);
    }

    @Transactional
    public void finishBenchmark(String benchmarkName, String sequenceId, List<Measurement> measurements)
    {
        Benchmark benchmark = findBenchmark(benchmarkName, sequenceId);
        for (Measurement measurement : measurements) {
            benchmark.getMeasurements().add(measurement);
        }
        LOG.debug("Finishing benchmark - {}", benchmark);
    }

    @Transactional
    public void startExecution(String benchmarkName, String benchmarkSequenceId, String executionSequenceId)
    {
        Benchmark benchmark = findBenchmark(benchmarkName, benchmarkSequenceId);

        boolean executionPresent = benchmark.getExecutions().stream()
                .filter(e -> executionSequenceId.equals(e.getSequenceId()))
                .findAny()
                .isPresent();
        if (executionPresent) {
            LOG.debug("Execution ({}) already present for benchmark ({})", executionSequenceId, benchmark);
            return;
        }

        LOG.debug("Starting new execution ({}) for benchmark ({})", executionSequenceId, benchmark);
        Execution execution = new Execution();
        execution.setSequenceId(executionSequenceId);
        execution.setBenchmark(benchmark);
        benchmark.getExecutions().add(execution);
    }

    @Transactional
    public void finishExecution(String benchmarkName, String benchmarkSequenceId, String executionSequenceId, List<Measurement> measurements)
    {
        Benchmark benchmark = findBenchmark(benchmarkName, benchmarkSequenceId);
        Execution execution = benchmark.getExecutions().stream()
                .filter(e -> executionSequenceId.equals(e.getSequenceId()))
                .findAny().get();

        execution.getMeasurements().addAll(measurements);
    }

    @Transactional(readOnly = true)
    public Benchmark findBenchmark(String benchmarkName, String sequenceId)
    {
        Benchmark benchmark = benchmarkRepo.findByNameAndSequenceId(benchmarkName, sequenceId);
        if (benchmark == null) {
            throw new IllegalArgumentException("Could not find benchmark " + benchmarkName + " - " + sequenceId);
        }
        return benchmark;
    }

    @Transactional(readOnly = true)
    public List<Benchmark> findBenchmarks(String benchmarkName, Pageable pageable)
    {
        return benchmarkRepo.findByName(benchmarkName, pageable);
    }

    @Transactional(readOnly = true)
    public List<Benchmark> findLatest(Pageable pageable)
    {
        return benchmarkRepo.findLatest(pageable.getPageNumber(), pageable.getPageSize());
    }
}
