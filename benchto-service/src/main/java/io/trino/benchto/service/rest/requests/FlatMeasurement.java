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
package io.trino.benchto.service.rest.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FlatMeasurement
{
    private final String name;
    private final String unit;
    private final double value;
    private final Map<String, String> attributes;

    @JsonCreator
    public FlatMeasurement(@JsonProperty("name") String name,
            @JsonProperty("unit") String unit,
            @JsonProperty("value") double value,
            @JsonProperty("attributes") Map<String, String> attributes)
    {
        this.name = name;
        this.unit = unit;
        this.value = value;
        this.attributes = attributes;
    }

    public String getName()
    {
        return name;
    }

    public String getUnit()
    {
        return unit;
    }

    public double getValue()
    {
        return value;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }
}
