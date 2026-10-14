// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.edge;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class EdgeProfileCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 8149368955940090989L;

    private final TenantId tenantId;
    private final EdgeProfileId id;
    private final String name;
    private final boolean defaultProfile;

    private EdgeProfileCacheKey(TenantId tenantId, EdgeProfileId id, String name, boolean defaultProfile) {
        this.tenantId = tenantId;
        this.id = id;
        this.name = name;
        this.defaultProfile = defaultProfile;
    }

    public static EdgeProfileCacheKey forId(TenantId tenantId, EdgeProfileId id) {
        return new EdgeProfileCacheKey(tenantId, id, null, false);
    }

    public static EdgeProfileCacheKey forName(TenantId tenantId, String name) {
        return new EdgeProfileCacheKey(tenantId, null, name, false);
    }

    public static EdgeProfileCacheKey forDefaultProfile(TenantId tenantId) {
        return new EdgeProfileCacheKey(tenantId, null, null, true);
    }

    // Three distinct string shapes so no two key forms can collide.
    @Override
    public String toString() {
        if (defaultProfile) {
            return tenantId + "_default";
        }
        if (id != null) {
            return tenantId + "_id_" + id;
        }
        return tenantId + "_name_" + name;
    }

}
