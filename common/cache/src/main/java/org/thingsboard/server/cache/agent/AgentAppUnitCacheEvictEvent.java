// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.agent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@RequiredArgsConstructor
public class AgentAppUnitCacheEvictEvent {

    private final AgentAppUnitId agentAppUnitId;
    private final TenantId tenantId;
    private final AgentId agentId;
    private final String projectName;
    private final String identifier;
    private final AgentAppUnitType type;
    private final boolean deleted;

}
