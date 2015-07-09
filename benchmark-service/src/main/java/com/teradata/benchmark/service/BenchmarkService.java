/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service;

import com.teradata.benchmark.service.model.BenchmarkRun;
import com.teradata.benchmark.service.model.BenchmarkRunExecution;
import com.teradata.benchmark.service.model.Environment;
import com.teradata.benchmark.service.model.Measurement;
import com.teradata.benchmark.service.model.Status;
import com.teradata.benchmark.service.repo.BenchmarkRunRepo;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.teradata.benchmark.service.model.Environment.DEFAULT_ENVIRONMENT_NAME;
import static com.teradata.benchmark.service.model.Status.STARTED;
import static com.teradata.benchmark.service.utils.BenchmarkUniqueNameUtils.generateBenchmarkUniqueName;
import static com.teradata.benchmark.service.utils.TimeUtils.currentDateTime;

@Service
public class BenchmarkService
{
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkService.class);

    @Autowired
    private BenchmarkRunRepo benchmarkRunRepo;

    @Autowired
    private EnvironmentService environmentService;

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class}, maxAttempts = 1)
    @Transactional
    public String startBenchmarkRun(String uniqueName, String name, String sequenceId, Optional<String> environmentName, Map<String, String> variables,
            Map<String, String> attributes)
    {
        String generatedUniqueName = generateBenchmarkUniqueName(name, variables);
        checkArgument(uniqueName.equals(generatedUniqueName), "Passed unique benchmark name (%s) does not match generated one: (%s) - name: %s, variables: %s",
                uniqueName, generatedUniqueName, name, variables);

        BenchmarkRun benchmarkRun = benchmarkRunRepo.findByUniqueNameAndSequenceId(uniqueName, sequenceId);
        if (benchmarkRun == null) {
            Environment environment = environmentService.findEnvironment(environmentName.orElse(DEFAULT_ENVIRONMENT_NAME));
            benchmarkRun = new BenchmarkRun(name, sequenceId, variables, uniqueName);
            benchmarkRun.setStatus(STARTED);
            benchmarkRun.setEnvironment(environment);
            benchmarkRun.getAttributes().putAll(attributes);
            benchmarkRun.setStarted(currentDateTime());
            benchmarkRunRepo.save(benchmarkRun);
        }
        LOG.debug("Starting benchmark - {}", benchmarkRun);

        return benchmarkRun.getUniqueName();
    }

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class}, maxAttempts = 1)
    @Transactional
    public void finishBenchmarkRun(String uniqueName, String sequenceId, Status status, List<Measurement> measurements, Map<String, String> attributes)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(uniqueName, sequenceId);
        benchmarkRun.getMeasurements().addAll(measurements);
        benchmarkRun.getAttributes().putAll(attributes);
        benchmarkRun.setEnded(currentDateTime());
        benchmarkRun.setStatus(status);
        LOG.debug("Finishing benchmark - {}", benchmarkRun);
    }

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class}, maxAttempts = 1)
    @Transactional
    public void startExecution(String uniqueName, String benchmarkSequenceId, String executionSequenceId, Map<String, String> attributes)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(uniqueName, benchmarkSequenceId);

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
        execution.setStatus(STARTED);
        execution.setStarted(currentDateTime());
        execution.setBenchmarkRun(benchmarkRun);
        execution.getAttributes().putAll(attributes);
        benchmarkRun.getExecutions().add(execution);
    }

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class}, maxAttempts = 1)
    @Transactional
    public void finishExecution(String uniqueName, String benchmarkSequenceId, String executionSequenceId, Status status,
            List<Measurement> measurements, Map<String, String> attributes)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(uniqueName, benchmarkSequenceId);
        BenchmarkRunExecution execution = benchmarkRun.getExecutions().stream()
                .filter(e -> executionSequenceId.equals(e.getSequenceId()))
                .findAny().get();

        execution.getMeasurements().addAll(measurements);
        execution.getAttributes().putAll(attributes);
        execution.setEnded(currentDateTime());
        execution.setStatus(status);
    }

    @Transactional(readOnly = true)
    public BenchmarkRun findBenchmarkRun(String uniqueName, String sequenceId)
    {
        BenchmarkRun benchmarkRun = benchmarkRunRepo.findByUniqueNameAndSequenceId(uniqueName, sequenceId);
        if (benchmarkRun == null) {
            throw new IllegalArgumentException("Could not find benchmark " + uniqueName + " - " + sequenceId);
        }
        Hibernate.initialize(benchmarkRun.getExecutions());
        return benchmarkRun;
    }

    @Transactional(readOnly = true)
    public List<BenchmarkRun> findBenchmark(String uniqueName)
    {
        List<BenchmarkRun> benchmarkRuns = benchmarkRunRepo.findByUniqueNameOrderBySequenceIdDesc(uniqueName);
        for (BenchmarkRun benchmarkRun : benchmarkRuns) {
            Hibernate.initialize(benchmarkRun.getExecutions());
        }
        return benchmarkRuns;
    }

    @Transactional(readOnly = true)
    public List<BenchmarkRun> findLatest()
    {
        return benchmarkRunRepo.findLatest();
    }

    public String generateUniqueBenchmarkName(String name, Map<String, String> variables)
    {
        LOG.debug("Generating unique benchmark name for: name = {}, variables = {}", name, variables);
        return generateBenchmarkUniqueName(name, variables);
    }
}
