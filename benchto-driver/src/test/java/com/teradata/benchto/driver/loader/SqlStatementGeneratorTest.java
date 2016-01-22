/*
 * Copyright 2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.teradata.benchto.driver.Query;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlStatementGeneratorTest {

    private SqlStatementGenerator sqlStatementGenerator;

    @Before
    public void setUp() {
        sqlStatementGenerator = new SqlStatementGenerator();
    }

    @Test
    public void testSimpleQuery() {
        Query query = new Query("simpleQuery", "select * from nation", ImmutableMap.of());
        List<String> queries = sqlStatementGenerator.generateQuerySqlStatement(query, ImmutableMap.of());
        assertThat(queries).containsExactly("list");
    }

    @Test
    public void testListTemplateQuery() {
        String listTemplateQuery =
                "<#list 0..<concurrency_level?number as execution_sequence_id>\n" +
                "CREATE TABLE ${execution_sequence_id} ${\"\\x003B\"}\n" +
                "</#list>";
        Query query = new Query("listQuery", listTemplateQuery, ImmutableMap.of());
        List<String> queries = sqlStatementGenerator.generateQuerySqlStatement(query, ImmutableMap.of("concurrency_level", 2));
        assertThat(queries).containsExactly("CREATE TABLE 0", "CREATE TABLE 1");
    }
}
