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
package io.trino.benchto.driver.loader;

import com.google.common.collect.ImmutableMap;
import io.trino.benchto.driver.Benchmark;
import org.junit.Test;

import static io.trino.benchto.driver.BenchmarkPropertiesTest.benchmarkPropertiesWithActiveVariables;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BenchmarkByActiveVariablesFilterTest
{
    @Test
    public void filter()
    {
        BenchmarkByActiveVariablesFilter filter = new BenchmarkByActiveVariablesFilter(benchmarkPropertiesWithActiveVariables("ala=k.t"));

        assertThat(filter.test(benchmarkWithVariable("ala", "pies"))).isFalse();
        assertThat(filter.test(benchmarkWithVariable("ala", "kot"))).isTrue();
        assertThat(filter.test(benchmarkWithVariable("ala", "kat"))).isTrue();
        assertThat(filter.test(benchmarkWithVariable("ala", "katar"))).isFalse();
        assertThat(filter.test(benchmarkWithVariable("tola", "kot"))).isFalse();
        assertThat(filter.test(benchmarkWithVariable("tola", "pies"))).isFalse();
    }

    private Benchmark benchmarkWithVariable(String key, String value)
    {
        Benchmark benchmark = mock(Benchmark.class);
        when(benchmark.getVariables())
                .thenReturn(ImmutableMap.of(key, value));

        return benchmark;
    }
}
