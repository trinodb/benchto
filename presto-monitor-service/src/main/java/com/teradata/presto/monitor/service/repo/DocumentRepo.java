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
    default Document findFirstDocument(String environment, String name)
    {
        return findFirstByEnvironmentAndNameOrderByTimestampAsc(environment, name);
    }

    default Document findLastDocument(String environment, String name)
    {
        return findFirstByEnvironmentAndNameOrderByTimestampDesc(environment, name);
    }

    default Document findLatestByName(String environment, String name, ZonedDateTime timestamp)
    {
        List<Document> documents = findLatestByName(environment, name, timestamp, new PageRequest(0, 1));
        return !documents.isEmpty() ? getOnlyElement(documents) : null;
    }

    Document findFirstByEnvironmentAndNameOrderByTimestampAsc(String environment, String name);

    Document findFirstByEnvironmentAndNameOrderByTimestampDesc(String environment, String name);

    @Query(value = "" +
            "from " +
            "  Document d " +
            "where " +
            "  d.environment = :environment and d.name = :name and d.timestamp <= :timestamp " +
            "order by d.timestamp desc")
    List<Document> findLatestByName(@Param("environment") String environment, @Param("name") String name, @Param("timestamp") ZonedDateTime timestamp, Pageable pageable);

    List<Document> findByEnvironmentAndName(String environment, String name);
}
