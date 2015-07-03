/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.repo;

import com.teradata.presto.monitor.service.IntegrationTestBase;
import com.teradata.presto.monitor.service.category.IntegrationTest;
import com.teradata.presto.monitor.service.model.Document;
import com.teradata.presto.monitor.service.model.Snapshot;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Category(IntegrationTest.class)
public class DocumentRepoTest
        extends IntegrationTestBase
{
    private static final String ENVIRONMENT = "env";

    private static final ZonedDateTime TIME_NOW = ZonedDateTime.now();
    private static final ZonedDateTime TIME_BEFORE = TIME_NOW.minusSeconds(1);
    private static final ZonedDateTime TIME_AFTER = TIME_NOW.plusSeconds(1);

    @Autowired
    private DocumentRepo documentRepo;

    @Autowired
    private SnapshotRepo snapshotRepo;

    @Test
    public void shouldFindLatestDocument()
    {
        createSampleSnapshotWithDocuments("documentY");

        Document latestDoc = documentRepo.findLatestByName(ENVIRONMENT, "documentY", TIME_NOW);
        assertThat(latestDoc.getTimestamp()).isEqualTo(TIME_NOW);
    }

    @Test
    public void shouldFindFirstDocument()
    {
        createSampleSnapshotWithDocuments("documentX");

        Document latestDoc = documentRepo.findFirstByEnvironmentAndNameOrderByTimestampAsc(ENVIRONMENT, "documentX");
        assertThat(latestDoc.getTimestamp()).isEqualTo(TIME_BEFORE);
    }

    private Snapshot createSampleSnapshotWithDocuments(String documentId)
    {
        Snapshot snapshot = getSnapshot();

        addDocument(snapshot, documentId, TIME_BEFORE);
        addDocument(snapshot, documentId, TIME_NOW);
        addDocument(snapshot, documentId, TIME_AFTER);
        snapshotRepo.save(snapshot);

        return snapshot;
    }

    private Snapshot getSnapshot()
    {
        Snapshot snapshot = new Snapshot();
        snapshot.setTimestamp(ZonedDateTime.now());
        return snapshot;
    }

    private Document addDocument(Snapshot snapshot, String name, ZonedDateTime timestamp)
    {
        Document document = new Document();
        document.setEnvironment(ENVIRONMENT);
        document.setName(name);
        document.setContent("Hello world");
        document.setTimestamp(timestamp);
        document.setSnapshot(snapshot);
        return document;
    }
}
