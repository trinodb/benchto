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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class GenerateBenchmarkNamesRequestItem
{
    @NotNull
    @Size(min = 1, max = 255)
    private final String name;
    private final Map<String, String> variables;

    @JsonCreator
    public GenerateBenchmarkNamesRequestItem(@JsonProperty("name") String name, @JsonProperty("variables") Map<String, String> variables)
    {
        this.name = name;
        this.variables = variables;
    }

    public String getName()
    {
        return name;
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }
}
