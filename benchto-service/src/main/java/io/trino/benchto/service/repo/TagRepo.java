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

import io.trino.benchto.service.model.Environment;
import io.trino.benchto.service.model.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface TagRepo
        extends CrudRepository<Tag, String>
{
    @Query(value = "SELECT t FROM Tag t " +
            "WHERE t.environment = :environment " +
            "ORDER BY t.created")
    List<Tag> find(@Param("environment") Environment environment);

    @Query(value = "SELECT t FROM Tag t " +
            "WHERE t.environment = :environment" +
            "   AND t.created >= :startDate " +
            "   AND t.created <= :endDate  " +
            "ORDER BY t.created")
    List<Tag> find(
            @Param("environment") Environment environment,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    @Query(value = "SELECT t FROM Tag t " +
            "WHERE t.environment = :environment" +
            "   AND t.created<= :until " +
            "ORDER BY t.created DESC")
    List<Tag> latest(
            @Param("environment") Environment environment,
            @Param("until") ZonedDateTime until,
            Pageable pageable);
}
