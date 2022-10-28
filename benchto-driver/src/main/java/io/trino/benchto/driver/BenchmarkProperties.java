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
package io.trino.benchto.driver;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import io.trino.benchto.driver.graphite.GraphiteProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;
import static io.trino.benchto.driver.utils.PropertiesUtils.extractPaths;
import static io.trino.benchto.driver.utils.PropertiesUtils.splitProperty;
import static java.util.stream.Collectors.toMap;

@Component
public class BenchmarkProperties
{
    @Value("${sql}")
    private String sqlDirs;

    @Value("${benchmarks}")
    private String benchmarksDirs;

    @Value("${presto.url}")
    private String prestoURL;

    @Value("${presto.username:#{null}}")
    private String prestoUsername;

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Value("${overrides:#{null}}")
    private String overridesPath;

    /**
     * Active benchmarks. If this property is set benchmarks will be filtered by name.
     */
    @Value("${activeBenchmarks:#{null}}")
    private String activeBenchmarks;

    /**
     * Active variables. If this property is set benchmarks will be filtered by their variable content.
     */
    @Value("${activeVariables:#{null}}")
    private String activeVariables;

    /**
     * Execution identifiers. Every value must be unique for all environments. If not set, it will be automatically set based on timestamp.
     */
    @Value("${executionSequenceId:#{null}}")
    private String executionSequenceId;

    @Value("${environment.name}")
    private String environmentName;

    @Value("${macroExecutions.healthCheck:#{null}}")
    private String healthCheckMacros;

    @Value("${macroExecutions.beforeAll:#{null}}")
    private String beforeAllMacros;

    @Value("${macroExecutions.afterAll:#{null}}")
    private String afterAllMacros;

    @Value("${timeLimit:#{null}}")
    private String timeLimit;

    @Value("${frequencyCheckEnabled:true}")
    private String frequencyCheckEnabled;

    @Value("${query-results-dir}")
    private String queryResultsDir;

    @Autowired
    private GraphiteProperties graphiteProperties;

    public List<Path> sqlFilesDirs()
    {
        return extractPaths(sqlDirs);
    }

    public List<Path> benchmarksFilesDirs()
    {
        return extractPaths(benchmarksDirs);
    }

    public String getServiceURL()
    {
        return serviceUrl;
    }

    public String getPrestoURL()
    {
        return prestoURL;
    }

    public Optional<String> getPrestoUsername()
    {
        return Optional.ofNullable(prestoUsername);
    }

    public Optional<Path> getOverridesPath()
    {
        return Optional.ofNullable(overridesPath).map(Paths::get);
    }

    public Optional<List<String>> getExecutionSequenceId()
    {
        return splitProperty(executionSequenceId);
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public Optional<List<String>> getActiveBenchmarks()
    {
        return splitProperty(activeBenchmarks);
    }

    public Optional<Map<String, String>> getActiveVariables()
    {
        Optional<List<String>> variables = splitProperty(activeVariables);
        if (!variables.isPresent()) {
            return Optional.empty();
        }
        Map<String, String> variablesMap = variables.get().stream()
                .map(variable -> {
                    List<String> variablePairList = ImmutableList.copyOf(Splitter.on("=").trimResults().split(variable));
                    checkState(variablePairList.size() == 2,
                            "Incorrect format of variable: '%s', while proper format is 'key=value'", variable);
                    return variablePairList;
                }).collect(toMap(variableList -> variableList.get(0), variableList -> variableList.get(1)));
        return Optional.of(variablesMap);
    }

    public Optional<Duration> getTimeLimit()
    {
        return Optional.ofNullable(timeLimit).map(Duration::parse);
    }

    public Path getQueryResultsDir()
    {
        return Paths.get(queryResultsDir);
    }

    @Override
    public String toString()
    {
        ToStringHelper toStringHelper = toStringHelper(this)
                .add("sqlDirs", sqlDirs)
                .add("benchmarksDirs", benchmarksDirs)
                .add("executionSequenceId", executionSequenceId)
                .add("environmentName", environmentName)
                .add("graphiteProperties", graphiteProperties)
                .add("frequencyCheck", frequencyCheckEnabled)
                .add("queryResultsDir", queryResultsDir);
        addForToStringOptionalField(toStringHelper, "activeBenchmarks", getActiveBenchmarks());
        addForToStringOptionalField(toStringHelper, "activeVariables", getActiveVariables());
        addForToStringOptionalField(toStringHelper, "beforeAllMacros", getBeforeAllMacros());
        addForToStringOptionalField(toStringHelper, "afterAllMacros", getAfterAllMacros());
        addForToStringOptionalField(toStringHelper, "healthCheckMacros", getHealthCheckMacros());
        addForToStringOptionalField(toStringHelper, "timeLimit", getTimeLimit());
        return toStringHelper.toString();
    }

    private void addForToStringOptionalField(ToStringHelper toStringHelper, String fieldName, Optional optionalField)
    {
        optionalField.ifPresent(value -> toStringHelper.add(fieldName, value));
    }

    public Optional<List<String>> getHealthCheckMacros()
    {
        return splitProperty(this.healthCheckMacros);
    }

    public Optional<List<String>> getBeforeAllMacros()
    {
        return splitProperty(beforeAllMacros);
    }

    public Optional<List<String>> getAfterAllMacros()
    {
        return splitProperty(afterAllMacros);
    }

    public boolean isFrequencyCheckEnabled()
    {
        return parseBoolean(frequencyCheckEnabled);
    }

    private boolean parseBoolean(String booleanString)
    {
        if (booleanString.equalsIgnoreCase(Boolean.TRUE.toString())) {
            return true;
        }
        else if (booleanString.equalsIgnoreCase(Boolean.FALSE.toString())) {
            return false;
        }
        else {
            throw new IllegalStateException(String.format("Incorrect boolean value: %s.", this.frequencyCheckEnabled));
        }
    }
}
