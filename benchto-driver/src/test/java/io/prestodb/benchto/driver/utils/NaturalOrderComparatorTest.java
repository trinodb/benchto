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
package io.prestodb.benchto.driver.utils;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static org.assertj.core.api.Assertions.assertThat;

public class NaturalOrderComparatorTest
{
    @Test
    public void testForStrings()
            throws Exception
    {
        assertStringsNaturalSort(asList(), asList());
        assertStringsNaturalSort(asList("1"), asList("1"));
        assertStringsNaturalSort(asList("1", "100", "10", "2"), asList("1", "2", "10", "100"));
        assertStringsNaturalSort(asList("value1", "value100", "value2"), asList("value1", "value2", "value100"));
        assertStringsNaturalSort(asList("qwerty_100500_qwerty", "qwerty_10050_qwerty", "qwerty_1005_qwerty"),
                asList("qwerty_1005_qwerty", "qwerty_10050_qwerty", "qwerty_100500_qwerty"));
        assertStringsNaturalSort(
                asList("selectivity=50", "selectivity=100", "selectivity=0", "selectivity=2", "selectivity=10"),
                asList("selectivity=0", "selectivity=2", "selectivity=10", "selectivity=50", "selectivity=100"));
    }

    @Test
    public void testForPaths()
            throws Exception
    {
        assertPathsNaturalSort(
                asList(
                        Paths.get("benchmarks/presto/linear-scan/selectivity=0.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=10.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=100.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=2.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=50.yaml")),
                asList(
                        Paths.get("benchmarks/presto/linear-scan/selectivity=0.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=2.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=10.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=50.yaml"),
                        Paths.get("benchmarks/presto/linear-scan/selectivity=100.yaml")));
    }

    private void assertStringsNaturalSort(Collection<String> strings, Collection<String> expected)
    {
        assertNaturalSort(strings, expected, NaturalOrderComparator.forStrings());
    }

    private void assertPathsNaturalSort(Collection<Path> paths, Collection<Path> expected)
    {
        assertNaturalSort(paths, expected, NaturalOrderComparator.forPaths());
    }

    private <T> void assertNaturalSort(Collection<T> input, Collection<T> expected, Comparator<T> comparator)
    {
        List<T> list = new ArrayList<>(input);
        sort(list, comparator);
        assertThat(list).containsExactlyElementsOf(expected);
    }
}
