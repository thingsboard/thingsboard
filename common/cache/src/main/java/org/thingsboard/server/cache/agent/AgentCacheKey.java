// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.AgentId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class AgentCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 7657100143498569699L;

    private final AgentId agentId;
    private final String routingKey;

    private AgentCacheKey(AgentId agentId, String routingKey) {
        this.agentId = agentId;
        this.routingKey = routingKey;
    }

    public static AgentCacheKey forId(AgentId agentId) {
        return new AgentCacheKey(agentId, null);
    }

    public static AgentCacheKey forRoutingKey(String routingKey) {
        return new AgentCacheKey(null, routingKey);
    }

    /**
     * IMPORTANT: toString() must return a value that cannot collide with the other key form. Both forms are
     * prefixed, so a routing key that happens to look like a UUID can never address the by-id entry.
     */
    @Override
    public String toString() {
        return agentId != null ? "id_" + agentId.getId() : "rk_" + routingKey;
    }
}
