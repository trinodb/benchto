/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.model;

import org.hibernate.annotations.Type;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Cacheable
@Entity
@Table(name = "documents", uniqueConstraints = @UniqueConstraint(columnNames = {"environment", "name", "timestamp"}))
public class Document
{
    @Id
    @SequenceGenerator(name = "documents_id_seq",
            sequenceName = "documents_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "documents_id_seq")
    @Column(name = "id")
    private long id;

    @NotNull
    @Column(name = "environment")
    private String environment;

    @NotNull
    @Column(name = "name")
    private String name;

    @Column(name = "timestamp")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    private ZonedDateTime timestamp;

    @NotNull
    @Column(name = "content", columnDefinition="TEXT")
    private String content;

    @NotNull
    @ManyToOne
    private Snapshot snapshot;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(String environment)
    {
        this.environment = environment;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public ZonedDateTime getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Snapshot getSnapshot()
    {
        return snapshot;
    }

    public void setSnapshot(Snapshot snapshot)
    {
        this.snapshot = snapshot;
        snapshot.getDocuments().add(this);
    }
}
