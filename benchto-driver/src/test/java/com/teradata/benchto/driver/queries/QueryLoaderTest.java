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
package com.teradata.benchto.driver.queries;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.teradata.benchto.driver.BenchmarkExecutionException;
import com.teradata.benchto.driver.IntegrationTest;
import com.teradata.benchto.driver.Query;
import com.teradata.benchto.driver.loader.QueryLoader;
import com.teradata.benchto.driver.loader.SqlStatementGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryLoaderTest
        extends IntegrationTest
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private QueryLoader queryLoader;

    @Autowired
    private SqlStatementGenerator sqlStatementGenerator;

    @Test
    public void shouldLoadPrestoQuery()
            throws Exception
    {
        Query query = queryLoader.loadFromFile("presto/simple_select.sql");
        List<String> sqlStatements = sqlStatementGenerator.generateQuerySqlStatement(query, createAttributes("database", "schema"));
        assertThat(query.getName()).isEqualTo("simple_select");
        assertThat(sqlStatements).containsExactly("SELECT 1 FROM \"schema\".SYSTEM_USERS");
    }

    @Test
    public void shouldFailsWhenRequiredAttributesAreAbsent()
            throws URISyntaxException
    {
        Query query = queryLoader.loadFromFile("presto/simple_select.sql");

        expectedException.expect(BenchmarkExecutionException.class);
        sqlStatementGenerator.generateQuerySqlStatement(query, emptyMap());
    }

    private Map<String, String> createAttributes(String database, String schema)
    {
        return ImmutableMap.<String, String>builder()
                .put("database", database)
                .put("schema", schema)
                .build();
    }

    private String trimSpaces(String string)
    {
        return string.replaceAll("\\s+", " ");
    }
}
