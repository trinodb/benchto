/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.queries;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.IntegrationTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryLoaderTest extends IntegrationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private QueryLoader queryLoader;

    @Test
    public void shouldLoadPrestoQuery() throws Exception {
        String query = loadQuery("presto/simple_select.sql", "database", "schema");
        assertThat(trimSpaces(query)).isEqualTo("SELECT count(*) FROM \"database\".\"schema\".\"table\"");
        assertThat(query).doesNotContain("comment");
    }

    @Test
    public void shouldFailWhenQueryDoesNotExist() {
        expectedException.expect(BenchmarkExecutionException.class);
        loadQuery("spark/simple_select.sql", "database", "schema");
    }

    @Test
    public void shouldFailsWhenRequiredAttributesAreAbsent() {
        expectedException.expect(BenchmarkExecutionException.class);
        queryLoader.loadQuery("presto/simple_select.sql", emptyMap());
    }

    private String loadQuery(String path, String database, String schema) {
        return queryLoader.loadQuery(path, createAttributes(database, schema));
    }

    private Map<String, String> createAttributes(String database, String schema) {
        return ImmutableMap.<String, String>builder()
                .put("database", database)
                .put("schema", schema)
                .build();
    }

    private String trimSpaces(String string) {
        return string.replaceAll("\\s+", " ");
    }
}