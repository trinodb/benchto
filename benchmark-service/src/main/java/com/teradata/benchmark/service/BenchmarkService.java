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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Service
public class BenchmarkService
{
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkService.class);
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");

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
            benchmarkRun.setStarted(currentDateTime());
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
        benchmarkRun.setEnded(currentDateTime());
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
        execution.setStarted(currentDateTime());
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
        execution.setEnded(currentDateTime());
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
    public Benchmark findBenchmark(String benchmarkName, Optional<ZonedDateTime> from, Optional<ZonedDateTime> to, Pageable pageable)
    {
        checkArgument(!(from.isPresent() ^ to.isPresent()), "from ({}) and to ({}) params should be either both set or not set", from, to);
        List<BenchmarkRun> benchmarkRuns;
        if (from.isPresent()) {
            benchmarkRuns = benchmarkRunRepo.findByNameAndStartedInRange(benchmarkName,
                    Date.from(from.get().toInstant()),
                    Date.from(to.get().toInstant()),
                    pageable.getPageNumber(), pageable.getPageSize());
        }
        else {
            benchmarkRuns = benchmarkRunRepo.findByName(benchmarkName, pageable);
        }
        return new Benchmark(benchmarkName, benchmarkRuns);
    }

    @Transactional(readOnly = true)
    public List<BenchmarkRun> findLatest(Pageable pageable)
    {
        return benchmarkRunRepo.findLatest(pageable.getPageNumber(), pageable.getPageSize());
    }

    private ZonedDateTime currentDateTime()
    {
        return ZonedDateTime.now(UTC_ZONE);
    }
}
