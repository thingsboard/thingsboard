// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.agent.AgentAppArgumentReferenceValidator;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.EntitiesLimitExceededException;

import java.util.Objects;

@Component
@AllArgsConstructor
public class AgentApplicationDataValidator extends DataValidator<AgentApplication> {

    private final AgentService agentService;
    private final AgentApplicationDao agentApplicationDao;
    private final AppTemplateRegistry templateRegistry;
    private final AgentAppProfileService agentAppProfileService;
    private final AgentAppArgumentReferenceValidator argumentReferenceValidator;
    private final ApiLimitService apiLimitService;

    @Override
    protected void validateCreate(TenantId tenantId, AgentApplication application) {
        long limit = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxAgentApplications);
        if (limit > 0 && agentApplicationDao.countByTenantId(tenantId) >= limit) {
            throw new EntitiesLimitExceededException(tenantId, EntityType.AGENT_APPLICATION, limit);
        }
    }

    @Override
    protected AgentApplication validateUpdate(TenantId tenantId, AgentApplication application) {
        AgentApplicationInfo old = agentApplicationDao.findInfoById(tenantId, application.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent application!");
        }
        if (old.isPendingDeletion() && application.isPendingDeletion()) {
            throw new DataValidationException("Application is already pending for removal");
        }
        if (!Objects.equals(old.getOrigin(), application.getOrigin())) {
            throw new DataValidationException("Application origin does not match");
        }
        if (old.getAppType() != application.getAppType()) {
            throw new DataValidationException("Application type does not match");
        }
        if (!Objects.equals(old.getProjectName(), application.getProjectName())) {
            throw new DataValidationException("Application project name does not match");
        }
        AgentAppProfileId newProfileId = application.getApplicationProfileId();
        boolean profileChanged = newProfileId != null && !newProfileId.equals(old.getApplicationProfileId());
        if (application.getDesiredTemplateVersion() == null && !profileChanged
                && !Objects.equals(old.getTemplateVersion(), application.getTemplateVersion())) {
            throw new DataValidationException("Cannot change template version without specifying desired template version");
        }

        boolean isUpgrade = application.getDesiredTemplateVersion() != null;

        if (isUpgrade) {
            validateUpgradeChain(old, application);
            if (newProfileId != null) {
                AgentAppProfile profile = loadProfile(tenantId, newProfileId);
                requireDesiredTemplateMatchesProfile(application, profile);
                requireConfigMatchesProfile(application, profile);
            }
            return old;
        }

        if (newProfileId == null) {
            return old;
        }
        if (!profileChanged && AgentAppConfig.equalsIgnoringCreds(application.getAppType(), application.getConfig(), old.getConfig())) {
            return old;
        }
        AgentAppProfile profile = loadProfile(tenantId, newProfileId);
        requireTemplateMatchesProfile(application, profile);
        requireConfigMatchesProfile(application, profile);
        return old;
    }

    private AgentAppProfile loadProfile(TenantId tenantId, AgentAppProfileId profileId) {
        AgentAppProfile profile = agentAppProfileService.findProfileById(tenantId, profileId);
        if (profile == null) {
            throw new DataValidationException("Application profile not found");
        }
        return profile;
    }

    private void requireConfigMatchesProfile(AgentApplication application, AgentAppProfile profile) {
        if (!AgentAppConfig.equalsIgnoringCreds(application.getAppType(), application.getConfig(), profile.getConfig())) {
            throw new DataValidationException(
                    "Application config does not match the profile's config (only credential fields can differ)");
        }
    }

    private void requireDesiredTemplateMatchesProfile(AgentApplication application, AgentAppProfile profile) {
        if (!Objects.equals(application.getDesiredTemplateVersion(), profile.getTemplateVersion())) {
            throw new DataValidationException("Desired template does not match the profile's template");
        }
    }

    private void requireTemplateMatchesProfile(AgentApplication application, AgentAppProfile profile) {
        if (!Objects.equals(application.getTemplateVersion(), profile.getTemplateVersion())) {
            throw new DataValidationException("Application's template version must match the assigned profile's template version");
        }
    }

    private void validateUpgradeChain(AgentApplication old, AgentApplication application) {
        AgentAppConfigType configType = configTypeOf(application);
        String nextVersion = templateRegistry.next(old.getAppType(), configType, old.getTemplateVersion());
        if (nextVersion == null) {
            throw new DataValidationException("No next version available for upgrade!");
        }
        if (templateRegistry.get(application.getAppType(), configType, application.getDesiredTemplateVersion()) == null) {
            throw new DataValidationException("Desired template not found");
        }
        if (!nextVersion.equals(application.getDesiredTemplateVersion())) {
            throw new DataValidationException("Desired template version does not match the next available version");
        }
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentApplication agentApplication) {
        if (agentApplication.getAgentId() == null) {
            throw new DataValidationException("Agent application should be assigned to agent!");
        }
        if (agentApplication.getAppType() == null) {
            throw new DataValidationException("Agent application type must not be null!");
        }
        if (agentApplication.getOrigin() == null) {
            throw new DataValidationException("Agent application origin must not be null!");
        }
        if (agentApplication.getProjectName() == null) {
            throw new DataValidationException("Agent application project name must not be null!");
        }
        validateTemplate(agentApplication);
        Agent agent = agentService.findAgentById(tenantId, agentApplication.getAgentId());
        if (agent == null) {
            throw new DataValidationException("Agent application is referencing non-existent agent!");
        }
        if (!agent.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Agent application cannot be assigned to agent from different tenant!");
        }
        String name = agentApplication.getName();
        if (name != null) {
            if (StringUtils.contains0x00(name)) {
                throw new DataValidationException("Agent application name should not contain 0x00 symbol!");
            }
            if (name.length() > 255) {
                throw new DataValidationException("Agent application name length must be equal or shorter than 255!");
            }
        }
        if (agentApplication.getApplicationProfileId() == null && agentApplication.getConfig() == null) {
            throw new DataValidationException("Agent application config must not be null!");
        }
        if (agentApplication.getConfig() != null) {
            agentApplication.getConfig().validate();
            if (agentApplication.getApplicationProfileId() != null) {
                agentApplication.getConfig().validateForProfile(agentApplication.getAppType());
            }
            argumentReferenceValidator.validate(tenantId, agentApplication.getConfig(), agentApplication);
        }
    }

    private static AgentAppConfigType configTypeOf(AgentApplication application) {
        return application.getConfig() != null ? application.getConfig().getType() : AgentAppConfigType.DOCKER_COMPOSE;
    }

    private void validateTemplate(AgentApplication application) {
        if (application.getTemplateVersion() == null) {
            throw new DataValidationException("Agent application should be assigned to a template version!");
        }
        if (templateRegistry.get(application.getAppType(), configTypeOf(application), application.getTemplateVersion()) == null) {
            throw new DataValidationException("Agent application is referencing non-existent template!");
        }
        String appTypeDefaultVersion = application.getAppType().getDefaultVersion();
        if (appTypeDefaultVersion != null && !appTypeDefaultVersion.equals(application.getTemplateVersion())) {
            throw new DataValidationException("Template version does not match the application's current version");
        }
    }
}
