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
package io.prestodb.benchto.driver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class BenchmarkPropertiesTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parseActiveVariablesForNull()
    {
        Optional<Map<String, String>> activeVariables = activeVariables(null);
        assertThat(activeVariables).isEmpty();
    }

    @Test
    public void parseActiveVariables()
    {
        Optional<Map<String, String>> activeVariables = activeVariables("ala=kot,tola=pies");
        assertThat(activeVariables).isPresent();
        assertThat(activeVariables.get()).containsOnly(entry("ala", "kot"), entry("tola", "pies"));
    }

    @Test
    public void parseActiveVariablesWithWrongFormat()
    {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Incorrect format of variable: 'ala=kot=pies', while proper format is 'key=value'");
        activeVariables("ala=kot=pies");
    }

    @Test
    public void parseTimeLimit()
    {
        assertThat(benchmarkPropertiesWithTimeLimit(null).getTimeLimit()).isEmpty();
        assertThat(benchmarkPropertiesWithTimeLimit("P1D").getTimeLimit().get()).isEqualTo(Duration.ofDays(1));
    }

    private Optional<Map<String, String>> activeVariables(String activeVariables)
    {
        BenchmarkProperties benchmarkProperties = benchmarkPropertiesWithActiveVariables(activeVariables);

        return benchmarkProperties.getActiveVariables();
    }

    public static BenchmarkProperties benchmarkPropertiesWithActiveVariables(String activeVariables)
    {
        BenchmarkProperties benchmarkProperties = new BenchmarkProperties();
        ReflectionTestUtils.setField(benchmarkProperties, "activeVariables", activeVariables);
        return benchmarkProperties;
    }

    public static BenchmarkProperties benchmarkPropertiesWithTimeLimit(String timeLimit)
    {
        BenchmarkProperties benchmarkProperties = new BenchmarkProperties();
        ReflectionTestUtils.setField(benchmarkProperties, "timeLimit", timeLimit);
        return benchmarkProperties;
    }
}
