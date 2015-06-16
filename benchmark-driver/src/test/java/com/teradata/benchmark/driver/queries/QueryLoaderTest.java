/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.queries;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.IntegrationTest;
import com.teradata.benchmark.driver.Query;
import com.teradata.benchmark.driver.loader.QueryLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URISyntaxException;
import java.nio.file.Paths;
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

    @Test
    public void shouldLoadPrestoQuery()
            throws Exception
    {
        Query query = loadQuery("sql/presto/simple_select.sql", "database", "schema");
        assertThat(query.getName()).isEqualTo("simple_select");
        assertThat(trimSpaces(query.getSql().trim())).isEqualTo("SELECT 1 FROM \"schema\".SYSTEM_USERS");
        assertThat(query.getSql()).doesNotContain("comment");
    }

    @Test
    public void shouldFailsWhenRequiredAttributesAreAbsent()
            throws URISyntaxException
    {
        expectedException.expect(BenchmarkExecutionException.class);
        queryLoader.loadFromFile(Paths.get(Resources.getResource("sql/presto/simple_select.sql").toURI()), emptyMap());
    }

    private Query loadQuery(String path, String database, String schema)
            throws URISyntaxException
    {
        return queryLoader.loadFromFile(Paths.get(Resources.getResource(path).toURI()), createAttributes(database, schema));
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
