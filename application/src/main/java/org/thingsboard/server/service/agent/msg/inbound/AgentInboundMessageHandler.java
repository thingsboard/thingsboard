// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

public interface AgentInboundMessageHandler {

    boolean canHandle(AgentInboundMsgCtx msgCtx);
    void handle(AgentInboundMsgCtx msgCtx);
}
