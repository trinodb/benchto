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
package io.trino.benchto.driver.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Collections;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

@JsonAutoDetect(fieldVisibility = ANY)
public class Measurement
{
    private String name;
    private String unit;
    private double value;

    private Map<String, String> attributes;

    public static Measurement measurement(String name, String unit, double value)
    {
        return measurement(name, unit, value, Collections.emptyMap());
    }

    public static Measurement measurement(String name, String unit, double value, Map<String, String> attributes)
    {
        Measurement measurement = new Measurement();
        measurement.name = requireNonNull(name);
        measurement.unit = requireNonNull(unit);
        measurement.value = value;
        measurement.attributes = attributes;
        return measurement;
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

        return Double.compare(that.value, value) == 0 && name.equals(that.name) && unit.equals(that.unit) && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        result = name.hashCode();
        result = 31 * result + unit.hashCode();
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + attributes.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("name", name)
                .add("unit", unit)
                .add("value", value)
                .add("attributes", attributes)
                .toString();
    }
}
