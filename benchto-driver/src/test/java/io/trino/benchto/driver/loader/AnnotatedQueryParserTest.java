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

import com.google.common.collect.ImmutableList;
import io.trino.benchto.driver.Query;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotatedQueryParserTest
{
    private final AnnotatedQueryParser queryParser = new AnnotatedQueryParser();

    @Test
    public void singleQuery()
    {
        List<String> fileContent = ImmutableList.of(
                "single sql",
                "query");
        Query parsingResult = queryParser.parseLines("whatever", fileContent);

        assertThat(parsingResult.getProperty("unknownProperty")).isEmpty();
        assertThat(parsingResult.getSqlTemplate()).isEqualTo("single sql\nquery");
    }

    @Test
    public void multipleQueriesWithProperties()
    {
        List<String> fileContent = ImmutableList.of(
                "--! property1: value1;",
                " -- just a comment",
                "\t--! property2: value2",
                "sql query 1;",
                "sql query 2");
        Query parsingResult = queryParser.parseLines("whatever", fileContent);

        assertThat(parsingResult.getProperty("property1").get()).isEqualTo("value1");
        assertThat(parsingResult.getProperty("property2").get()).isEqualTo("value2");

        assertThat(parsingResult.getProperty("property3")).isEmpty();

        assertThat(parsingResult.getSqlTemplate()).isEqualTo(
                "sql query 1;\nsql query 2");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailRedundantOptions()
    {
        List<String> fileContent = ImmutableList.of(
                "--! property1: value",
                "--! property1: value2");
        queryParser.parseLines("whatever", fileContent);
    }
}
