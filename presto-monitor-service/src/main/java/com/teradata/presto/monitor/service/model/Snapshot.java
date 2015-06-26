/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.model;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import java.time.ZonedDateTime;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

@Cacheable
@Entity
@Table(name = "snapshots")
public class Snapshot
{
    @Id
    @SequenceGenerator(name = "snapshots_id_seq",
            sequenceName = "snapshots_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "snapshots_id_seq")
    @Column(name = "id")
    private long id;

    @Column(name = "timestamp")
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    private ZonedDateTime timestamp;

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL)
    private Set<Document> documents = newHashSet();

    public Set<Document> getDocuments()
    {
        return documents;
    }

    public void setDocuments(Set<Document> documents)
    {
        this.documents = documents;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public ZonedDateTime getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp)
    {
        this.timestamp = timestamp;
    }
}
