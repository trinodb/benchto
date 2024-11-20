/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.benchto.service.repo;

import io.trino.benchto.service.model.BenchmarkRun;
import io.trino.benchto.service.model.Environment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface BenchmarkRunRepo
        extends JpaRepository<BenchmarkRun, String>
{
    BenchmarkRun findByUniqueNameAndSequenceId(String uniqueName, String sequenceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    BenchmarkRun findForUpdateByUniqueNameAndSequenceId(String uniqueName, String sequenceId);

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
            "  WHERE b.environment_id = :environment_id " +
            ") " +
            "SELECT s.* " +
            "FROM summary s " +
            "WHERE s.rk = 1 " +
            "ORDER BY s.started DESC ",
            nativeQuery = true)
    List<BenchmarkRun> findLatest(@Param("environment_id") long environmentId);

    @Query("SELECT id FROM BenchmarkRun br WHERE " +
            "br.status = 'STARTED' AND " +
            "br.started <= :startDate")
    List<Long> findStartedBefore(@Param("startDate") ZonedDateTime startDate);

    @Query(value = "" +
            "SELECT MAX(ended) " +
            "FROM benchmark_runs " +
            "WHERE unique_name = :uniqueName and status = 'ENDED'",
            nativeQuery = true)
    Timestamp findTimeOfLatestSuccessfulExecution(@Param("uniqueName") String uniqueName);

    @Modifying
    @Query("UPDATE BenchmarkRun br SET br.ended = :ended, br.status = 'FAILED' WHERE br.id = :id")
    void markAsFailed(@Param(value = "id") long id, @Param(value = "ended") ZonedDateTime ended);
}
