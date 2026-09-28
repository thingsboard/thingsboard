// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentAppProfile extends BaseData<AgentAppProfileId> implements HasId<AgentAppProfileId>, HasTenantId, HasVersion, HasName, HasAgentAppConfig, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_PROFILE;
    }

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "description")
    private String description;
    private AgentApplicationType appType;
    private String templateVersion;
    @Valid
    private AgentAppConfig config;
    @Getter
    private Long version;

    public AgentAppProfile() {
        super();
    }

    public AgentAppProfile(AgentAppProfileId id) {
        super(id);
    }

    public AgentAppProfile(AgentAppProfile profile) {
        super(profile);
        this.tenantId = profile.getTenantId();
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.appType = profile.getAppType();
        this.templateVersion = profile.getTemplateVersion();
        this.config = profile.getConfig() == null ? null : profile.getConfig().copy();
        this.version = profile.getVersion();
    }

    @Schema(description = "JSON object with the Agent Application Profile Id.")
    @Override
    public AgentAppProfileId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the profile creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique profile name within tenant")
    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "Profile description")
    public String getDescription() {
        return description;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Application type: EDGE, GATEWAY, or GENERIC")
    public AgentApplicationType getAppType() {
        return appType;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Template version this profile is based on")
    public String getTemplateVersion() {
        return templateVersion;
    }

    @Schema(description = "Shared config (compose definition)")
    public AgentAppConfig getConfig() {
        return config;
    }
}
