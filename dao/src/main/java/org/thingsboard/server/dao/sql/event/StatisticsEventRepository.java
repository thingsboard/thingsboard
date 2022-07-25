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
import org.thingsboard.server.dao.model.sql.LifecycleEventEntity;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
public interface StatisticsEventRepository extends JpaRepository<StatisticsEventEntity, UUID> {

    @Query("SELECT e FROM StatisticsEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.entityId = :entityId " +
            "AND (:startTime IS NULL OR e.ts >= :startTime) " +
            "AND (:endTime IS NULL OR e.ts <= :endTime)"
    )
    Page<StatisticsEventEntity> findEventsWithoutFilter(@Param("tenantId") UUID tenantId,
                                                  @Param("entityId") UUID entityId,
                                                  @Param("startTime") Long startTime,
                                                  @Param("endTime") Long endTime,
                                                  Pageable pageable);

}
