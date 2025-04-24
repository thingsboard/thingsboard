/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
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
package org.thingsboard.server.dao.sql.task;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.dao.model.sql.JobEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Query("SELECT j FROM JobEntity j WHERE j.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(j.key, concat('%', :searchText, '%')) = true " +
           "OR ilike(j.description, concat('%', :searchText, '%')) = true)")
    Page<JobEntity> findByTenantIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE job
            SET result = jsonb_set(
                result,
                '{successfulCount}',
                to_jsonb((result->>'successfulCount')::int + :count)
            )
            WHERE id = :jobId
            RETURNING ((result->>'successfulCount')::int + :count)
                     + (result->>'failedCount')::int = (result->>'totalCount')::int
            """, nativeQuery = true)
    boolean reportTaskSuccess(@Param("jobId") UUID jobId,
                              @Param("count") int count);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE job
            SET result = jsonb_set(
                jsonb_set(
                    result,
                    '{failedCount}',
                    to_jsonb((result->>'failedCount')::int + 1)
                ),
                ARRAY['failures', :taskKey],
                to_jsonb(:error)
            )
            WHERE id = :jobId
            RETURNING ((result->>'failedCount')::int + 1) + (result->>'successfulCount')::int
            = (result->>'totalCount')::int
            """, nativeQuery = true)
    boolean reportTaskFailure(@Param("jobId") UUID jobId,
                              @Param("taskKey") String taskKey,
                              @Param("error") String error);

    boolean existsByKeyAndStatusIn(String key, List<JobStatus> statuses);

    boolean existsByTenantIdAndTypeAndStatusIn(UUID tenantId, JobType type, List<JobStatus> statuses);

}
