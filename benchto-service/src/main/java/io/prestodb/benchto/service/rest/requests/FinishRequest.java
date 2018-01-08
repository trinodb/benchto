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
package io.prestodb.benchto.service.rest.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.prestodb.benchto.service.model.Measurement;
import io.prestodb.benchto.service.model.Status;

import javax.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class FinishRequest
{
    @NotNull
    private final Status status;
    private final Instant endTime;
    private final List<Measurement> measurements;
    private final Map<String, String> attributes;

    @JsonCreator
    public FinishRequest(@JsonProperty("status") Status status,
            @JsonProperty("endTime") Instant endTime,
            @JsonProperty("measurements") List<Measurement> measurements,
            @JsonProperty("attributes") Map<String, String> attributes)
    {
        this.status = status;
        this.endTime = endTime;
        this.measurements = measurements;
        this.attributes = attributes;
    }

    public Status getStatus()
    {
        return status;
    }

    public Instant getEndTime()
    {
        return endTime;
    }

    public List<Measurement> getMeasurements()
    {
        return measurements;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }
}
