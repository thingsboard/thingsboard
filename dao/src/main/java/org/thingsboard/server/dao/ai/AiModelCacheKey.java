// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ai;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

record AiModelCacheKey(UUID tenantId, UUID modelId) implements VersionedCacheKey {

    AiModelCacheKey {
        requireNonNull(tenantId);
        requireNonNull(modelId);

        if (TenantId.SYS_TENANT_ID.getId().equals(tenantId)) {
            throw new IllegalArgumentException("Tenant ID must not be the system tenant ID");
        }
        if (EntityId.NULL_UUID.equals(modelId)) {
            throw new IllegalArgumentException("Model ID must not be reserved null UUID");
        }
    }

    static AiModelCacheKey of(TenantId tenantId, AiModelId modelId) {
        return new AiModelCacheKey(tenantId.getId(), modelId.getId());
    }

    @Override
    public boolean isVersioned() {
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return /* cache name */ "_" + tenantId + "_" + modelId;
    }

}
