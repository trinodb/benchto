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
package io.trino.benchto.driver.macro;

import io.trino.benchto.driver.Benchmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Component
public class MacroServiceImpl
        implements MacroService
{
    @Autowired
    private List<MacroExecutionDriver> macroExecutionDrivers;

    public void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark, Optional<Connection> connection)
    {
        MacroExecutionDriver macroExecutionDriver = macroExecutionDrivers.stream()
                .filter(executionDriver -> executionDriver.canExecuteBenchmarkMacro(macroName))
                .collect(Collectors.collectingAndThen(toList(), matchingExecutionDrivers -> {
                    if (matchingExecutionDrivers.size() > 1) {
                        throw new IllegalStateException(format("More than one execution driver for macro %s - matching drivers %s", macroName, matchingExecutionDrivers));
                    }
                    else if (matchingExecutionDrivers.size() == 0) {
                        throw new IllegalStateException(format("No execution driver for macro %s", macroName));
                    }
                    return matchingExecutionDrivers.get(0);
                }));

        macroExecutionDriver.runBenchmarkMacro(macroName, benchmark, connection);
    }
}
