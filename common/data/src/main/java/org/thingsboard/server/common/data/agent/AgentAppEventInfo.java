// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Schema
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class AgentAppEventInfo extends AgentAppEvent {

    @Schema(description = "Name of the Agent that owns the Application.", accessMode = Schema.AccessMode.READ_ONLY)
    private String agentName;

    public AgentAppEventInfo() {
        super();
    }

    public AgentAppEventInfo(AgentAppEvent event, String agentName) {
        super(event);
        this.agentName = agentName;
    }
}
