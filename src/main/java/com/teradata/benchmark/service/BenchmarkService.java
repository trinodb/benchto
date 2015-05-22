/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service;

import com.teradata.benchmark.service.model.Benchmark;
import com.teradata.benchmark.service.model.BenchmarkRun;
import com.teradata.benchmark.service.model.BenchmarkRunExecution;
import com.teradata.benchmark.service.model.Measurement;
import com.teradata.benchmark.service.repo.BenchmarkRunRepo;
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
    private BenchmarkRunRepo benchmarkRunRepo;

    @Transactional
    public void startBenchmarkRun(String benchmarkName, String sequenceId)
    {
        BenchmarkRun benchmarkRun = benchmarkRunRepo.findByNameAndSequenceId(benchmarkName, sequenceId);
        if (benchmarkRun == null) {
            benchmarkRun = new BenchmarkRun();
            benchmarkRun.setName(benchmarkName);
            benchmarkRun.setSequenceId(sequenceId);
            benchmarkRunRepo.save(benchmarkRun);
        }
        LOG.debug("Starting benchmark - {}", benchmarkRun);
    }

    @Transactional
    public void finishBenchmarkRun(String benchmarkName, String sequenceId, List<Measurement> measurements)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(benchmarkName, sequenceId);
        for (Measurement measurement : measurements) {
            benchmarkRun.getMeasurements().add(measurement);
        }
        LOG.debug("Finishing benchmark - {}", benchmarkRun);
    }

    @Transactional
    public void startExecution(String benchmarkName, String benchmarkSequenceId, String executionSequenceId)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(benchmarkName, benchmarkSequenceId);

        boolean executionPresent = benchmarkRun.getExecutions().stream()
                .filter(e -> executionSequenceId.equals(e.getSequenceId()))
                .findAny()
                .isPresent();
        if (executionPresent) {
            LOG.debug("Execution ({}) already present for benchmark ({})", executionSequenceId, benchmarkRun);
            return;
        }

        LOG.debug("Starting new execution ({}) for benchmark ({})", executionSequenceId, benchmarkRun);
        BenchmarkRunExecution execution = new BenchmarkRunExecution();
        execution.setSequenceId(executionSequenceId);
        execution.setBenchmarkRun(benchmarkRun);
        benchmarkRun.getExecutions().add(execution);
    }

    @Transactional
    public void finishExecution(String benchmarkName, String benchmarkSequenceId, String executionSequenceId, List<Measurement> measurements)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(benchmarkName, benchmarkSequenceId);
        BenchmarkRunExecution execution = benchmarkRun.getExecutions().stream()
                .filter(e -> executionSequenceId.equals(e.getSequenceId()))
                .findAny().get();

        execution.getMeasurements().addAll(measurements);
    }

    @Transactional(readOnly = true)
    public BenchmarkRun findBenchmarkRun(String benchmarkName, String sequenceId)
    {
        BenchmarkRun benchmarkRun = benchmarkRunRepo.findByNameAndSequenceId(benchmarkName, sequenceId);
        if (benchmarkRun == null) {
            throw new IllegalArgumentException("Could not find benchmark " + benchmarkName + " - " + sequenceId);
        }
        return benchmarkRun;
    }

    @Transactional(readOnly = true)
    public Benchmark findBenchmark(String benchmarkName, Pageable pageable)
    {
        List<BenchmarkRun> benchmarkRuns = benchmarkRunRepo.findByName(benchmarkName, pageable);
        return new Benchmark(benchmarkName, benchmarkRuns);
    }

    @Transactional(readOnly = true)
    public List<BenchmarkRun> findLatest(Pageable pageable)
    {
        return benchmarkRunRepo.findLatest(pageable.getPageNumber(), pageable.getPageSize());
    }
}
