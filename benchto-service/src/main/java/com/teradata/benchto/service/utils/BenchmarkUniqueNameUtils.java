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
package com.teradata.benchto.service.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.List;
import java.util.Map;

public final class BenchmarkUniqueNameUtils
{

    public static String generateBenchmarkUniqueName(String benchmarkName, Map<String, String> benchmarkVariables)
    {
        StringBuilder generatedName = new StringBuilder(benchmarkName);

        List<String> orderedVariableNames = ImmutableList.copyOf(Ordering.natural().sortedCopy(benchmarkVariables.keySet()));
        for (String variableName : orderedVariableNames) {
            generatedName.append('_');
            generatedName.append(variableName);
            generatedName.append('=');
            generatedName.append(benchmarkVariables.get(variableName));
        }

        // leaves in benchmark name only alphanumerics, underscores and dashes
        return generatedName.toString().replaceAll("[^A-Za-z0-9_=-]", "_");
    }

    private BenchmarkUniqueNameUtils()
    {
    }
}
