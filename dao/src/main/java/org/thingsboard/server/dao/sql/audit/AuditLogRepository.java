/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends PagingAndSortingRepository<AuditLogEntity, UUID> {

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND (:actionTypes IS NULL OR a.actionType in :actionTypes) " +
            "AND (LOWER(a.entityType) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.entityName) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.userName) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionType) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionStatus) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<AuditLogEntity> findByTenantId(
                                 @Param("tenantId") UUID tenantId,
                                 @Param("textSearch") String textSearch,
                                 @Param("startTime") Long startTime,
                                 @Param("endTime") Long endTime,
                                 @Param("actionTypes") List<ActionType> actionTypes,
                                 Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.entityType = :entityType AND a.entityId = :entityId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND (:actionTypes IS NULL OR a.actionType in :actionTypes) " +
            "AND (LOWER(a.entityName) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.userName) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionType) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionStatus) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<AuditLogEntity> findAuditLogsByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                                            @Param("entityType") EntityType entityType,
                                                            @Param("entityId") UUID entityId,
                                                            @Param("textSearch") String textSearch,
                                                            @Param("startTime") Long startTime,
                                                            @Param("endTime") Long endTime,
                                                            @Param("actionTypes") List<ActionType> actionTypes,
                                                            Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND (:actionTypes IS NULL OR a.actionType in :actionTypes) " +
            "AND (LOWER(a.entityType) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.entityName) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.userName) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionType) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionStatus) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<AuditLogEntity> findAuditLogsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                              @Param("customerId") UUID customerId,
                                                              @Param("textSearch") String textSearch,
                                                              @Param("startTime") Long startTime,
                                                              @Param("endTime") Long endTime,
                                                              @Param("actionTypes") List<ActionType> actionTypes,
                                                              Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.userId = :userId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND (:actionTypes IS NULL OR a.actionType in :actionTypes) " +
            "AND (LOWER(a.entityType) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.entityName) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionType) LIKE LOWER(CONCAT(:textSearch, '%'))" +
            "OR LOWER(a.actionStatus) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<AuditLogEntity> findAuditLogsByTenantIdAndUserId(@Param("tenantId") UUID tenantId,
                                                          @Param("userId") UUID userId,
                                                          @Param("textSearch") String textSearch,
                                                          @Param("startTime") Long startTime,
                                                          @Param("endTime") Long endTime,
                                                          @Param("actionTypes") List<ActionType> actionTypes,
                                                          Pageable pageable);

}
