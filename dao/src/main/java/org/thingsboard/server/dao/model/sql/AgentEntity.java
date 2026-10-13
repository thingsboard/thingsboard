// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.Agent;

import static org.thingsboard.server.dao.model.ModelConstants.AGENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = AGENT_TABLE_NAME)
public final class AgentEntity extends AbstractAgentEntity<Agent> {

    public AgentEntity() {
        super();
    }

    public AgentEntity(Agent agent) {
        super(agent);
    }

    @Override
    public Agent toData() {
        return super.toAgent();
    }
}
