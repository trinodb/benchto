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
        Snapshot snapshot = new Snapshot();
        addDocument(snapshot, "documentX", TIME_BEFORE);
        addDocument(snapshot, "documentX", TIME_NOW);
        addDocument(snapshot, "documentX", TIME_AFTER);
        snapshotRepo.save(snapshot);

        Document latestDoc = documentRepo.findLatestByName("documentX", TIME_NOW);
        assertThat(latestDoc.getTimestamp()).isEqualTo(TIME_NOW);
    }

    private Document addDocument(Snapshot snapshot, String name, ZonedDateTime timestamp)
    {
        Document document = new Document();
        document.setName(name);
        document.setContent("Hello world");
        document.setTimestamp(timestamp);
        document.setSnapshot(snapshot);
        return document;
    }
}
