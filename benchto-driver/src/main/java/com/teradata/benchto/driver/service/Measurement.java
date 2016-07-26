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
package com.teradata.benchto.driver.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.MoreObjects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkNotNull;

@JsonAutoDetect(fieldVisibility = ANY)
public class Measurement
{
    private String name;
    private String unit;
    private double value;

    public static Measurement measurement(String name, String unit, double value)
    {
        Measurement measurement = new Measurement();
        measurement.name = checkNotNull(name);
        measurement.unit = checkNotNull(unit);
        measurement.value = value;
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

        return Double.compare(that.value, value) == 0 && name.equals(that.name) && unit.equals(that.unit);
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
        return result;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("unit", unit)
                .add("value", value)
                .toString();
    }
}
