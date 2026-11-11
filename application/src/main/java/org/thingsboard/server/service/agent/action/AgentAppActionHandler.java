// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.action;

import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;

public interface AgentAppActionHandler {

    AgentAppEventActionType getActionType();

    void handle(AgentApplication application, AgentAppEventRequest request, AgentAppActionContext ctx);
}
