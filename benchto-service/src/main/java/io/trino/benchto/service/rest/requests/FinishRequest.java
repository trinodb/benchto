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
import io.trino.benchto.service.model.Status;

import javax.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class FinishRequest
{
    @NotNull
    private final Status status;
    private final Instant endTime;
    private final List<FlatMeasurement> measurements;
    private final Map<String, String> attributes;
    private final String queryInfo;

    @JsonCreator
    public FinishRequest(@JsonProperty("status") Status status,
            @JsonProperty("endTime") Instant endTime,
            @JsonProperty("measurements") List<FlatMeasurement> measurements,
            @JsonProperty("attributes") Map<String, String> attributes,
            @JsonProperty("queryInfo") String queryInfo)
    {
        this.status = status;
        this.endTime = endTime;
        this.measurements = measurements;
        this.attributes = attributes;
        this.queryInfo = queryInfo;
    }

    public Status getStatus()
    {
        return status;
    }

    public Instant getEndTime()
    {
        return endTime;
    }

    public List<FlatMeasurement> getMeasurements()
    {
        return measurements;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public String getQueryInfo()
    {
        return queryInfo;
    }
}
