/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.repo;

import com.teradata.benchmark.service.model.BenchmarkRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

import static javax.persistence.TemporalType.TIMESTAMP;

@Repository
public interface BenchmarkRunRepo
        extends JpaRepository<BenchmarkRun, String>
{
    BenchmarkRun findByNameAndSequenceId(String benchmarkName, String sequenceId);

    List<BenchmarkRun> findByName(String benchmarkName, Pageable pageable);

    @Query(value = "" +
            "SELECT * " +
            "FROM benchmark_runs " +
            "WHERE name = ?1 AND started BETWEEN ?2 AND ?3 " +
            "ORDER BY started " +
            "LIMIT ?5 OFFSET ?4",
            nativeQuery = true)
    List<BenchmarkRun> findByNameAndStartedInRange(String benchmarkName,
            @Temporal(TIMESTAMP) Date from,
            @Temporal(TIMESTAMP) Date to,
            int page, int size);

    @Query(value = "" +
            "WITH summary AS ( " +
            "  SELECT " +
            "    b.id, " +
            "    b.name, " +
            "    b.sequence_id, " +
            "    b.status, " +
            "    b.version, " +
            "    b.started, " +
            "    b.ended, " +
            "    b.environment_id, " +
            "    rank() " +
            "    OVER (PARTITION BY b.name " +
            "      ORDER BY b.sequence_id DESC) AS rk " +
            "  FROM benchmark_runs b " +
            ") " +
            "SELECT s.* " +
            "FROM summary s " +
            "WHERE s.rk = 1 " +
            "ORDER BY s.name " +
            "LIMIT ?2 OFFSET ?1",
            nativeQuery = true)
    List<BenchmarkRun> findLatest(int page, int size);
}
