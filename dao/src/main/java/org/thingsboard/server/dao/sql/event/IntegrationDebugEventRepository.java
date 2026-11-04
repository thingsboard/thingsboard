// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.dao.model.sql.IntegrationDebugEventEntity;

import java.util.List;
import java.util.UUID;


public interface IntegrationDebugEventRepository extends EventRepository<IntegrationDebugEventEntity, IntegrationDebugEvent>, JpaRepository<IntegrationDebugEventEntity, UUID> {

    @Override
    @Query(nativeQuery = true, value = "SELECT * FROM integration_debug_event e WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId ORDER BY e.ts DESC LIMIT :limit")
    List<IntegrationDebugEventEntity> findLatestEvents(@Param("tenantId") UUID tenantId, @Param("entityId") UUID entityId, @Param("limit") int limit);

    @Override
    @Query("SELECT e FROM IntegrationDebugEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    Page<IntegrationDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                                 @Param("entityId") UUID entityId,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime,
                                                 Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM integration_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:message IS NULL OR e.e_message ILIKE concat('%', :message, '%')) " +
                    "AND (:status IS NULL OR e.e_status ILIKE concat('%', :status, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
            ,
            countQuery = "SELECT count(*) FROM integration_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:message IS NULL OR e.e_message ILIKE concat('%', :message, '%')) " +
                    "AND (:status IS NULL OR e.e_status ILIKE concat('%', :status, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
    )
    Page<IntegrationDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                                 @Param("entityId") UUID entityId,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime,
                                                 @Param("serviceId") String server,
                                                 @Param("eventType") String type,
                                                 @Param("message") String message,
                                                 @Param("status") String status,
                                                 @Param("isError") boolean isError,
                                                 @Param("error") String error,
                                                 Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM IntegrationDebugEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime);

    @Transactional
    @Modifying
    @Query(nativeQuery = true,
            value = "DELETE FROM integration_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:message IS NULL OR e.e_message ILIKE concat('%', :message, '%')) " +
                    "AND (:status IS NULL OR e.e_status ILIKE concat('%', :status, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))")
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime,
                      @Param("serviceId") String server,
                      @Param("eventType") String type,
                      @Param("message") String message,
                      @Param("status") String status,
                      @Param("isError") boolean isError,
                      @Param("error") String error);
}
