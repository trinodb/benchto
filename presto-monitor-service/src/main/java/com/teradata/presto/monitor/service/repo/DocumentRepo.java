/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.presto.monitor.service.repo;

import com.teradata.presto.monitor.service.model.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;

@Repository
public interface DocumentRepo
        extends JpaRepository<Document, Long>
{
    @Query(value = "" +
            "from " +
            "  Document d " +
            "where " +
            "  d.name = :name and d.timestamp <= :timestamp " +
            "order by d.timestamp desc")
    List<Document> findLatestByName(@Param("name") String name, @Param("timestamp") ZonedDateTime timestamp, Pageable pageable);

    default Document findLatestByName(String name, ZonedDateTime timestamp)
    {
        return getOnlyElement(findLatestByName(name, timestamp, new PageRequest(0, 1)));
    }
}
