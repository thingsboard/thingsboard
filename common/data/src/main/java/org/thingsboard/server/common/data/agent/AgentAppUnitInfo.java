// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.AgentId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AgentAppUnitInfo extends AgentAppUnit {

    private AgentId agentId;
    private String projectName;

    public AgentAppUnitInfo() {
        super();
    }

    public AgentAppUnitInfo(AgentAppUnit unit, AgentId agentId, String projectName) {
        super(unit);
        this.agentId = agentId;
        this.projectName = projectName;
    }

}
