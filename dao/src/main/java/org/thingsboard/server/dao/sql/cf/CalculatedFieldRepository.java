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
package org.thingsboard.server.dao.sql.cf;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.dao.model.sql.CalculatedFieldEntity;

import java.util.List;
import java.util.UUID;

public interface CalculatedFieldRepository extends JpaRepository<CalculatedFieldEntity, UUID> {

    boolean existsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    CalculatedFieldEntity findByEntityIdAndName(UUID entityId, String name);

    List<CalculatedFieldId> findCalculatedFieldIdsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    List<CalculatedFieldEntity> findAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    Page<CalculatedFieldEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT cf FROM CalculatedFieldEntity cf WHERE cf.tenantId = :tenantId " +
            "AND cf.entityId = :entityId " +
            "AND (:textSearch IS NULL OR ilike(cf.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<CalculatedFieldEntity> findAllByTenantIdAndEntityId(UUID tenantId, UUID entityId, String textSearch, Pageable pageable);

    List<CalculatedFieldEntity> findAllByTenantId(UUID tenantId);

    List<CalculatedFieldEntity> removeAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    long countByTenantIdAndEntityId(UUID tenantId, UUID entityId);

}
