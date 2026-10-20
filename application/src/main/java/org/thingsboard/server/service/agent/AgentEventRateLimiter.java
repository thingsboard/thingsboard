// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AgentEventRateLimiter {

    private final RateLimitService rateLimitService;

    public void checkOrThrow(TenantId tenantId, AgentId agentId) {
        if (!rateLimitService.checkRateLimit(LimitedApi.AGENT_EVENTS, tenantId)) {
            throw new TbRateLimitsException(EntityType.TENANT);
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.AGENT_EVENTS_PER_AGENT, tenantId, agentId)) {
            throw new TbRateLimitsException(EntityType.AGENT);
        }
    }
}
