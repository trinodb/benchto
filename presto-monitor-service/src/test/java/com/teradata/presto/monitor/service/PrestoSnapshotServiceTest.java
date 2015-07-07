/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service;

import com.google.common.io.Resources;
import com.teradata.presto.monitor.service.model.Document;
import com.teradata.presto.monitor.service.repo.DocumentRepo;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class PrestoSnapshotServiceTest
        extends IntegrationTestBase
{
    @Autowired
    private DocumentRepo documentRepo;

    @Autowired
    private PrestoSnapshotService snapshotService;

    @Test
    @Transactional
    public void shouldDownloadPrestoSnapshot()
            throws IOException
    {
        verifyServiceJsonRequested();
        verifyQueryListJsonRequested();
        verifyQueryJsonRequested();
        verifyQueryExecutionJsonRequested();

        verifyServiceJsonRequested();
        verifyQueryListJsonRequested();

        snapshotService.makeSnapshot();
        snapshotService.makeSnapshot();

        assertThat(documentRepo.findByEnvironmentAndName("test", "/v1/query/20150630_103317_00032_g6mqe")).hasSize(1);
        Document queryDocument = documentRepo.findFirstDocument("test", "/v1/query/20150630_103317_00032_g6mqe");

        assertThat(queryDocument.getContent()).isEqualTo(loadResource("json/query.json"));
        assertThat(queryDocument.getSnapshot().getDocuments().size()).isEqualTo(4);
    }

    private void verifyServiceJsonRequested()
            throws IOException
    {
        verifyJsonRequested("/v1/service", "json/service.json");
    }

    private void verifyQueryListJsonRequested()
            throws IOException
    {
        verifyJsonRequested("/v1/query", "json/query_list.json");
    }

    private void verifyQueryJsonRequested()
            throws IOException
    {
        verifyJsonRequested("/v1/query/20150630_103317_00032_g6mqe", "json/query.json");
    }

    private void verifyQueryExecutionJsonRequested()
            throws IOException
    {
        verifyJsonRequested("/v1/query-execution/20150630_103317_00032_g6mqe", "json/query_execution.json");
    }

    private void verifyJsonRequested(String path, String resource)
            throws IOException
    {
        restServiceServer.expect(matchAll(
                requestTo("http://presto-test-master:8090" + path),
                method(HttpMethod.GET)
        )).andRespond(withSuccess().contentType(APPLICATION_JSON).body(loadResource(resource)));
    }

    private String loadResource(String resource)
            throws IOException
    {
        return Resources.toString(getResource(resource), UTF_8);
    }
}
