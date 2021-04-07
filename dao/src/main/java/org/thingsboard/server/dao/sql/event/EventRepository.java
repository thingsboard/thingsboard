/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EventEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
public interface EventRepository extends PagingAndSortingRepository<EventEntity, UUID> {

    EventEntity findByTenantIdAndEntityTypeAndEntityIdAndEventTypeAndEventUid(UUID tenantId,
                                                                              EntityType entityType,
                                                                              UUID entityId,
                                                                              String eventType,
                                                                              String eventUid);

    EventEntity findByTenantIdAndEntityTypeAndEntityId(UUID tenantId,
                                                       EntityType entityType,
                                                       UUID entityId);

    @Query("SELECT e FROM EventEntity e WHERE e.tenantId = :tenantId AND e.entityType = :entityType " +
            "AND e.entityId = :entityId AND e.eventType = :eventType ORDER BY e.eventType DESC, e.id DESC")
    List<EventEntity> findLatestByTenantIdAndEntityTypeAndEntityIdAndEventType(
                                                    @Param("tenantId") UUID tenantId,
                                                    @Param("entityType") EntityType entityType,
                                                    @Param("entityId") UUID entityId,
                                                    @Param("eventType") String eventType,
                                                    Pageable pageable);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityType = :entityType AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) " +
            "AND LOWER(e.eventType) LIKE LOWER(CONCAT(:textSearch, '%'))"
    )
    Page<EventEntity> findEventsByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                                      @Param("entityType") EntityType entityType,
                                                      @Param("entityId") UUID entityId,
                                                      @Param("textSearch") String textSearch,
                                                      @Param("startTime") Long startTime,
                                                      @Param("endTime") Long endTime,
                                                      Pageable pageable);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityType = :entityType AND e.entityId = :entityId " +
            "AND e.eventType = :eventType " +
            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime)"
    )
    Page<EventEntity> findEventsByTenantIdAndEntityIdAndEventType(@Param("tenantId") UUID tenantId,
                                                                  @Param("entityType") EntityType entityType,
                                                                  @Param("entityId") UUID entityId,
                                                                  @Param("eventType") String eventType,
                                                                  @Param("startTime") Long startTime,
                                                                  @Param("endTime") Long endTime,
                                                                  Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'DEBUG_RULE_NODE' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'type') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:type AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'server') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:server AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'entityName') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:entityName AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'relationType') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:relationType AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'msgId') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:messageId AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'msgType') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:messageType AS VARCHAR)), '%')) " +
                    "AND :isError = FALSE OR (json_object_field(cast(e.body as json), 'error') is not null)" +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'data') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:dataSearch AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'metadata') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:metadataSearch AS VARCHAR)), '%')) ",
            countQuery = "SELECT count(*) FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'DEBUG_RULE_NODE' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'type') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:type AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'server') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:server AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'entityName') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:entityName AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'relationType') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:relationType AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'msgId') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:messageId AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'msgType') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:messageType AS VARCHAR)), '%')) " +
                    "AND :isError = false OR (json_object_field(cast(e.body as json), 'error') is not null) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'data') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:dataSearch AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'metadata') as varchar)) " +
                    "LIKE  concat('%', lower(cast(:metadataSearch AS VARCHAR)), '%')) "
    )
    Page<EventEntity> findDebugRuleNodeEvents(@Param("tenantId") UUID tenantId,
                                              @Param("entityId") UUID entityId,
                                              @Param("entityType") String entityType,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTIme,
                                              @Param("type") String type,
                                              @Param("server") String server,
                                              @Param("entityName") String entityName,
                                              @Param("relationType") String relationType,
                                              @Param("messageId") String messageId,
                                              @Param("messageType") String messageType,
                                              @Param("isError") boolean isError,
                                              @Param("dataSearch") String dataSearch,
                                              @Param("metadataSearch") String metadataSearch,
                                              Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'ERROR' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body as json), 'server') as varchar )) " +
                    "LIKE concat('%', lower(cast(:server as varchar)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body as json), 'method') as varchar )) " +
                    "LIKE concat('%', lower(cast(:method as varchar)), '%')) ",
            countQuery = "SELECT count(*) FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'ERROR' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body as json), 'server') as varchar )) " +
                    "LIKE concat('%', lower(cast(:server as varchar)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body as json), 'method') as varchar )) " +
                    "LIKE concat('%', lower(cast(:method as varchar)), '%')) ")
    Page<EventEntity> findErrorEvents(@Param("tenantId") UUID tenantId,
                                      @Param("entityId") UUID entityId,
                                      @Param("entityType") String entityType,
                                      @Param("startTime") Long startTime,
                                      @Param("endTime") Long endTIme,
                                      @Param("server") String server,
                                      @Param("method") String method,
                                      Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'server') AS VARCHAR )) " +
                    "LIKE concat('%', lower(cast(:server AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'event') AS VARCHAR )) " +
                    "LIKE concat('%', lower(cast(:event AS VARCHAR)), '%')) " +
                    "AND :isError = FALSE OR cast(jsonb_object_field(cast(e.body AS jsonb), 'success') as boolean) = FALSE",
            countQuery = "SELECT count(*) FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'server') AS VARCHAR )) " +
                    "LIKE concat('%', lower(cast(:server AS VARCHAR)), '%')) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'event') AS VARCHAR )) " +
                    "LIKE concat('%', lower(cast(:event AS VARCHAR)), '%')) " +
                    "AND :isError = FALSE OR cast(jsonb_object_field(cast(e.body AS jsonb), 'success') AS boolean) = FALSE")
    Page<EventEntity> findLifeCycleEvents(@Param("tenantId") UUID tenantId,
                                          @Param("entityId") UUID entityId,
                                          @Param("entityType") String entityType,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTIme,
                                          @Param("isError") boolean isError,
                                          @Param("event") String event,
                                          @Param("server") String server,
                                          Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'STATS' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'server') AS VARCHAR )) " +
                    "LIKE concat('%', lower(cast(:server AS VARCHAR)), '%')) " +
                    "AND (:messagesProcessed = 0 OR cast(jsonb_object_field(cast(e.body AS jsonb), 'messagesProcessed') AS INTEGER) = :messagesProcessed)" +
                    "AND (:errorsOccured = 0 OR cast(jsonb_object_field(cast(e.body AS jsonb), 'errorsOccured') AS INTEGER) = :errorsOccured)",
            countQuery = "SELECT count(*) FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'STATS' " +
                    "AND (:startTime = 0 OR e.created_time >= :startTime) " +
                    "AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    "AND (lower(cast(json_object_field(cast(e.body AS json), 'server') AS VARCHAR )) " +
                    "LIKE concat('%', lower(cast(:server AS VARCHAR)), '%')) " +
                    "AND (:messagesProcessed = 0 OR cast(jsonb_object_field(cast(e.body AS jsonb), 'messagesProcessed') AS INTEGER) = :messagesProcessed)" +
                    "AND (:errorsOccured = 0 OR cast(jsonb_object_field(cast(e.body AS jsonb), 'errorsOccured') AS INTEGER) = :errorsOccured)")
    Page<EventEntity> findStatisticsEvents(@Param("tenantId") UUID tenantId,
                                           @Param("entityId") UUID entityId,
                                           @Param("entityType") String entityType,
                                           @Param("startTime") Long startTime,
                                           @Param("endTime") Long endTIme,
                                           @Param("server") String server,
                                           @Param("messagesProcessed") int messagesProcessed,
                                           @Param("errorsOccured") int errorsOccured,
                                           Pageable pageable);

}
