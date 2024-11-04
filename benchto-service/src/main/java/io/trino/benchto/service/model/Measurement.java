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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

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
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("unit", unit)
                .add("value", value)
                .toString();
    }
}
