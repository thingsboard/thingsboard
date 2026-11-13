// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_PROFILE_TABLE_NAME)
public class AgentAppProfileEntity extends BaseVersionedEntity<AgentAppProfile> {

    @Column(name = ModelConstants.AGENT_APP_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_PROFILE_APP_TYPE_PROPERTY)
    private AgentApplicationType appType;

    @Column(name = ModelConstants.AGENT_APP_PROFILE_TEMPLATE_VERSION_PROPERTY)
    private String templateVersion;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.AGENT_APP_PROFILE_CONFIG_PROPERTY)
    private JsonNode config;

    public AgentAppProfileEntity() {
        super();
    }

    public AgentAppProfileEntity(AgentAppProfile profile) {
        super(profile);
        if (profile.getTenantId() != null) {
            this.tenantId = profile.getTenantId().getId();
        }
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.appType = profile.getAppType();
        this.templateVersion = profile.getTemplateVersion();
        this.config = profile.getConfig() != null ? JacksonUtil.valueToTree(profile.getConfig()) : null;
    }

    public AgentAppProfileEntity(AgentAppProfileEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.name = entity.name;
        this.description = entity.description;
        this.appType = entity.appType;
        this.templateVersion = entity.templateVersion;
        this.config = entity.config;
    }

    @Override
    public AgentAppProfile toData() {
        AgentAppProfile profile = new AgentAppProfile(new AgentAppProfileId(id));
        profile.setCreatedTime(createdTime);
        profile.setVersion(version);
        if (tenantId != null) {
            profile.setTenantId(TenantId.fromUUID(tenantId));
        }
        profile.setName(name);
        profile.setDescription(description);
        profile.setAppType(appType);
        profile.setTemplateVersion(templateVersion);
        profile.setConfig(config != null ? JacksonUtil.treeToValue(config, AgentAppConfig.class) : null);
        return profile;
    }
}
