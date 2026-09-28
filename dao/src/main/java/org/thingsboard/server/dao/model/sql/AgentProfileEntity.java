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
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_PROFILE_TABLE_NAME)
public class AgentProfileEntity extends BaseVersionedEntity<AgentProfile> {

    @Column(name = ModelConstants.AGENT_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.AGENT_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.AGENT_PROFILE_PROVISION_KEY_PROPERTY)
    private String provisionKey;

    @Column(name = ModelConstants.AGENT_PROFILE_PROVISION_SECRET_PROPERTY)
    private String provisionSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_PROFILE_PROVISION_TYPE_PROPERTY)
    private AgentProvisionType provisionType;

    @Column(name = ModelConstants.AGENT_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    public AgentProfileEntity() {
        super();
    }

    public AgentProfileEntity(AgentProfileEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.name = entity.name;
        this.description = entity.description;
        this.provisionKey = entity.provisionKey;
        this.provisionSecret = entity.provisionSecret;
        this.provisionType = entity.provisionType;
        this.isDefault = entity.isDefault;
    }

    public AgentProfileEntity(AgentProfile agentProfile) {
        super(agentProfile);
        if (agentProfile.getTenantId() != null) {
            this.tenantId = agentProfile.getTenantId().getId();
        }
        this.name = agentProfile.getName();
        this.description = agentProfile.getDescription();
        this.provisionKey = agentProfile.getProvisionKey();
        this.provisionSecret = agentProfile.getProvisionSecret();
        this.provisionType = agentProfile.getProvisionType();
        this.isDefault = agentProfile.isDefault();
    }

    @Override
    public AgentProfile toData() {
        AgentProfile agentProfile = new AgentProfile(new AgentProfileId(id));
        agentProfile.setCreatedTime(createdTime);
        agentProfile.setVersion(version);
        if (tenantId != null) {
            agentProfile.setTenantId(TenantId.fromUUID(tenantId));
        }
        agentProfile.setName(name);
        agentProfile.setDescription(description);
        agentProfile.setProvisionKey(provisionKey);
        agentProfile.setProvisionSecret(provisionSecret);
        agentProfile.setProvisionType(provisionType);
        agentProfile.setDefault(isDefault);
        return agentProfile;
    }
}
