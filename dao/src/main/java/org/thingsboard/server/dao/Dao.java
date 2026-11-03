// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import com.google.common.util.concurrent.ListenableFuture;
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
