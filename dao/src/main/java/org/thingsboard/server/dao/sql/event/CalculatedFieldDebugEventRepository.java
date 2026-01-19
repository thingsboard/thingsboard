/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.dao.model.sql.CalculatedFieldDebugEventEntity;

import java.util.List;
import java.util.UUID;

public interface CalculatedFieldDebugEventRepository extends EventRepository<CalculatedFieldDebugEventEntity, CalculatedFieldDebugEvent>, JpaRepository<CalculatedFieldDebugEventEntity, UUID> {

    @Override
    @Query(nativeQuery = true, value = "SELECT * FROM cf_debug_event e WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId ORDER BY e.ts DESC LIMIT :limit")
    List<CalculatedFieldDebugEventEntity> findLatestEvents(@Param("tenantId") UUID tenantId, @Param("entityId") UUID entityId, @Param("limit") int limit);

    @Override
    @Query("SELECT e FROM CalculatedFieldDebugEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    Page<CalculatedFieldDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                                     @Param("entityId") UUID entityId,
                                                     @Param("startTime") Long startTime,
                                                     @Param("endTime") Long endTime,
                                                     Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM cf_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:calculatedFieldId IS NULL OR e.cf_id = uuid(:calculatedFieldId)) " +
                    "AND (:eventEntityId IS NULL OR e.e_entity_id = uuid(:eventEntityId)) " +
                    "AND (:eventEntityType IS NULL OR e.e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                    "AND (:msgId IS NULL OR e.e_msg_id = uuid(:msgId)) " +
                    "AND (:msgType IS NULL OR e.e_msg_type ILIKE concat('%', :msgType, '%')) " +
                    "AND (:eventArguments IS NULL OR e.e_args ILIKE concat('%', :eventArguments, '%')) " +
                    "AND (:eventResult IS NULL OR e.e_result ILIKE concat('%', :eventResult, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
            ,
            countQuery = "SELECT count(*) FROM cf_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:calculatedFieldId IS NULL OR e.cf_id = uuid(:calculatedFieldId)) " +
                    "AND (:eventEntityId IS NULL OR e.e_entity_id = uuid(:eventEntityId)) " +
                    "AND (:eventEntityType IS NULL OR e.e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                    "AND (:msgId IS NULL OR e.e_msg_id = uuid(:msgId)) " +
                    "AND (:msgType IS NULL OR e.e_msg_type ILIKE concat('%', :msgType, '%')) " +
                    "AND (:eventArguments IS NULL OR e.e_args ILIKE concat('%', :eventArguments, '%')) " +
                    "AND (:eventResult IS NULL OR e.e_result ILIKE concat('%', :eventResult, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))"
    )
    Page<CalculatedFieldDebugEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                                     @Param("entityId") UUID entityId,
                                                     @Param("startTime") Long startTime,
                                                     @Param("endTime") Long endTime,
                                                     @Param("serviceId") String serviceId,
                                                     @Param("calculatedFieldId") UUID calculatedFieldId,
                                                     @Param("eventEntityId") String eventEntityId,
                                                     @Param("eventEntityType") String eventEntityType,
                                                     @Param("msgId") String eventMsgId,
                                                     @Param("msgType") String eventMsgType,
                                                     @Param("eventArguments") String eventArguments,
                                                     @Param("eventResult") String eventResult,
                                                     @Param("isError") boolean isError,
                                                     @Param("error") String error,
                                                     Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM CalculatedFieldDebugEventEntity e WHERE " +
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
            value = "DELETE FROM cf_debug_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:calculatedFieldId IS NULL OR e.cf_id = uuid(:calculatedFieldId)) " +
                    "AND (:eventEntityId IS NULL OR e.e_entity_id = uuid(:eventEntityId)) " +
                    "AND (:eventEntityType IS NULL OR e.e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                    "AND (:msgId IS NULL OR e.e_msg_id = uuid(:msgId)) " +
                    "AND (:msgType IS NULL OR e.e_msg_type ILIKE concat('%', :msgType, '%')) " +
                    "AND (:eventArguments IS NULL OR e.e_args ILIKE concat('%', :eventArguments, '%')) " +
                    "AND (:eventResult IS NULL OR e.e_result ILIKE concat('%', :eventResult, '%')) " +
                    "AND ((:isError = FALSE) OR e.e_error IS NOT NULL) " +
                    "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))")
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime,
                      @Param("serviceId") String serviceId,
                      @Param("calculatedFieldId") UUID calculatedFieldId,
                      @Param("eventEntityId") String eventEntityId,
                      @Param("eventEntityType") String eventEntityType,
                      @Param("msgId") String eventMsgId,
                      @Param("msgType") String eventMsgType,
                      @Param("eventArguments") String eventArguments,
                      @Param("eventResult") String eventResult,
                      @Param("isError") boolean isError,
                      @Param("error") String error);

}
