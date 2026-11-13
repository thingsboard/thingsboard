// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.io.Serializable;

/**
 * Dual-index cache key: a unit is cached under both its {@link AgentAppUnitId} and its
 * (tenantId, agentId, projectName, identifier, type) natural key. The type is part of the natural key
 * because compose namespaces services/volumes/networks separately, so the same identifier can exist as
 * several units of one application. Because the two forms hash differently, an eviction must be issued
 * under BOTH key forms to fully invalidate an entry.
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AgentAppUnitCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 433030109879120547L;

    private final AgentAppUnitId agentAppUnitId;

    private final TenantId tenantId;
    private final AgentId agentId;
    private final String projectName;
    private final String identifier;
    private final AgentAppUnitType type;

    public static AgentAppUnitCacheKey from(AgentAppUnitId id) {
        return new AgentAppUnitCacheKey(id, null, null, null, null, null);
    }

    public static AgentAppUnitCacheKey from(AgentAppUnitCacheEvictEvent event) {
        return from(event.getTenantId(), event.getAgentId(), event.getProjectName(), event.getIdentifier(), event.getType());
    }

    public static AgentAppUnitCacheKey from(TenantId tenantId, AgentId agentId, String projectName,
                                            String identifier, AgentAppUnitType type) {
        return new AgentAppUnitCacheKey(null, tenantId, agentId, projectName, identifier, type);
    }

    @Override
    public String toString() {
        return agentAppUnitId != null
                ? agentAppUnitId.toString()
                : tenantId + ":" + agentId + ":" + projectName + ":" + identifier + ":" + type;
    }
}
