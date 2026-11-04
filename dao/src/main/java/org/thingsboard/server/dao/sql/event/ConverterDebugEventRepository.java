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
import org.thingsboard.server.common.data.event.ConverterDebugEvent;
import org.thingsboard.server.dao.model.sql.ConverterDebugEventEntity;

import java.util.List;
import java.util.UUID;


public interface ConverterDebugEventRepository extends EventRepository<ConverterDebugEventEntity, ConverterDebugEvent>, JpaRepository<ConverterDebugEventEntity, UUID> {

    @Override
    @Query(nativeQuery = true, value = "SELECT * FROM converter_debug_event e WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId ORDER BY e.ts DESC LIMIT :limit")
    List<ConverterDebugEventEntity> findLatestEvents(@Param("tenantId") UUID tenantId, @Param("entityId") UUID entityId, @Param("limit") int limit);

    @Override
    @Query("SELECT e FROM ConverterDebugEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    Page<ConverterDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                               @Param("entityId") UUID entityId,
                                               @Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime,
                                               Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM converter_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:inParam IS NULL OR e.e_in_message ILIKE concat('%', :inParam, '%')) " +
                    "AND (:outParam IS NULL OR e.e_out_message ILIKE concat('%', :outParam, '%')) " +
                    "AND (:metadata IS NULL OR e.e_metadata ILIKE concat('%', :metadata, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
            ,
            countQuery = "SELECT count(*) FROM converter_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:inParam IS NULL OR e.e_in_message ILIKE concat('%', :inParam, '%')) " +
                    "AND (:outParam IS NULL OR e.e_out_message ILIKE concat('%', :outParam, '%')) " +
                    "AND (:metadata IS NULL OR e.e_metadata ILIKE concat('%', :metadata, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
    )
    Page<ConverterDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                               @Param("entityId") UUID entityId,
                                               @Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime,
                                               @Param("serviceId") String server,
                                               @Param("eventType") String type,
                                               @Param("inParam") String in,
                                               @Param("outParam") String out,
                                               @Param("metadata") String metadata,
                                               @Param("isError") boolean isError,
                                               @Param("error") String error,
                                               Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM ConverterDebugEventEntity e WHERE " +
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
            value = "DELETE FROM converter_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                    "AND (:inParam IS NULL OR e.e_in_message ILIKE concat('%', :inParam, '%')) " +
                    "AND (:outParam IS NULL OR e.e_out_message ILIKE concat('%', :outParam, '%')) " +
                    "AND (:metadata IS NULL OR e.e_metadata ILIKE concat('%', :metadata, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))")
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime,
                      @Param("serviceId") String server,
                      @Param("eventType") String type,
                      @Param("inParam") String in,
                      @Param("outParam") String out,
                      @Param("metadata") String metadata,
                      @Param("isError") boolean isError,
                      @Param("error") String error);
}
