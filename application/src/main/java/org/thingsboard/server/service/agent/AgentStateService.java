// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import org.thingsboard.server.common.data.agent.Agent;

public interface AgentStateService {

    void onAgentConnect(Agent agent, long lastConnectTime, String agentVersion, String agentContainerId);

    void onAgentDisconnect(Agent agent, long lastDisconnectTime);

}
