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
package com.teradata.benchto.driver.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import static com.teradata.benchto.driver.utils.FilterUtils.pathContainsAny;
import static org.assertj.core.api.Assertions.assertThat;

public class FilterUtilsTest
{
    @Test
    public void test()
    {
        Predicate<Path> pathPredicate = pathContainsAny(ImmutableList.of("simple", "YACK"));

        assertThat(pathPredicate.test(Paths.get("simple"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/simple/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/YACK/file.yaml"))).isTrue();
        assertThat(pathPredicate.test(Paths.get("dir/YA-CK/file.yaml"))).isFalse();
    }
}
