// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AGENT_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_DESCRIPTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_PROFILE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_SECRET_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AGENT_TENANT_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractAgentEntity<T extends Agent> extends BaseVersionedEntity<T> {

    @Column(name = AGENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = AGENT_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = AGENT_NAME_PROPERTY)
    private String name;

    @Column(name = AGENT_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = AGENT_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Column(name = AGENT_SECRET_PROPERTY)
    private String secret;

    @Column(name = AGENT_PROFILE_ID_PROPERTY)
    private UUID agentProfileId;

    @Convert(converter = JsonConverter.class)
    @Column(name = AGENT_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AbstractAgentEntity() {
        super();
    }

    public AbstractAgentEntity(T agent) {
        super(agent);
        if (agent.getTenantId() != null) {
            this.tenantId = agent.getTenantId().getId();
        }
        if (agent.getCustomerId() != null) {
            this.customerId = agent.getCustomerId().getId();
        }
        this.name = agent.getName();
        this.description = agent.getDescription();
        this.routingKey = agent.getRoutingKey();
        this.secret = agent.getSecret();
        if (agent.getAgentProfileId() != null) {
            this.agentProfileId = agent.getAgentProfileId().getId();
        }
        this.additionalInfo = agent.getAdditionalInfo();
    }

    public AbstractAgentEntity(AgentEntity agentEntity) {
        super(agentEntity);
        this.tenantId = agentEntity.getTenantId();
        this.customerId = agentEntity.getCustomerId();
        this.name = agentEntity.getName();
        this.description = agentEntity.getDescription();
        this.routingKey = agentEntity.getRoutingKey();
        this.secret = agentEntity.getSecret();
        this.agentProfileId = agentEntity.getAgentProfileId();
        this.additionalInfo = agentEntity.getAdditionalInfo();
    }

    protected Agent toAgent() {
        Agent agent = new Agent(new AgentId(id));
        agent.setCreatedTime(createdTime);
        agent.setVersion(version);
        if (tenantId != null) {
            agent.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            agent.setCustomerId(new CustomerId(customerId));
        }
        agent.setName(name);
        agent.setDescription(description);
        agent.setRoutingKey(routingKey);
        agent.setSecret(secret);
        if (agentProfileId != null) {
            agent.setAgentProfileId(new AgentProfileId(agentProfileId));
        }
        agent.setAdditionalInfo(additionalInfo);
        return agent;
    }

}
