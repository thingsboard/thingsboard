// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_UNIT_TABLE_NAME)
public class AgentAppUnitEntity extends BaseSqlEntity<AgentAppUnit> {

    @Column(name = ModelConstants.AGENT_APP_UNIT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_UNIT_AGENT_APPLICATION_ID_PROPERTY)
    private UUID agentApplicationId;

    @Column(name = ModelConstants.AGENT_APP_UNIT_IDENTIFIER_PROPERTY)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_UNIT_TYPE_PROPERTY)
    private AgentAppUnitType type;

    public AgentAppUnitEntity() {
        super();
    }

    public AgentAppUnitEntity(AgentAppUnit unit) {
        super(unit);
        if (unit.getTenantId() != null) {
            this.tenantId = unit.getTenantId().getId();
        }
        if (unit.getAgentApplicationId() != null) {
            this.agentApplicationId = unit.getAgentApplicationId().getId();
        }
        this.identifier = unit.getIdentifier();
        this.type = unit.getType();
    }

    public AgentAppUnitEntity(AgentAppUnitEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.agentApplicationId = entity.agentApplicationId;
        this.identifier = entity.identifier;
        this.type = entity.type;
    }

    @Override
    public AgentAppUnit toData() {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(id));
        unit.setCreatedTime(createdTime);
        if (tenantId != null) {
            unit.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (agentApplicationId != null) {
            unit.setAgentApplicationId(new AgentApplicationId(agentApplicationId));
        }
        unit.setIdentifier(identifier);
        unit.setType(type);
        return unit;
    }
}
