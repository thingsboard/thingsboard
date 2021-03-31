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
            "AND e.event_type = :eventType " +
            "AND (:startTime = 0 OR e.created_time >= :startTime) " +
            "AND (:endTime = 0 OR e.created_time <= :endTime) " +
            "AND jsonb_contains(cast(e.body AS jsonb), cast(:jsonFilter AS jsonb)) " +
            "AND (:dataSearch IS NULL OR cast(json_object_field(cast(body AS json), 'data') AS VARCHAR) " +
                    "LIKE concat('%', :dataSearch, '%')) " +
            "AND (:metadataSearch IS NULL OR cast(json_object_field(cast(body AS json), 'metadata') AS VARCHAR)" +
                    "LIKE concat('%', :metadataSearch, '%')) ",
            countQuery = "SELECT count(*) FROM event e WHERE " +
            "e.tenant_id = :tenantId " +
            "AND e.entity_type = :entityType " +
            "AND e.entity_id = :entityId " +
            "AND e.event_type = :eventType " +
            "AND (:startTime = 0 OR e.created_time >= :startTime) " +
            "AND (:endTime = 0 OR e.created_time <= :endTime) " +
            "AND jsonb_contains(cast(e.body AS jsonb), cast(:jsonFilter AS jsonb)) " +
            "AND (:dataSearch IS NULL OR cast(json_object_field(cast(body AS json), 'data') AS VARCHAR) " +
            "LIKE concat('%', :dataSearch, '%')) " +
            "AND (:metadataSearch IS NULL OR cast(json_object_field(cast(body AS json), 'metadata') AS VARCHAR)" +
            "LIKE concat('%', :metadataSearch, '%')) ")
    Page<EventEntity> findEventsByTenantIdAndEntityIdAndEventTypeByFilters(@Param("tenantId") UUID tenantId,
                                                                  @Param("entityType") String entityType,
                                                                  @Param("entityId") UUID entityId,
                                                                  @Param("eventType") String eventType,
                                                                  @Param("startTime") Long startTime,
                                                                  @Param("endTime") Long endTime,
                                                                  @Param("jsonFilter") String jsonFilter,
                                                                  @Param("dataSearch") String dataSearch,
                                                                  @Param("metadataSearch") String metadataSearch,
                                                                  Pageable pageable);

}
