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
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.dao.model.sql.ErrorEventEntity;
import org.thingsboard.server.dao.model.sql.RawDataEventEntity;

import java.util.List;
import java.util.UUID;


public interface RawEventRepository extends EventRepository<RawDataEventEntity, RawDataEvent>, JpaRepository<RawDataEventEntity, UUID> {

    @Override
    @Query(nativeQuery = true, value = "SELECT * FROM raw_data_event e WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId ORDER BY e.ts DESC LIMIT :limit")
    List<RawDataEventEntity> findLatestEvents(@Param("tenantId") UUID tenantId, @Param("entityId") UUID entityId, @Param("limit") int limit);

    @Override
    @Query("SELECT e FROM RawDataEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    Page<RawDataEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                        @Param("entityId") UUID entityId,
                                        @Param("startTime") Long startTime,
                                        @Param("endTime") Long endTime,
                                        Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM raw_data_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:uuidStr IS NULL OR e.e_uuid ILIKE concat('%', :uuidStr, '%')) " +
                    "AND (:messageType IS NULL OR e.e_message_type ILIKE concat('%', :messageType, '%')) " +
                    "AND (:message IS NULL OR e.e_message ILIKE concat('%', :message, '%'))"
            ,
            countQuery = "SELECT count(*) FROM raw_data_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:uuidStr IS NULL OR e.e_uuid ILIKE concat('%', :uuidStr, '%')) " +
                    "AND (:messageType IS NULL OR e.e_message_type ILIKE concat('%', :messageType, '%')) " +
                    "AND (:message IS NULL OR e.e_message ILIKE concat('%', :message, '%'))"
    )
    Page<ErrorEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                      @Param("entityId") UUID entityId,
                                      @Param("startTime") Long startTime,
                                      @Param("endTime") Long endTime,
                                      @Param("serviceId") String server,
                                      @Param("uuidStr") String uuid,
                                      @Param("messageType") String messageType,
                                      @Param("message") String message,
                                      Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM RawDataEventEntity e WHERE " +
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
            value = "DELETE FROM raw_data_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:uuidStr IS NULL OR e.e_uuid ILIKE concat('%', :uuidStr, '%')) " +
                    "AND (:messageType IS NULL OR e.e_message_type ILIKE concat('%', :messageType, '%')) " +
                    "AND (:message IS NULL OR e.e_message ILIKE concat('%', :message, '%'))"
    )
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime,
                      @Param("serviceId") String server,
                      @Param("uuidStr") String uuid,
                      @Param("messageType") String messageType,
                      @Param("message") String message);
}
