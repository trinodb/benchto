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
package io.trino.benchto.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.stream.Collectors.toMap;
import static org.hibernate.annotations.CacheConcurrencyStrategy.TRANSACTIONAL;

@Entity
@Table(name = "benchmark_runs", uniqueConstraints = @UniqueConstraint(columnNames = {"unique_name", "sequence_id"}))
public class BenchmarkRun
        implements Serializable
{
    @Id
    @SequenceGenerator(name = "benchmark_runs_id_seq",
            sequenceName = "benchmark_runs_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "benchmark_runs_id_seq")
    @Column(name = "id")
    @JsonIgnore
    private long id;

    @Size(min = 1, max = 255)
    @Column(name = "name")
    private String name;

    @Size(min = 1, max = 54)
    @Column(name = "sequence_id")
    private String sequenceId;

    @Size(min = 1, max = 1024)
    @Column(name = "unique_name")
    private String uniqueName;

    @Cache(usage = TRANSACTIONAL)
    @BatchSize(size = 10)
    @ElementCollection(fetch = EAGER)
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "benchmark_runs_variables", joinColumns = @JoinColumn(name = "benchmark_run_id"))
    private Map<String, String> variables = newHashMap();

    @JsonIgnore
    @Column(name = "version")
    @Version
    private Long version;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "benchmarkRun", cascade = CascadeType.ALL)
    private final Set<BenchmarkRunExecution> executions = newHashSet();

    @BatchSize(size = 10)
    @OneToMany(cascade = CascadeType.ALL, fetch = EAGER)
    @JoinTable(name = "benchmark_run_measurements",
            joinColumns = @JoinColumn(name = "benchmark_run_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "measurement_id", referencedColumnName = "id"))
    private final Set<Measurement> measurements = newHashSet();

    @Column(name = "started")
    private ZonedDateTime started;

    @Column(name = "ended")
    private ZonedDateTime ended;

    @NotNull
    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "environment_id")
    private Environment environment;

    @Cache(usage = TRANSACTIONAL)
    @BatchSize(size = 10)
    @ElementCollection(fetch = EAGER)
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "benchmark_runs_attributes", joinColumns = @JoinColumn(name = "benchmark_run_id"))
    private Map<String, String> attributes = newHashMap();

    @Column(name = "executions_mean_duration")
    private double executionsMeanDuration = -1.0;

    @Column(name = "executions_stddev_duration")
    private double executionStdDevDuration = -1.0;

    @Transient
    private Map<String, AggregatedMeasurement> aggregatedMeasurements;

    protected BenchmarkRun()
    {
    }

    public BenchmarkRun(String name, String sequenceId, Map<String, String> variables, String uniqueName)
    {
        this.name = name;
        this.sequenceId = sequenceId;
        this.variables = variables;
        this.uniqueName = uniqueName;
    }

    public long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getSequenceId()
    {
        return sequenceId;
    }

    public String getUniqueName()
    {
        return uniqueName;
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public Long getVersion()
    {
        return version;
    }

    public void setVersion(Long version)
    {
        this.version = version;
    }

    public Set<BenchmarkRunExecution> getExecutions()
    {
        return executions;
    }

    public Set<Measurement> getMeasurements()
    {
        return measurements;
    }

    public ZonedDateTime getStarted()
    {
        return started;
    }

    public void setStarted(ZonedDateTime started)
    {
        this.started = started;
    }

    public ZonedDateTime getEnded()
    {
        return ended;
    }

    public void setEnded(ZonedDateTime ended)
    {
        this.ended = ended;
    }

    public Environment getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(Environment environment)
    {
        this.environment = environment;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes)
    {
        this.attributes = attributes;
    }

    public double getExecutionsMeanDuration()
    {
        return executionsMeanDuration;
    }

    public void setExecutionsMeanDuration(double executionsMeanDuration)
    {
        this.executionsMeanDuration = executionsMeanDuration;
    }

    public double getExecutionStdDevDuration()
    {
        return executionStdDevDuration;
    }

    public void setExecutionStdDevDuration(double executionStdDevDuration)
    {
        this.executionStdDevDuration = executionStdDevDuration;
    }

    public Map<String, AggregatedMeasurement> getAggregatedMeasurements()
    {
        if (aggregatedMeasurements == null && Hibernate.isInitialized(executions)) {
            ListMultimap<Measurement, Double> measurementValues = ArrayListMultimap.create();
            for (BenchmarkRunExecution execution : executions) {
                for (Measurement measurement : execution.getMeasurements()) {
                    measurementValues.put(measurement, measurement.getValue());
                }
            }
            aggregatedMeasurements = measurementValues.asMap().entrySet().stream()
                    .collect(toMap(entry -> entry.getKey().getName(), entry -> AggregatedMeasurement.aggregate(entry.getKey().getUnit(), entry.getValue())));
        }
        return aggregatedMeasurements;
    }

    public void clearAggregatedMeasurements()
    {
        aggregatedMeasurements = null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BenchmarkRun benchmarkRun = (BenchmarkRun) o;
        return Objects.equals(uniqueName, benchmarkRun.uniqueName) &&
                Objects.equals(sequenceId, benchmarkRun.sequenceId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(uniqueName, sequenceId);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("uniqueName", uniqueName)
                .add("variables", variables)
                .add("version", version)
                .add("sequenceId", sequenceId)
                .add("status", status)
                .add("executions", executions)
                .add("attributes", attributes)
                .add("measurements", measurements)
                .add("started", started)
                .add("ended", ended)
                .add("executionsMeanDuration", executionsMeanDuration)
                .add("executionStdDevDuration", executionStdDevDuration)
                .toString();
    }
}
