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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;
import static javax.persistence.FetchType.EAGER;

@Entity
@Table(name = "measurements")
public class Measurement
        implements Serializable
{
    @Id
    @SequenceGenerator(name = "measurements_id_seq",
            sequenceName = "measurements_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "measurements_id_seq")
    @Column(name = "id")
    @JsonIgnore
    private long id;

    @Size(min = 1, max = 64)
    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private double value;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "unit")
    private MeasurementUnit unit;

    @NotNull
    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "metric_id")
    private Metric metric;

    protected Measurement()
    {
    }

    public Measurement(Metric metric, double value)
    {
        this.metric = metric;
        this.name = metric.getName();
        if (metric.getAttributes() != null && metric.getAttributes().size() != 0) {
            this.name += " {" + metric.getAttributes().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .sorted()
                    .collect(Collectors.joining(",")) + "}";
        }
        this.unit = metric.getUnit();
        this.value = value;
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

    public double getValue()
    {
        return value;
    }

    public void setValue(double value)
    {
        this.value = value;
    }

    public Metric getMetric()
    {
        return metric;
    }

    public void setMetric(Metric metric)
    {
        this.metric = metric;
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
        Measurement that = (Measurement) o;
        return metric.equals(that.metric);
    }

    @Override
    public int hashCode()
    {
        return metric.hashCode();
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("id", id)
                .add("metric", metric)
                .add("value", value)
                .toString();
    }
}
