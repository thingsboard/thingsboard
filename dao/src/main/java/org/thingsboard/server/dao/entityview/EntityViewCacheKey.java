// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
