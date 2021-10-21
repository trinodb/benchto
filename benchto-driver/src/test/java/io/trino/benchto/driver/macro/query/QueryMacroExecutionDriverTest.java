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
package io.trino.benchto.driver.macro.query;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryMacroExecutionDriverTest
{
    @Test
    public void testExtractKeyValue()
    {
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session optimize_hash_generation=false"))
                .contains("optimize_hash_generation", "false");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session task_default_concurrency=8"))
                .contains("task_default_concurrency", "8");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session task_default_concurrency = 8"))
                .contains("task_default_concurrency", "8");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session execution_policy='all-at-once'"))
                .contains("execution_policy", "all-at-once");
        assertThat(QueryMacroExecutionDriver.extractKeyValue("set session foo='bar=baz'"))
                .contains("foo", "bar=baz");
    }
}
