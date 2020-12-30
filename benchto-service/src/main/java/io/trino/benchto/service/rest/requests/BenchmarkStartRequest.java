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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.Map;

public class BenchmarkStartRequest
{
    @NotNull
    @Size(min = 1, max = 64)
    private final String name;
    @NotNull
    @Size(min = 1, max = 64)
    private final String environmentName;
    private final Map<String, String> variables;
    private final Map<String, String> attributes;

    @JsonCreator
    public BenchmarkStartRequest(@JsonProperty("name") String name, @JsonProperty("environmentName") String environmentName,
            @JsonProperty("variables") Map<String, String> variables, @JsonProperty("attributes") Map<String, String> attributes)
    {
        this.name = name;
        this.environmentName = environmentName;
        this.variables = variables;
        this.attributes = attributes;
    }

    public String getName()
    {
        return name;
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }
}
