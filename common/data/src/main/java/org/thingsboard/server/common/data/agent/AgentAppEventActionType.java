// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum AgentAppEventActionType {
    INSTALL,
    UPDATE,
    DELETE,
    RESTART,
    ROLLBACK,
    UPGRADE,
    AGENT_UPGRADE(true);

    private final boolean agentScoped;

    AgentAppEventActionType() {
        this.agentScoped = false;
    }

    AgentAppEventActionType(boolean agentScoped) {
        this.agentScoped = agentScoped;
    }

    @JsonIgnore
    public boolean isAgentScoped() {
        return agentScoped;
    }
}
