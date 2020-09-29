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
package org.thingsboard.server.dao.sql.queue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.QueueEntity;

import java.util.List;
import java.util.UUID;

public interface QueueRepository extends CrudRepository<QueueEntity, UUID> {
    QueueEntity findByTenantIdAndTopic(UUID tenantId, String topic);

    QueueEntity findByTenantIdAndName(UUID tenantId, String name);

    List<QueueEntity> findByTenantId(UUID tenantId);

    @Query("SELECT q FROM QueueEntity q WHERE q.tenantId = :tenantId " +
            "AND LOWER(q.name) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<QueueEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                     @Param("textSearch") String textSearch,
                                     Pageable pageable);

    List<QueueEntity> findAllByName(String name);
}