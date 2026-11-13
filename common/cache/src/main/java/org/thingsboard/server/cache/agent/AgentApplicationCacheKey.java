// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.AgentApplicationId;

import java.io.Serial;
import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class AgentApplicationCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 8657100143498569699L;

    private final AgentApplicationId agentApplicationId;

    public static AgentApplicationCacheKey from(AgentApplicationId id) {
        return new AgentApplicationCacheKey(id);
    }

    @Override
    public String toString() {
        return agentApplicationId.toString();
    }
}
