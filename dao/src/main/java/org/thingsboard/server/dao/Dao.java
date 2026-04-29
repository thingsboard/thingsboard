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
package org.thingsboard.server.dao;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface Dao<T> {

    List<T> find(TenantId tenantId);

    T findById(TenantId tenantId, UUID id);

    ListenableFuture<T> findByIdAsync(TenantId tenantId, UUID id);

    default List<EntityInfo> findEntityInfosByNamePrefix(TenantId tenantId, String name) {
        throw new UnsupportedOperationException();
    }

    boolean existsById(TenantId tenantId, UUID id);

    ListenableFuture<Boolean> existsByIdAsync(TenantId tenantId, UUID id);

    T save(TenantId tenantId, T t);

    T saveAndFlush(TenantId tenantId, T t);

    void removeById(TenantId tenantId, UUID id);

    void removeAllByIds(Collection<UUID> ids);

    List<UUID> findIdsByTenantIdAndIdOffset(TenantId tenantId, UUID idOffset, int limit);

    default List<? extends EntityFields> findNextBatch(UUID id, int batchSize) {
        throw new UnsupportedOperationException();
    }

    default EntityType getEntityType() {
        return null;
    }

}
