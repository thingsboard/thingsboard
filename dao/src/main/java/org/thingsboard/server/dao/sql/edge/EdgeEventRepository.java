/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;

import java.util.UUID;

public interface EdgeEventRepository extends PagingAndSortingRepository<EdgeEventEntity, UUID>, JpaSpecificationExecutor<EdgeEventEntity> {

    @Query("SELECT e FROM EdgeEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.edgeId = :edgeId " +
            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) "
    )
    Page<EdgeEventEntity> findEdgeEventsByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                            @Param("edgeId") UUID edgeId,
                                                            @Param("startTime") Long startTime,
                                                            @Param("startTime") Long endTime,
                                                            Pageable pageable);
}
