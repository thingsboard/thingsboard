// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.id.AgentId;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAppUnitInfoEntity extends AgentAppUnitEntity {

    private UUID agentId;
    private String projectName;

    public AgentAppUnitInfoEntity() {
        super();
    }

    public AgentAppUnitInfoEntity(AgentAppUnitEntity unitEntity, UUID agentId, String projectName) {
        super(unitEntity);
        this.agentId = agentId;
        this.projectName = projectName;
    }

    @Override
    public AgentAppUnitInfo toData() {
        AgentAppUnit unit = super.toData();
        return new AgentAppUnitInfo(unit, agentId == null ? null : new AgentId(agentId),  projectName);
    }
}
