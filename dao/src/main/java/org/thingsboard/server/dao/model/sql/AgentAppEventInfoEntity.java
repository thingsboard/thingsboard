// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAppEventInfoEntity extends AgentAppEventEntity {

    private String agentName;

    public AgentAppEventInfoEntity() {
        super();
    }

    public AgentAppEventInfoEntity(AgentAppEventEntity entity, String applicationName) {
        this(entity, applicationName, null);
    }

    public AgentAppEventInfoEntity(AgentAppEventEntity entity, String applicationName, String agentName) {
        super(entity);
        setApplicationName(applicationName);
        this.agentName = agentName;
    }

    @Override
    public AgentAppEventInfo toData() {
        return new AgentAppEventInfo(super.toData(), agentName);
    }
}
