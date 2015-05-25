/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

@Entity
@Table(name = "executions")
public class BenchmarkRunExecution
{

    @Id
    @SequenceGenerator(name = "executions_id_seq",
            sequenceName = "executions_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "executions_id_seq")
    @Column(name = "id")
    @JsonIgnore
    private long id;

    @Column(name = "sequence_id")
    private String sequenceId;

    @Column(name = "version")
    @Version
    private Long version;

    @JsonIgnore
    @ManyToOne
    private BenchmarkRun benchmarkRun;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "execution_measurements",
            joinColumns = @JoinColumn(name = "execution_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "measurement_id", referencedColumnName = "id"))
    private Set<Measurement> measurements;

    @Column(name = "started")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    private ZonedDateTime started;

    @Column(name = "ended")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    private ZonedDateTime ended;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
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

    public Set<Measurement> getMeasurements()
    {
        return measurements;
    }

    public void setMeasurements(Set<Measurement> measurements)
    {
        this.measurements = measurements;
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

    public BenchmarkRun getBenchmarkRun()
    {
        return benchmarkRun;
    }

    public void setBenchmarkRun(BenchmarkRun benchmarkRun)
    {
        this.benchmarkRun = benchmarkRun;
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
        BenchmarkRunExecution execution = (BenchmarkRunExecution) o;
        return Objects.equals(sequenceId, execution.sequenceId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sequenceId);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("id", id)
                .add("sequenceId", sequenceId)
                .add("version", version)
                .add("measurements", measurements)
                .add("started", started)
                .add("ended", ended)
                .toString();
    }
}
