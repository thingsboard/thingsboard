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
package org.thingsboard.server.dao.entityview;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@Getter
@EqualsAndHashCode
@Builder
public class EntityViewCacheKey implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 5986277528222738163L;

    private final TenantId tenantId;
    private final String name;
    private final EntityId entityId;
    private final EntityViewId entityViewId;

    private EntityViewCacheKey(TenantId tenantId, String name, EntityId entityId, EntityViewId entityViewId) {
        this.tenantId = tenantId;
        this.name = name;
        this.entityId = entityId;
        this.entityViewId = entityViewId;
    }

    public static EntityViewCacheKey byName(TenantId tenantId, String name) {
        return new EntityViewCacheKey(tenantId, name, null, null);
    }

    public static EntityViewCacheKey byEntityId(TenantId tenantId, EntityId entityId) {
        return new EntityViewCacheKey(tenantId, null, entityId, null);
    }

    public static EntityViewCacheKey byId(EntityViewId id) {
        return new EntityViewCacheKey(null, null, null, id);
    }

    @Override
    public String toString() {
        if (entityViewId != null) {
            return entityViewId.toString();
        } else if (entityId != null) {
            return tenantId + "_" + entityId;
        } else {
            return tenantId + "_n_" + name;
        }
    }

    @Override
    public boolean isVersioned() {
        return entityViewId != null;
    }

}
