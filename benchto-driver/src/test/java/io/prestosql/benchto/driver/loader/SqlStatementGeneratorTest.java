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
package io.prestosql.benchto.driver.loader;

import com.google.common.collect.ImmutableMap;
import io.prestosql.benchto.driver.Query;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlStatementGeneratorTest
{
    private SqlStatementGenerator sqlStatementGenerator;

    @Before
    public void setUp()
    {
        sqlStatementGenerator = new SqlStatementGenerator();
    }

    @Test
    public void testSimpleQuery()
    {
        Query query = new Query("simpleQuery", "select * from nation", ImmutableMap.of());
        List<String> queries = sqlStatementGenerator.generateQuerySqlStatement(query, ImmutableMap.of());
        assertThat(queries).containsExactly("select * from nation");
    }

    @Test
    public void testListTemplateQuery()
    {
        String listTemplateQuery =
                "<#list 0..<concurrency_level?number as execution_sequence_id>\n" +
                        "CREATE TABLE ${execution_sequence_id} ${\"\\x003B\"}\n" +
                        "</#list>";
        Query query = new Query("listQuery", listTemplateQuery, ImmutableMap.of());
        List<String> queries = sqlStatementGenerator.generateQuerySqlStatement(query, ImmutableMap.of("concurrency_level", 2));
        assertThat(queries).containsExactly("CREATE TABLE 0", "CREATE TABLE 1");
    }
}
