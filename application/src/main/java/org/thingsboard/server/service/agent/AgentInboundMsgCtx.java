// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import lombok.Builder;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionState;

@Builder
public record AgentInboundMsgCtx(
        AgentSession session,
        AgentToServer msg) {

    public AgentSessionState sessionState() {
        return session.getState();
    }
}
