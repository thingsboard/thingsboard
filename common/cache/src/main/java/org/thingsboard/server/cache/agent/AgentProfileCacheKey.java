// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class AgentProfileCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final TenantId tenantId;
    private final String name;
    private final boolean defaultProfile;

    private AgentProfileCacheKey(TenantId tenantId, String name, boolean defaultProfile) {
        this.tenantId = tenantId;
        this.name = name;
        this.defaultProfile = defaultProfile;
    }

    public static AgentProfileCacheKey forName(TenantId tenantId, String name) {
        return new AgentProfileCacheKey(tenantId, name, false);
    }

    public static AgentProfileCacheKey forDefaultProfile(TenantId tenantId) {
        return new AgentProfileCacheKey(tenantId, null, true);
    }

    /**
     * IMPORTANT: toString() must return a value that cannot collide with another key form.
     * The default-profile case returns the bare tenantId so it can never equal the
     * "tenantId_name" form (which always carries a "_name" suffix), even for a profile named "default".
     */
    @Override
    public String toString() {
        if (defaultProfile) {
            return tenantId.toString();
        }
        return tenantId + "_" + name;
    }
}
