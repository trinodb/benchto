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
package io.trino.benchto.driver.utils;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static io.trino.benchto.driver.utils.YamlUtils.loadYamlFromPath;
import static io.trino.benchto.driver.utils.YamlUtils.loadYamlFromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class YamlUtilsTest
{
    @Test
    public void testLoadYamlFromString()
    {
        assertThat(loadYamlFromString("")).containsOnly();
        assertThat(loadYamlFromString("first-value: 1")).containsOnly(entry("first-value", 1));
        assertThat(loadYamlFromString("first-value: 1\nsecond-value: 2")).containsOnly(entry("first-value", 1), entry("second-value", 2));
    }

    @Test
    public void testLoadYamlFromPath()
            throws IOException
    {
        assertThat(loadYamlFromPath(Paths.get(this.getClass().getResource("empty.yaml").getPath()))).containsOnly();
        assertThat(loadYamlFromPath(Paths.get(this.getClass().getResource("single_value.yaml").getPath()))).containsOnly(entry("first-value", 1));
        assertThat(loadYamlFromPath(Paths.get(this.getClass().getResource("multiple_values.yaml").getPath()))).containsOnly(entry("first-value", 1), entry("second-value", 2));
    }
}
