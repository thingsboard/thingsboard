// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.job;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.dao.model.sql.JobEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Query("SELECT j FROM JobEntity j WHERE j.tenantId = :tenantId " +
           "AND (:types IS NULL OR j.type IN (:types)) " +
           "AND (:statuses IS NULL OR j.status IN (:statuses)) " +
           "AND (:entities IS NULL OR j.entityId IN :entities) " +
           "AND (:startTime <= 0 OR j.createdTime >= :startTime) " +
           "AND (:endTime <= 0 OR j.createdTime <= :endTime) " +
           "AND (:searchText IS NULL OR ilike(j.key, concat('%', :searchText, '%')) = true)")
    Page<JobEntity> findByTenantIdAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                     @Param("types") List<JobType> types,
                                                                                     @Param("statuses") List<JobStatus> statuses,
                                                                                     @Param("entities") List<UUID> entities,
                                                                                     @Param("startTime") long startTime,
                                                                                     @Param("endTime") long endTime,
                                                                                     @Param("searchText") String searchText,
                                                                                     Pageable pageable);

    @Query(value = "SELECT * FROM job j WHERE j.id = :id FOR UPDATE", nativeQuery = true)
    JobEntity findByIdForUpdate(UUID id);

    @Query("SELECT j FROM JobEntity j WHERE j.tenantId = :tenantId AND j.key = :key " +
           "ORDER BY j.createdTime DESC")
    JobEntity findLatestByTenantIdAndKey(@Param("tenantId") UUID tenantId, @Param("key") String key, Limit limit);

    boolean existsByTenantIdAndKeyAndStatusIn(UUID tenantId, String key, List<JobStatus> statuses);

    boolean existsByTenantIdAndTypeAndStatusIn(UUID tenantId, JobType type, List<JobStatus> statuses);

    boolean existsByTenantIdAndEntityIdAndStatusIn(UUID tenantId, UUID entityId, List<JobStatus> statuses);

    @Query(value = "SELECT * FROM job j WHERE j.tenant_id = :tenantId AND j.type = :type " +
                   "AND j.status = :status ORDER BY j.created_time ASC, j.id ASC LIMIT 1 FOR UPDATE", nativeQuery = true)
    JobEntity findOldestByTenantIdAndTypeAndStatusForUpdate(UUID tenantId, String type, String status);

    @Transactional
    @Modifying
    @Query("DELETE FROM JobEntity j WHERE j.tenantId = :tenantId")
    void deleteByTenantId(UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM JobEntity j WHERE j.entityId = :entityId")
    int deleteByEntityId(UUID entityId);

}
