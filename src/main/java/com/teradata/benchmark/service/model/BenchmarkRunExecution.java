/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @JsonIgnore
    @ManyToOne
    private BenchmarkRun benchmarkRun;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "execution_measurements",
            joinColumns = @JoinColumn(name = "execution_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "measurement_id", referencedColumnName = "id"))
    private Set<Measurement> measurements;

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

    public Set<Measurement> getMeasurements()
    {
        return measurements;
    }

    public void setMeasurements(Set<Measurement> measurements)
    {
        this.measurements = measurements;
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
                .add("measurements", measurements)
                .toString();
    }
}
