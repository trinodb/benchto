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

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface MacroService
{
    default void runBenchmarkMacros(List<String> macroNames)
    {
        runBenchmarkMacros(macroNames, Optional.empty(), Optional.<Connection>empty());
    }

    default void runBenchmarkMacros(List<String> macroNames, Benchmark benchmark)
    {
        runBenchmarkMacros(macroNames, Optional.of(benchmark), Optional.<Connection>empty());
    }

    default void runBenchmarkMacros(List<String> macroNames, Benchmark benchmark, Connection connection)
    {
        for (String macroName : macroNames) {
            runBenchmarkMacro(macroName, Optional.of(benchmark), Optional.of(connection));
        }
    }

    default void runBenchmarkMacros(List<String> macroNames, Optional<Benchmark> benchmark, Optional<Connection> connection)
    {
        for (String macroName : macroNames) {
            runBenchmarkMacro(macroName, benchmark, connection);
        }
    }

    void runBenchmarkMacro(String macroName, Optional<Benchmark> benchmark, Optional<Connection> connection);
}
