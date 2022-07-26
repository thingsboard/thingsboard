/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import java.util.List;
import java.util.UUID;

public interface StatisticsEventRepository extends EventRepository<StatisticsEventEntity, StatisticsEvent>, JpaRepository<StatisticsEventEntity, UUID> {

    @Override
    @Query("SELECT e FROM LifecycleEventEntity e WHERE e.tenantId = :tenantId AND e.entityId = :entityId ORDER BY e.ts DESC")
    List<StatisticsEventEntity> findLatestEvents(UUID tenantId, UUID entityId, int limit);

    @Query("SELECT e FROM StatisticsEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    Page<StatisticsEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                           @Param("entityId") UUID entityId,
                                           @Param("startTime") Long startTime,
                                           @Param("endTime") Long endTime,
                                           Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT * FROM stats_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:messagesProcessed IS NULL OR e.e_messages_processed >= :messagesProcessed) " +
                    "AND (:errorsOccurred IS NULL OR e.e_errors_occurred >= :errorsOccurred)"
            ,
            countQuery = "SELECT count(*) FROM stats_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:messagesProcessed IS NULL OR e.e_messages_processed >= :messagesProcessed) " +
                    "AND (:errorsOccurred IS NULL OR e.e_errors_occurred >= :errorsOccurred)"
    )
    Page<StatisticsEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                           @Param("entityId") UUID entityId,
                                           @Param("startTime") Long startTime,
                                           @Param("endTime") Long endTime,
                                           @Param("serviceId") String server,
                                           @Param("messagesProcessed") Integer messagesProcessed,
                                           @Param("errorsOccurred") Integer errorsOccurred,
                                           Pageable pageable);

}
