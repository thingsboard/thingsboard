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
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.ThreadLocalRandom;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentApplication extends BaseData<AgentApplicationId> implements HasId<AgentApplicationId>, HasTenantId, HasVersion, HasName, HasAgentAppConfig, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APPLICATION;
    }

    private TenantId tenantId;
    private AgentId agentId;
    private String name;
    private String templateVersion;
    @Getter
    private String desiredTemplateVersion;
    private AgentApplicationType appType;
    @Valid
    private AgentAppConfig config;
    @Getter
    private Long version;
    @Getter
    private String projectName;
    @Getter
    private boolean pendingDeletion;
    private AgentApplicationOrigin origin;
    private AgentAppProfileId applicationProfileId;
    @Getter
    private Long profileConfigVersion;

    public AgentApplication() {
        super();
    }

    public AgentApplication(AgentApplicationId id) {
        super(id);
    }

    public AgentApplication(AgentApplication application) {
        super(application);
        this.tenantId = application.getTenantId();
        this.agentId = application.getAgentId();
        this.appType = application.getAppType();
        this.name = application.getName();
        this.templateVersion = application.getTemplateVersion();
        this.desiredTemplateVersion = application.getDesiredTemplateVersion();
        this.config = application.getConfig() == null ? null : application.getConfig().copy();
        this.projectName = application.getProjectName();
        this.version = application.getVersion();
        this.pendingDeletion = application.isPendingDeletion();
        this.origin = application.getOrigin();
        this.applicationProfileId = application.getApplicationProfileId();
        this.profileConfigVersion = application.getProfileConfigVersion();
    }

    public static AgentApplication fromTemplate(AgentAppTemplate template) {
        AgentApplication app = new AgentApplication();
        app.setTemplateVersion(template.getCurrentVersion());
        app.setAppType(template.getAppType());
        app.setConfig(AgentAppConfig.forType(template.getConfigType()));
        return app;
    }

    @Schema(description = "JSON object with the Agent Application Id.")
    @Override
    public AgentApplicationId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the agent application creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(description = "Agent this application belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentId getAgentId() {
        return agentId;
    }

    @Schema(description = "Application type", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentApplicationType getAppType() {
        return appType;
    }

    @Schema(description = "Application name (not unique across tenant)")
    public String getName() {
        return name;
    }

    @Schema(description = "Config (with compose field and type = 'DOCKER_COMPOSE' for EDGE/GATEWAY)")
    public AgentAppConfig getConfig() {
        return config;
    }

    @Schema(description = "Template version this application is based on", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getTemplateVersion() {
        return templateVersion;
    }

    @Schema(description = "Origin of the application (INSTALLED or DISCOVERED)")
    public AgentApplicationOrigin getOrigin() {
        return origin;
    }

    @Schema(description = "Application Profile Id. When set, config is read-only and inherited from the profile.")
    public AgentAppProfileId getApplicationProfileId() {
        return applicationProfileId;
    }

    public static String generateProjectName() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong(0x1000000000000L, 0xFFFFFFFFFFFFFL));
    }

}
