// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentFinalizeClaimService;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.session.AgentSessionState;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class FinalizeClaimMessageHandler implements AgentInboundMessageHandler {

    private final AgentFinalizeClaimService claimService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasFinalizeClaim();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        AgentSessionState state = msgCtx.sessionState();
        claimService.onFinalizeClaim(state.getAgent().getTenantId(), state.getAgentId(), msgCtx.msg().getFinalizeClaim());
    }
}
