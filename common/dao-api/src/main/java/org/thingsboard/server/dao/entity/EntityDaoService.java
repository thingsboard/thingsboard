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
package org.thingsboard.server.dao.entity;

import com.google.common.util.concurrent.FluentFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Optional;

public interface EntityDaoService {

    Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId);

    FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId);

    default long countByTenantId(TenantId tenantId) {
        throw new IllegalArgumentException("Not implemented for " + getEntityType());
    }

    default void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        throw new IllegalArgumentException(getEntityType().getNormalName() + " deletion not supported");
    }

    default void deleteByTenantId(TenantId tenantId) {
        throw new IllegalArgumentException("Deletion by tenant id not supported for " + getEntityType().getNormalName());
    }

    EntityType getEntityType();

}
