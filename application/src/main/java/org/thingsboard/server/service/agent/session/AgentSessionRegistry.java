// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.session;

import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@TbCoreComponent
public class AgentSessionRegistry {

    private final ConcurrentHashMap<AgentId, AgentSession> sessionsByAgentId = new ConcurrentHashMap<>();

    public void registerOrReplace(AgentId agentId, TenantId tenantId, AgentSession session) {
        AgentSession old = sessionsByAgentId.put(agentId, session);
        if (old != null) {
            log.info("[{}] Replacing old session for agent [{}]", tenantId, agentId);
            old.onError(Status.ABORTED.withDescription("Session replaced by a newer connection for agentId=" + agentId));
        }
    }

    public boolean removeIfSame(AgentSession session) {
        if (session == null) {
            log.warn("Can't remove session from holder because it's null");
            return false;
        }
        return sessionsByAgentId.remove(session.getState().getAgentId(), session);
    }


    public boolean hasSession(AgentId agentId) {
        return sessionsByAgentId.containsKey(agentId);
    }

    public AgentSession getByAgentId(AgentId agentId) {
        return sessionsByAgentId.get(agentId);
    }
}
