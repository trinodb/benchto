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
package io.trino.benchto.service;

import com.google.common.collect.ImmutableList;
import io.trino.benchto.service.model.AggregatedMeasurement;
import io.trino.benchto.service.model.BenchmarkRun;
import io.trino.benchto.service.model.BenchmarkRunExecution;
import io.trino.benchto.service.model.Environment;
import io.trino.benchto.service.model.Measurement;
import io.trino.benchto.service.model.MeasurementUnit;
import io.trino.benchto.service.model.Metric;
import io.trino.benchto.service.model.QueryInfo;
import io.trino.benchto.service.model.Status;
import io.trino.benchto.service.repo.BenchmarkRunRepo;
import io.trino.benchto.service.repo.MetricRepo;
import io.trino.benchto.service.rest.requests.FlatMeasurement;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.trino.benchto.service.model.Status.STARTED;
import static io.trino.benchto.service.utils.BenchmarkUniqueNameUtils.generateBenchmarkUniqueName;
import static io.trino.benchto.service.utils.TimeUtils.currentDateTime;
import static java.util.stream.Collectors.groupingBy;

@Service
public class BenchmarkService
{
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkService.class);

    @Autowired
    private BenchmarkRunRepo benchmarkRunRepo;

    @Autowired
    private MetricRepo metricRepo;

    @Autowired
    private EnvironmentService environmentService;

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class})
    @Transactional
    public String startBenchmarkRun(String uniqueName, String name, String sequenceId, Optional<String> environmentName, Map<String, String> variables,
            Map<String, String> attributes)
    {
        String generatedUniqueName = generateBenchmarkUniqueName(name, variables);
        checkArgument(uniqueName.equals(generatedUniqueName), "Passed unique benchmark name (%s) does not match generated one: (%s) - name: %s, variables: %s",
                uniqueName, generatedUniqueName, name, variables);

        BenchmarkRun benchmarkRun = benchmarkRunRepo.findForUpdateByUniqueNameAndSequenceId(uniqueName, sequenceId);
        if (benchmarkRun == null) {
            Environment environment = environmentService.findEnvironment(environmentName.orElse(Environment.DEFAULT_ENVIRONMENT_NAME));
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

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class})
    @Transactional
    public void finishBenchmarkRun(String uniqueName, String sequenceId, Status status, Optional<Instant> endTime, List<FlatMeasurement> measurements, Map<String, String> attributes)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(uniqueName, sequenceId);
        benchmarkRun.getMeasurements().addAll(normalizeMeasurements(measurements));
        benchmarkRun.getAttributes().putAll(attributes);
        benchmarkRun.setEnded(fromInstantOrCurrentDateTime(endTime));
        benchmarkRun.setStatus(status);
        aggregateBenchmarkExecutions(benchmarkRun);
        LOG.debug("Finishing benchmark - {}", benchmarkRun);
    }

    private void aggregateBenchmarkExecutions(BenchmarkRun benchmarkRun)
    {
        benchmarkRun.clearAggregatedMeasurements();
        AggregatedMeasurement durationAggregatedMeasurement = benchmarkRun.getAggregatedMeasurements().get("duration");
        if (durationAggregatedMeasurement != null) {
            benchmarkRun.setExecutionsMeanDuration(durationAggregatedMeasurement.getMean());
            benchmarkRun.setExecutionStdDevDuration(durationAggregatedMeasurement.getStdDev());
        }
    }

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class})
    @Transactional
    public void startExecution(String uniqueName, String benchmarkSequenceId, String executionSequenceId, Map<String, String> attributes)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(uniqueName, benchmarkSequenceId);

        boolean executionPresent = benchmarkRun.getExecutions().stream()
                .anyMatch(e -> executionSequenceId.equals(e.getSequenceId()));
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

    @Retryable(value = {TransientDataAccessException.class, DataIntegrityViolationException.class})
    @Transactional
    public void finishExecution(String uniqueName, String benchmarkSequenceId, String executionSequenceId, Status status,
            Optional<Instant> endTime, List<FlatMeasurement> measurements, Map<String, String> attributes, String queryInfo)
    {
        BenchmarkRun benchmarkRun = findBenchmarkRun(uniqueName, benchmarkSequenceId);

        BenchmarkRunExecution execution = benchmarkRun.getExecutions().stream()
                .filter(e -> executionSequenceId.equals(e.getSequenceId()))
                .findAny().orElseThrow(() -> new IllegalStateException("Execution cannot be found"));

        checkState(execution.getStatus() == STARTED, "Wrong execution status: %s", execution.getStatus());

        execution.getMeasurements().addAll(normalizeMeasurements(measurements));
        execution.getAttributes().putAll(attributes);
        execution.setEnded(fromInstantOrCurrentDateTime(endTime));
        execution.setStatus(status);

        if (queryInfo != null) {
            QueryInfo info = new QueryInfo();
            info.setInfo(queryInfo);
            execution.setQueryInfo(info);
        }

        if (benchmarkRun.getStatus() != STARTED) {
            // Already finished and aggregated so needs re-aggregating.
            aggregateBenchmarkExecutions(benchmarkRun);
        }

        LOG.debug("Finishing execution - {}", execution);
    }

    private List<Measurement> normalizeMeasurements(List<FlatMeasurement> input)
    {
        // use a lookup map to avoid building a complex SQL query that compares attributes list
        Map<Metric, Metric> metrics = new HashMap<>();
        metricRepo.findAll().forEach(metric -> metrics.put(metric, metric));
        Map<Metric, List<FlatMeasurement>> groups = input.stream()
                .collect(groupingBy(flatMeasurement -> new Metric(
                        flatMeasurement.getName(),
                        MeasurementUnit.valueOf(flatMeasurement.getUnit()),
                        flatMeasurement.getAttributes())));
        ImmutableList.Builder<Measurement> result = ImmutableList.builder();
        for (Map.Entry<Metric, List<FlatMeasurement>> entry : groups.entrySet()) {
            Metric metric = metrics.get(entry.getKey());
            if (metric == null) {
                metric = metricRepo.save(entry.getKey());
                metrics.put(metric, metric);
            }
            Metric finalMetric = metric;
            result.addAll(entry.getValue().stream()
                    .map(flatMeasurement -> new Measurement(finalMetric, flatMeasurement.getValue()))
                    .collect(Collectors.toList()));
        }
        return result.build();
    }

    @Transactional
    public BenchmarkRun findBenchmarkRun(String uniqueName, String sequenceId)
    {
        BenchmarkRun benchmarkRun = benchmarkRunRepo.findForUpdateByUniqueNameAndSequenceId(uniqueName, sequenceId);
        if (benchmarkRun == null) {
            throw new IllegalArgumentException("Could not find benchmark " + uniqueName + " - " + sequenceId);
        }
        Hibernate.initialize(benchmarkRun.getExecutions());
        Hibernate.initialize(benchmarkRun.getMeasurements());
        return benchmarkRun;
    }

    @Transactional(readOnly = true)
    public List<BenchmarkRun> findBenchmark(String uniqueName, String environmentName)
    {
        List<BenchmarkRun> benchmarkRuns = benchmarkRunRepo.findByUniqueNameAndEnvironmentOrderBySequenceIdDesc(uniqueName, findEnvironment(environmentName));
        for (BenchmarkRun benchmarkRun : benchmarkRuns) {
            Hibernate.initialize(benchmarkRun.getExecutions());
        }
        return benchmarkRuns;
    }

    private Environment findEnvironment(String environmentName)
    {
        return environmentService.findEnvironment(environmentName);
    }

    @Transactional(readOnly = true)
    public List<BenchmarkRun> findLatest(String environmentName)
    {
        return benchmarkRunRepo.findLatest(findEnvironment(environmentName).getId());
    }

    public String generateUniqueBenchmarkName(String name, Map<String, String> variables)
    {
        LOG.debug("Generating unique benchmark name for: name = {}, variables = {}", name, variables);
        return generateBenchmarkUniqueName(name, variables);
    }

    public Duration getSuccessfulExecutionAge(String uniqueName)
    {
        Timestamp ended = benchmarkRunRepo.findTimeOfLatestSuccessfulExecution(uniqueName);
        if (ended == null) {
            return Duration.ofDays(Integer.MAX_VALUE);
        }
        ZonedDateTime endedAsZDT = ZonedDateTime.of(ended.toLocalDateTime(), ZoneId.systemDefault());
        return Duration.between(endedAsZDT, currentDateTime());
    }

    private ZonedDateTime fromInstantOrCurrentDateTime(Optional<Instant> instant)
    {
        ZonedDateTime currentDateTime = currentDateTime();
        return instant
                .map(i -> i.atZone(currentDateTime.getZone()))
                .orElse(currentDateTime);
    }
}
