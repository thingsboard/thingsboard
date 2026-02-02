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
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import java.util.List;
import java.util.UUID;

public interface StatisticsEventRepository extends EventRepository<StatisticsEventEntity, StatisticsEvent>, JpaRepository<StatisticsEventEntity, UUID> {

    @Override
    @Query(nativeQuery = true,  value = "SELECT * FROM stats_event e WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId ORDER BY e.ts DESC LIMIT :limit")
    List<StatisticsEventEntity> findLatestEvents(@Param("tenantId") UUID tenantId, @Param("entityId") UUID entityId, @Param("limit") int limit);

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
                    "AND (:minMessagesProcessed IS NULL OR e.e_messages_processed >= :minMessagesProcessed) " +
                    "AND (:maxMessagesProcessed IS NULL OR e.e_messages_processed < :maxMessagesProcessed) " +
                    "AND (:minErrorsOccurred IS NULL OR e.e_errors_occurred >= :minErrorsOccurred) " +
                    "AND (:maxErrorsOccurred IS NULL OR e.e_errors_occurred < :maxErrorsOccurred)"
            ,
            countQuery = "SELECT count(*) FROM stats_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:minMessagesProcessed IS NULL OR e.e_messages_processed >= :minMessagesProcessed) " +
                    "AND (:maxMessagesProcessed IS NULL OR e.e_messages_processed < :maxMessagesProcessed) " +
                    "AND (:minErrorsOccurred IS NULL OR e.e_errors_occurred >= :minErrorsOccurred) " +
                    "AND (:maxErrorsOccurred IS NULL OR e.e_errors_occurred < :maxErrorsOccurred)"
    )
    Page<StatisticsEventEntity> findEvents(@Param("tenantId") UUID tenantId,
                                           @Param("entityId") UUID entityId,
                                           @Param("startTime") Long startTime,
                                           @Param("endTime") Long endTime,
                                           @Param("serviceId") String server,
                                           @Param("minMessagesProcessed") Integer minMessagesProcessed,
                                           @Param("maxMessagesProcessed") Integer maxMessagesProcessed,
                                           @Param("minErrorsOccurred") Integer minErrorsOccurred,
                                           @Param("maxErrorsOccurred") Integer maxErrorsOccurred,
                                           Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM StatisticsEventEntity e WHERE " +
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
            value = "DELETE FROM stats_event e WHERE " +
                    "e.tenant_id = :tenantId " +
                    "AND e.entity_id = :entityId " +
                    "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                    "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                    "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                    "AND (:minMessagesProcessed IS NULL OR e.e_messages_processed >= :minMessagesProcessed) " +
                    "AND (:maxMessagesProcessed IS NULL OR e.e_messages_processed < :maxMessagesProcessed) " +
                    "AND (:minErrorsOccurred IS NULL OR e.e_errors_occurred >= :minErrorsOccurred) " +
                    "AND (:maxErrorsOccurred IS NULL OR e.e_errors_occurred < :maxErrorsOccurred)"

    )
    void removeEvents(@Param("tenantId") UUID tenantId,
                      @Param("entityId") UUID entityId,
                      @Param("startTime") Long startTime,
                      @Param("endTime") Long endTime,
                      @Param("serviceId") String server,
                      @Param("minMessagesProcessed") Integer minMessagesProcessed,
                      @Param("maxMessagesProcessed") Integer maxMessagesProcessed,
                      @Param("minErrorsOccurred") Integer minErrorsOccurred,
                      @Param("maxErrorsOccurred") Integer maxErrorsOccurred);
}
