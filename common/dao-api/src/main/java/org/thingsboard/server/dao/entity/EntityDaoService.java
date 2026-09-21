// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
