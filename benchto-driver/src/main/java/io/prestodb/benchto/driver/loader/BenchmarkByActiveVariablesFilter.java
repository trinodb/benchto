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
package io.prestodb.benchto.driver.loader;

import com.google.common.collect.ImmutableMap;
import io.prestodb.benchto.driver.Benchmark;
import io.prestodb.benchto.driver.BenchmarkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

class BenchmarkByActiveVariablesFilter
        implements Predicate<Benchmark>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkByActiveVariablesFilter.class);

    private final Map<String, Pattern> variablePatterns;

    public BenchmarkByActiveVariablesFilter(BenchmarkProperties properties)
    {
        Optional<Map<String, String>> activeVariables = requireNonNull(properties, "properties is null").getActiveVariables();
        ImmutableMap.Builder<String, Pattern> builder = ImmutableMap.<String, Pattern>builder();
        if (activeVariables.isPresent()) {
            for (String variableKey : activeVariables.get().keySet()) {
                builder.put(variableKey, Pattern.compile(activeVariables.get().get(variableKey)));
            }
        }
        variablePatterns = builder.build();
    }

    @Override
    public boolean test(Benchmark benchmark)
    {
        Map<String, String> benchmarkVariables = benchmark.getVariables();
        for (String variableKey : variablePatterns.keySet()) {
            if (benchmarkVariables.containsKey(variableKey)) {
                Pattern valuePattern = variablePatterns.get(variableKey);
                String benchmarkVariableValue = benchmarkVariables.get(variableKey);
                if (!valuePattern.matcher(benchmarkVariableValue).matches()) {
                    LOGGER.debug("Benchmark '{}' is EXCLUDED because mismatches on variable '{}', have '{}' does not match to '{}'",
                            benchmark.getName(), variableKey, valuePattern, benchmarkVariableValue);
                    return false;
                }
            }
            else {
                return false;
            }
        }
        return true;
    }
}
