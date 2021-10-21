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
package io.trino.benchto.driver.queries;

import com.google.common.collect.ImmutableMap;
import io.trino.benchto.driver.BenchmarkExecutionException;
import io.trino.benchto.driver.IntegrationTest;
import io.trino.benchto.driver.Query;
import io.trino.benchto.driver.loader.QueryLoader;
import io.trino.benchto.driver.loader.SqlStatementGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

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
    {
        verifySimpleSelect("presto/simple_select.sql", "simple_select", "1");
        verifySimpleSelect("presto/second_simple_select.sql", "second_simple_select", "2");
    }

    private void verifySimpleSelect(String path, String queryName, String rowValue)
    {
        Query query = queryLoader.loadFromFile(path);
        List<String> sqlStatements = sqlStatementGenerator.generateQuerySqlStatement(query, createAttributes("database", "schema"));
        assertThat(query.getName()).isEqualTo(queryName);
        assertThat(sqlStatements).containsExactly("SELECT " + rowValue + " FROM \"schema\".SYSTEM_USERS");
    }

    @Test
    public void shouldFailWhenNoQueryFile()
    {
        expectedException.expect(BenchmarkExecutionException.class);
        expectedException.expectMessage("Could not find any SQL query file for query name: presto/non_existing_file.sql");

        queryLoader.loadFromFile("presto/non_existing_file.sql");
    }

    @Test
    public void shouldFailWhenQueryFileIsDuplicated()
    {
        expectedException.expect(BenchmarkExecutionException.class);
        expectedException.expectMessage("Found multiple SQL query files for query name: presto/duplicate_query.sql");

        queryLoader.loadFromFile("presto/duplicate_query.sql");
    }

    @Test
    public void shouldFailsWhenRequiredAttributesAreAbsent()
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
}
