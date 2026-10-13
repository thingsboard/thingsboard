// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.log.AgentLogFanout;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentLogChunkMessageHandler implements AgentInboundMessageHandler {

    private final AgentLogFanout logFanout;
    private final RateLimitService rateLimitService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasLogChunk();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        AgentLogChunk chunk = msgCtx.msg().getLogChunk();
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] LogChunk unit={} lines={} dropped={}",
                    tenantId, agentId, chunk.getUnitId(), chunk.getLinesCount(), chunk.getDropped());
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS, tenantId)
                || !rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS_PER_AGENT, tenantId, agentId)) {
            log.debug("[{}][{}] Dropping log chunk for unit {}, rate limit reached", tenantId, agentId, chunk.getUnitId());
            return;
        }
        logFanout.fanout(msgCtx.sessionState(), chunk);
    }
}
