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
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = :eventType " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:entityName IS NULL OR lower(json_body->>'entityName') LIKE concat('%', lower(:entityName\\:\\:varchar), '%')) " +
                    "AND (:relationType IS NULL OR lower(json_body->>'relationType') LIKE concat('%', lower(:relationType\\:\\:varchar), '%')) " +
                    "AND (:bodyEntityId IS NULL OR lower(json_body->>'entityId') LIKE concat('%', lower(:bodyEntityId\\:\\:varchar), '%')) " +
                    "AND (:msgType IS NULL OR lower(json_body->>'msgType') LIKE concat('%', lower(:msgType\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%')) " +
                    "AND (:data IS NULL OR lower(json_body->>'data') LIKE concat('%', lower(:data\\:\\:varchar), '%')) " +
                    "AND (:metadata IS NULL OR lower(json_body->>'metadata') LIKE concat('%', lower(:metadata\\:\\:varchar), '%')) ",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = :eventType " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:type IS NULL OR lower(json_body->>'type') LIKE concat('%', lower(:type\\:\\:varchar), '%')) " +
                    "AND (:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:entityName IS NULL OR lower(json_body->>'entityName') LIKE concat('%', lower(:entityName\\:\\:varchar), '%')) " +
                    "AND (:relationType IS NULL OR lower(json_body->>'relationType') LIKE concat('%', lower(:relationType\\:\\:varchar), '%')) " +
                    "AND (:bodyEntityId IS NULL OR lower(json_body->>'entityId') LIKE concat('%', lower(:bodyEntityId\\:\\:varchar), '%')) " +
                    "AND (:msgType IS NULL OR lower(json_body->>'msgType') LIKE concat('%', lower(:msgType\\:\\:varchar), '%')) " +
                    "AND ((:isError = FALSE) OR (json_body->>'error') IS NOT NULL) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%')) " +
                    "AND (:data IS NULL OR lower(json_body->>'data') LIKE concat('%', lower(:data\\:\\:varchar), '%')) " +
                    "AND (:metadata IS NULL OR lower(json_body->>'metadata') LIKE concat('%', lower(:metadata\\:\\:varchar), '%'))"
    )
    Page<EventEntity> findDebugRuleNodeEvents(@Param("tenantId") UUID tenantId,
                                              @Param("entityId") UUID entityId,
                                              @Param("entityType") String entityType,
                                              @Param("eventType") String eventType,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTime,
                                              @Param("type") String type,
                                              @Param("server") String server,
                                              @Param("entityName") String entityName,
                                              @Param("relationType") String relationType,
                                              @Param("bodyEntityId") String bodyEntityId,
                                              @Param("msgType") String msgType,
                                              @Param("isError") boolean isError,
                                              @Param("error") String error,
                                              @Param("data") String data,
                                              @Param("metadata") String metadata,
                                              Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'ERROR' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:method IS NULL OR lower(json_body->>'method') LIKE concat('%', lower(:method\\:\\:varchar), '%')) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'ERROR' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:method IS NULL OR lower(json_body->>'method') LIKE concat('%', lower(:method\\:\\:varchar), '%')) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))")
    Page<EventEntity> findErrorEvents(@Param("tenantId") UUID tenantId,
                                      @Param("entityId") UUID entityId,
                                      @Param("entityType") String entityType,
                                      @Param("startTime") Long startTime,
                                      @Param("endTime") Long endTIme,
                                      @Param("server") String server,
                                      @Param("method") String method,
                                      @Param("error") String error,
                                      Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:event IS NULL OR lower(json_body->>'event') LIKE concat('%', lower(:event\\:\\:varchar), '%')) " +
                    "AND ((:statusFilterEnabled = FALSE) OR lower(json_body->>'success')\\:\\:boolean = :statusFilter) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))"
            ,
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(json_body->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:event IS NULL OR lower(json_body->>'event') LIKE concat('%', lower(:event\\:\\:varchar), '%')) " +
                    "AND ((:statusFilterEnabled = FALSE) OR lower(json_body->>'success')\\:\\:boolean = :statusFilter) " +
                    "AND (:error IS NULL OR lower(json_body->>'error') LIKE concat('%', lower(:error\\:\\:varchar), '%'))"
    )
    Page<EventEntity> findLifeCycleEvents(@Param("tenantId") UUID tenantId,
                                          @Param("entityId") UUID entityId,
                                          @Param("entityType") String entityType,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTIme,
                                          @Param("server") String server,
                                          @Param("event") String event,
                                          @Param("statusFilterEnabled") boolean statusFilterEnabled,
                                          @Param("statusFilter") boolean statusFilter,
                                          @Param("error") String error,
                                          Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT e.id, e.created_time, e.body, e.entity_id, e.entity_type, e.event_type, e.event_uid, e.tenant_id, ts  FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'STATS' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(e.body\\:\\:json->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:messagesProcessed = 0 OR (json_body->>'messagesProcessed')\\:\\:integer >= :messagesProcessed) " +
                    "AND (:errorsOccurred = 0 OR (json_body->>'errorsOccurred')\\:\\:integer >= :errorsOccurred) ",
            countQuery = "SELECT count(*) FROM " +
                    "(SELECT *, e.body\\:\\:jsonb as json_body FROM event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_type = :entityType " +
                    "AND e.entity_id = :entityId " +
                    "AND e.event_type = 'LC_EVENT' " +
                    "AND e.created_time >= :startTime AND (:endTime = 0 OR e.created_time <= :endTime) " +
                    ") AS e WHERE " +
                    "(:server IS NULL OR lower(e.body\\:\\:json->>'server') LIKE concat('%', lower(:server\\:\\:varchar), '%')) " +
                    "AND (:messagesProcessed = 0 OR (json_body->>'messagesProcessed')\\:\\:integer >= :messagesProcessed) " +
                    "AND (:errorsOccurred = 0 OR (json_body->>'errorsOccurred')\\:\\:integer >= :errorsOccurred) ")
    Page<EventEntity> findStatisticsEvents(@Param("tenantId") UUID tenantId,
                                           @Param("entityId") UUID entityId,
                                           @Param("entityType") String entityType,
                                           @Param("startTime") Long startTime,
                                           @Param("endTime") Long endTIme,
                                           @Param("server") String server,
                                           @Param("messagesProcessed") Integer messagesProcessed,
                                           @Param("errorsOccurred") Integer errorsOccurred,
                                           Pageable pageable);

}
