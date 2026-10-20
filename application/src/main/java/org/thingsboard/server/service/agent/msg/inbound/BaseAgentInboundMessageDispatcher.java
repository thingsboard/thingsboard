// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

import java.util.List;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class BaseAgentInboundMessageDispatcher implements AgentInboundMessageDispatcher {

    private final List<AgentInboundMessageHandler> agentInboundMessageHandlerList;

    @Override
    public void process(AgentInboundMsgCtx ctx) {
        boolean handled = false;
        for (AgentInboundMessageHandler handler : agentInboundMessageHandlerList) {
            if (handler.canHandle(ctx)) {
                handled = true;
                handleSafely(handler, ctx);
            }
        }
        if (!handled) {
            log.warn("[{}] No inbound handler matched message of type {}, dropping it",
                    ctx.sessionState().getAgentId(), ctx.msg().getMsgCase());
        }
    }

    private void handleSafely(AgentInboundMessageHandler handler, AgentInboundMsgCtx ctx) {
        try {
            handler.handle(ctx);
        } catch (Exception e) {
            log.error("[{}] Handler {} failed to process inbound message, dropping it to keep the stream alive",
                    ctx.sessionState().getAgentId(), handler.getClass().getSimpleName(), e);
        }
    }
}
