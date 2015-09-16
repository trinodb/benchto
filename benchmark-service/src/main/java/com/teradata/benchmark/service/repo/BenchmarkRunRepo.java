/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.repo;

import com.teradata.benchmark.service.model.BenchmarkRun;
import com.teradata.benchmark.service.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface BenchmarkRunRepo
        extends JpaRepository<BenchmarkRun, String>
{
    BenchmarkRun findByUniqueNameAndSequenceId(String uniqueName, String sequenceId);

    List<BenchmarkRun> findByUniqueNameAndEnvironmentOrderBySequenceIdDesc(String uniqueName, Environment environment);

    @Query(value = "" +
            "WITH summary AS ( " +
            "  SELECT " +
            "    b.id, " +
            "    b.name, " +
            "    b.unique_name, " +
            "    b.sequence_id, " +
            "    b.status, " +
            "    b.version, " +
            "    b.started, " +
            "    b.ended, " +
            "    b.environment_id, " +
            "    b.executions_mean_duration, " +
            "    b.executions_stddev_duration, " +
            "    rank() " +
            "    OVER (PARTITION BY b.unique_name, b.environment_id " +
            "      ORDER BY b.sequence_id DESC) AS rk " +
            "  FROM benchmark_runs b " +
            ") " +
            "SELECT s.* " +
            "FROM summary s " +
            "WHERE s.rk = 1 " +
            "ORDER BY s.started DESC ",
            nativeQuery = true)
    List<BenchmarkRun> findLatest();

    @Query("SELECT br FROM BenchmarkRun br WHERE " +
            "br.status = 'STARTED' AND " +
            "br.started <= :startDate")
    List<BenchmarkRun> findSartedBefore(@Param("startDate") ZonedDateTime startDate);
}
