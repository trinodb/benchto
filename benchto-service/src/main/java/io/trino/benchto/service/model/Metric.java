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
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.Maps.newHashMap;
import static javax.persistence.FetchType.EAGER;
import static org.hibernate.annotations.CacheConcurrencyStrategy.TRANSACTIONAL;

@Entity
@Table(name = "metrics")
public class Metric
        implements Serializable
{
    @Id
    @SequenceGenerator(name = "metrics_id_seq",
            sequenceName = "metrics_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "metrics_id_seq")
    @Column(name = "id")
    @JsonIgnore
    private long id;

    @Size(min = 1, max = 64)
    @Column(name = "name")
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "unit")
    private MeasurementUnit unit;

    @Cache(usage = TRANSACTIONAL)
    @BatchSize(size = 10)
    @ElementCollection(fetch = EAGER)
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "metric_attributes", joinColumns = @JoinColumn(name = "metric_id"))
    private Map<String, String> attributes = newHashMap();

    protected Metric()
    {
    }

    public Metric(String name, MeasurementUnit unit, Map<String, String> attributes)
    {
        this.name = name;
        this.unit = unit;
        this.attributes = attributes;
    }

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

    public MeasurementUnit getUnit()
    {
        return unit;
    }

    public void setUnit(MeasurementUnit unit)
    {
        this.unit = unit;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes)
    {
        this.attributes = attributes;
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
        Metric that = (Metric) o;
        return Objects.equals(name, that.name)
                && unit.equals(that.unit)
                && ((attributes == null && that.attributes == null)
                || (attributes != null && attributes.equals(that.attributes)));
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(name);
        result += 31 * result + unit.hashCode();
        if (attributes != null) {
            result += 31 * result + attributes.hashCode();
        }
        return result;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("unit", unit)
                .add("attributes", attributes)
                .toString();
    }
}
