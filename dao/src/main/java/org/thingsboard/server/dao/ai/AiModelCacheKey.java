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
