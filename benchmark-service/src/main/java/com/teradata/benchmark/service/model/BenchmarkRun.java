/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.Sets.newHashSet;
import static com.teradata.benchmark.service.model.AggregatedMeasurement.aggregate;
import static java.util.stream.Collectors.toMap;

@Entity
@Table(name = "benchmark_runs", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "sequence_id"}))
public class BenchmarkRun
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

    @Column(name = "name")
    private String name;

    @Column(name = "sequence_id")
    private String sequenceId;

    @Column(name = "version")
    @Version
    private Long version;

    @OneToMany(mappedBy = "benchmarkRun", cascade = CascadeType.ALL)
    private Set<BenchmarkRunExecution> executions = newHashSet();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "benchmark_run_measurements",
            joinColumns = @JoinColumn(name = "benchmark_run_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "measurement_id", referencedColumnName = "id"))
    private Set<Measurement> measurements = newHashSet();

    @Column(name = "started")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    private ZonedDateTime started;

    @Column(name = "ended")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    private ZonedDateTime ended;

    @Transient
    private Map<String, AggregatedMeasurement> aggregatedMeasurements;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSequenceId()
    {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId)
    {
        this.sequenceId = sequenceId;
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

    public Map<String, AggregatedMeasurement> getAggregatedMeasurements()
    {
        if (aggregatedMeasurements == null) {
            ListMultimap<Measurement, Double> measurementValues = ArrayListMultimap.create();
            for (BenchmarkRunExecution execution : executions) {
                for (Measurement measurement : execution.getMeasurements()) {
                    measurementValues.put(measurement, measurement.getValue());
                }
            }
            aggregatedMeasurements = measurementValues.asMap().entrySet().stream()
                    .collect(toMap(entry -> entry.getKey().getName(), entry -> aggregate(entry.getKey().getUnit(), entry.getValue())));
        }
        return aggregatedMeasurements;
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
        return Objects.equals(name, benchmarkRun.name) &&
                Objects.equals(sequenceId, benchmarkRun.sequenceId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), name, sequenceId);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("version", version)
                .add("sequenceId", sequenceId)
                .add("executions", executions)
                .add("measurements", measurements)
                .add("started", started)
                .add("ended", ended)
                .toString();
    }
}
