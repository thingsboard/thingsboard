// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

/**
 * Read-only agent app configuration materialized from the public template repository (keyed by app type + version in
 * {@code AppTemplateRegistry}). Not a persistent entity: it has no id/EntityType and is never stored in the DB, so it
 * intentionally does not extend {@code BaseData} / implement {@code HasId}.
 */
@Schema
@EqualsAndHashCode
@ToString
@Setter
public class AgentAppTemplate implements HasTenantId {

    private TenantId tenantId;
    private AgentApplicationType appType;
    private AgentAppConfigType configType;
    private String currentVersion;
    private String nextVersion;
    private List<AgentAppStep> startSteps;
    private List<AgentAppStep> upgradeSteps;
    private List<AgentAppStep> deleteSteps;
    private List<AgentAppStep> rollbackSteps;
    private List<AgentAppStep> restartSteps;

    public AgentAppTemplate() {
    }

    public AgentAppTemplate(AgentAppTemplate template) {
        this.tenantId = template.getTenantId();
        this.appType = template.getAppType();
        this.configType = template.getConfigType();
        this.currentVersion = template.getCurrentVersion();
        this.nextVersion = template.getNextVersion();
        this.startSteps = template.getStartSteps();
        this.upgradeSteps = template.getUpgradeSteps();
        this.deleteSteps = template.getDeleteSteps();
        this.rollbackSteps = template.getRollbackSteps();
        this.restartSteps = template.getRestartSteps();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(description = "Application type", requiredMode = Schema.RequiredMode.REQUIRED)
    public AgentApplicationType getAppType() {
        return appType;
    }

    @Schema(description = "Config type (e.g. 'DOCKER_COMPOSE'); the compose body lives in the template's compose-template step, not here")
    public AgentAppConfigType getConfigType() {
        return configType;
    }

    @Schema(description = "Current template version", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getCurrentVersion() {
        return currentVersion;
    }

    @Schema(description = "Next template version")
    public String getNextVersion() {
        return nextVersion;
    }

    @Schema(description = "Start steps")
    public List<AgentAppStep> getStartSteps() {
        return startSteps;
    }

    @Schema(description = "Upgrade steps")
    public List<AgentAppStep> getUpgradeSteps() {
        return upgradeSteps;
    }

    @Schema(description = "Delete steps")
    public List<AgentAppStep> getDeleteSteps() {
        return deleteSteps;
    }

    @Schema(description = "Rollback steps")
    public List<AgentAppStep> getRollbackSteps() {
        return rollbackSteps;
    }

    @Schema(description = "Restart steps")
    public List<AgentAppStep> getRestartSteps() {
        return restartSteps;
    }
}
