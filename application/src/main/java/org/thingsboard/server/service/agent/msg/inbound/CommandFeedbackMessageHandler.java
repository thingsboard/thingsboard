// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.CommandFeedbackHandler;
import org.thingsboard.server.service.agent.session.AgentSessionState;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class CommandFeedbackMessageHandler implements AgentInboundMessageHandler {

    private final CommandFeedbackHandler commandFeedbackHandler;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        AgentToServer msg = msgCtx.msg();
        return msg.hasCommandAck() || msg.hasProgress() || msg.hasResult();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        AgentToServer msg = msgCtx.msg();
        AgentSessionState state = msgCtx.sessionState();
        AgentId agentId = state.getAgentId();
        TenantId tenantId = state.getAgent().getTenantId();

        if (msg.hasCommandAck()) {
            commandFeedbackHandler.onCommandAck(tenantId, agentId, msg.getCommandAck());
        } else if (msg.hasProgress()) {
            commandFeedbackHandler.onCommandProgress(tenantId, agentId, msg.getProgress());
        } else if (msg.hasResult()) {
            commandFeedbackHandler.onCommandResult(tenantId, agentId, msg.getResult());
        }
    }
}
