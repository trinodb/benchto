/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.controllers.response;

import com.teradata.presto.monitor.service.model.Document;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

public class DocumentDescriptor
{
    private final long snapshotId;

    private final String documentTimestamp;

    public DocumentDescriptor(Document document)
    {
        this(document.getSnapshot().getId(), document.getTimestamp().format(ISO_DATE_TIME));
    }

    public DocumentDescriptor(long snapshotId, String documentTimestamp)
    {
        this.snapshotId = snapshotId;
        this.documentTimestamp = documentTimestamp;
    }

    public long getSnapshotId()
    {
        return snapshotId;
    }

    public String getDocumentTimestamp()
    {
        return documentTimestamp;
    }
}
