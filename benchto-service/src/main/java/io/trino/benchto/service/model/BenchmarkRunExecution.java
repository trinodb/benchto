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
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.Maps.newHashMap;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.annotations.CacheConcurrencyStrategy.TRANSACTIONAL;

@Cacheable
@Entity
@Table(name = "executions")
public class BenchmarkRunExecution
        implements Serializable
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

    @Size(min = 1, max = 64)
    @Column(name = "sequence_id")
    private String sequenceId;

    @JsonIgnore
    @Column(name = "version")
    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @NotNull
    @JsonIgnore
    @ManyToOne
    private BenchmarkRun benchmarkRun;

    @BatchSize(size = 10)
    @OneToMany(cascade = CascadeType.ALL, fetch = EAGER)
    @JoinTable(name = "execution_measurements",
            joinColumns = @JoinColumn(name = "execution_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "measurement_id", referencedColumnName = "id"))
    private Set<Measurement> measurements;

    @Column(name = "started")
    private ZonedDateTime started;

    @Column(name = "ended")
    private ZonedDateTime ended;

    @Cache(usage = TRANSACTIONAL)
    @BatchSize(size = 10)
    @ElementCollection(fetch = EAGER)
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "execution_attributes", joinColumns = @JoinColumn(name = "execution_id"))
    private Map<String, String> attributes = newHashMap();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "query_info_id")
    private QueryInfo queryInfo;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "query_completion_event_id")
    private QueryCompletionEvent queryCompletionEvent;

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

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes)
    {
        this.attributes = attributes;
    }

    public QueryInfo getQueryInfo()
    {
        return queryInfo;
    }

    public void setQueryInfo(QueryInfo queryInfo)
    {
        this.queryInfo = queryInfo;
    }

    public QueryCompletionEvent getQueryCompletionEvent()
    {
        return queryCompletionEvent;
    }

    public void setQueryCompletionEvent(QueryCompletionEvent queryCompletionEvent)
    {
        this.queryCompletionEvent = queryCompletionEvent;
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
                .add("status", status)
                .add("version", version)
                .add("measurements", measurements)
                .add("attributes", attributes)
                .add("queryInfo", queryInfo)
                .add("started", started)
                .add("ended", ended)
                .toString();
    }
}
