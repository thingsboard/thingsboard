// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum AgentAppStepType {
    COMPOSE_TEMPLATE,
    COMPOSE(true),
    COMPOSE_START,
    COMPOSE_DOWN,
    ROLLBACK,
    BACKUP_VOLUME,
    BACKUP_VOLUME_REMOVE,
    COMPOSE_RESTART,
    RUN_JOB(true),
    AGENT_PREPARE,
    AGENT_FINALIZE
    ;

    private final boolean containsCustomArguments;

    AgentAppStepType() {
        this.containsCustomArguments = false;
    }

    AgentAppStepType(boolean containsCustomArguments) {
        this.containsCustomArguments = containsCustomArguments;
    }

    @JsonIgnore
    public boolean containsCustomArguments() {
        return containsCustomArguments;
    }
}

