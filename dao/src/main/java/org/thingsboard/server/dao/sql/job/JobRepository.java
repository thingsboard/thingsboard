// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.common.data.util.TbTriple;
import org.thingsboard.server.dao.model.sql.JobEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Query("SELECT j FROM JobEntity j WHERE j.tenantId = :tenantId " +
           "AND j.customerId = :customerId " +
           "AND (:types IS NULL OR j.type IN (:types)) " +
           "AND (:statuses IS NULL OR j.status IN (:statuses)) " +
           "AND (:entities IS NULL OR j.entityId IN :entities) " +
           "AND (:startTime <= 0 OR j.createdTime >= :startTime) " +
           "AND (:endTime <= 0 OR j.createdTime <= :endTime) " +
           "AND (:searchText IS NULL OR ilike(j.key, concat('%', :searchText, '%')) = true)")
    Page<JobEntity> findByTenantIdAndCustomerIdAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                                  @Param("customerId") UUID customerId,
                                                                                                  @Param("types") List<JobType> types,
                                                                                                  @Param("statuses") List<JobStatus> statuses,
                                                                                                  @Param("entities") List<UUID> entities,
                                                                                                  @Param("startTime") long startTime,
                                                                                                  @Param("endTime") long endTime,
                                                                                                  @Param("searchText") String searchText,
                                                                                                  Pageable pageable);

    @Query("SELECT j FROM JobEntity j WHERE j.tenantId = :tenantId " +
           "AND (:types IS NULL OR j.type IN (:types)) " +
           "AND (:statuses IS NULL OR j.status IN (:statuses)) " +
           "AND (:entities IS NULL OR j.entityId IN :entities) " +
           "AND (:startTime <= 0 OR j.createdTime >= :startTime) " +
           "AND (:endTime <= 0 OR j.createdTime <= :endTime) " +
           "AND (:searchText IS NULL OR ilike(j.key, concat('%', :searchText, '%')) = true)")
    Page<JobEntity> findAllByTenantIdAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                        @Param("types") List<JobType> types,
                                                                                        @Param("statuses") List<JobStatus> statuses,
                                                                                        @Param("entities") List<UUID> entities,
                                                                                        @Param("startTime") long startTime,
                                                                                        @Param("endTime") long endTime,
                                                                                        @Param("searchText") String searchText,
                                                                                        Pageable pageable);

    @Query("SELECT j FROM JobEntity j WHERE j.tenantId = :tenantId " +
            "AND (j.customerId IS NULL OR j.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:types IS NULL OR j.type IN (:types)) " +
            "AND (:statuses IS NULL OR j.status IN (:statuses)) " +
            "AND (:entities IS NULL OR j.entityId IN :entities) " +
            "AND (:startTime <= 0 OR j.createdTime >= :startTime) " +
            "AND (:endTime <= 0 OR j.createdTime <= :endTime) " +
            "AND (:searchText IS NULL OR ilike(j.key, concat('%', :searchText, '%')) = true)")
    Page<JobEntity> findTenantJobsByTypesAndStatusesAndEntitiesAndTimeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                    @Param("types") List<JobType> types,
                                                                                    @Param("statuses") List<JobStatus> statuses,
                                                                                    @Param("entities") List<UUID> entities,
                                                                                    @Param("startTime") long startTime,
                                                                                    @Param("endTime") long endTime,
                                                                                    @Param("searchText") String searchText,
                                                                                    Pageable pageable);

    @Query(value = "SELECT e.*, e.created_time as createdtime FROM (" +
            "    SELECT j.id, j.created_time, j.tenant_id, j.type, j.key," +
            "           j.entity_id, j.entity_type, j.status, j.configuration, j.result, j.customer_id" +
            "    FROM job j" +
            ") e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (COALESCE(:types) IS NULL OR e.type IN (:types)) " +
            "AND (COALESCE(:statuses) IS NULL OR e.status IN (:statuses)) " +
            "AND (COALESCE(:entities) IS NULL OR e.entity_id IN (:entities)) " +
            "AND (:startTime <= 0 OR e.created_time >= :startTime) " +
            "AND (:endTime <= 0 OR e.created_time <= :endTime) " +
            "AND (COALESCE(:searchText) IS NULL OR e.key ILIKE CONCAT('%', :searchText, '%')) ",
            countQuery = "SELECT count(e.id) FROM job e " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (COALESCE(:types) IS NULL OR e.type IN (:types)) " +
                    "AND (COALESCE(:statuses) IS NULL OR e.status IN (:statuses)) " +
                    "AND (COALESCE(:entities) IS NULL OR e.entity_id IN (:entities)) " +
                    "AND (:startTime <= 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime <= 0 OR e.created_time <= :endTime) " +
                    "AND (COALESCE(:searchText) IS NULL OR e.key ILIKE CONCAT('%', :searchText, '%')) ",
            nativeQuery = true)
    Page<JobEntity> findByTenantIdAndSubCustomersAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                                    @Param("customerId") UUID customerId,
                                                                                                    @Param("types") List<String> types,
                                                                                                    @Param("statuses") List<String> statuses,
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

    @Query("SELECT NEW org.thingsboard.server.common.data.util.TbTriple(job.type, job.status, COUNT(job)) " +
            "FROM JobEntity job " +
            "WHERE job.createdTime >= :sinceMillis " +
            "GROUP BY job.type, job.status")
    List<TbTriple<JobType, JobStatus, Long>> findCountsGroupedByTypeAndStatusSince(@Param("sinceMillis") long sinceMillis);

}
