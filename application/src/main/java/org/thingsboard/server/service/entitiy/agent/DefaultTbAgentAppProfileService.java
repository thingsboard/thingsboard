// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentAppProfileService extends AbstractTbEntityService implements TbAgentAppProfileService {

    private final AgentAppProfileService profileService;
    private final AgentAppConfigMergeOrchestrator configMergeOrchestrator;

    @Override
    public AgentAppProfile save(AgentAppProfile profile, User user) throws Exception {
        ActionType actionType = profile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = profile.getTenantId();
        try {
            AgentAppProfile saved = checkNotNull(profileService.saveProfile(profile));
            logEntityActionService.logEntityAction(tenantId, saved.getId(), saved, actionType, user);
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APP_PROFILE), profile, actionType, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(AgentAppProfile profile, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = profile.getTenantId();
        try {
            profileService.deleteProfile(tenantId, profile.getId());
            logEntityActionService.logEntityAction(tenantId, profile.getId(), profile, actionType, user, profile.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APP_PROFILE), profile, actionType, user, e, profile.getId().toString());
            throw e;
        }
    }

    @Override
    public AgentAppProfile mergeForPreview(TenantId tenantId, AgentAppProfile appProfile, AppConfigMergeCtx ctx) {
        log.trace("Executing mergeForPreview, tenantId [{}], appProfileId [{}], templateVersion [{}]",
                tenantId, appProfile.getId(), ctx.getTemplate() != null ? ctx.getTemplate().getCurrentVersion() : null);
        configMergeOrchestrator.merge(appProfile, ctx);
        return appProfile;
    }

    @Override
    public AgentAppProfile createFromTemplate(TenantId tenantId, AgentAppTemplate template, String composeType, String baseUrl, User user) throws Exception {
        List<AgentAppProfile> existing = profileService.findProfilesByTenantIdAndAppTypeAndTemplateVersion(
                tenantId, template.getAppType(), template.getCurrentVersion());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setAppType(template.getAppType());
        profile.setTemplateVersion(template.getCurrentVersion());
        profile.setName(defaultProfileName(template));
        profile.setConfig(AgentAppConfig.forType(template.getConfigType()));

        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder()
                .template(template)
                .selectedComposeType(resolveComposeType(template, composeType))
                .setHostValues(true)
                .baseUrl(baseUrl)
                .build();
        configMergeOrchestrator.merge(profile, ctx);
        return save(profile, user);
    }

    private static String resolveComposeType(AgentAppTemplate template, String composeType) {
        if (StringUtils.isNotBlank(composeType)) {
            return composeType;
        }
        return StepLinkedListUtils.getByType(AgentAppStepType.COMPOSE_TEMPLATE, ComposeTypeChoiceStep.class, template.getStartSteps())
                .flatMap(step -> step.getComposeTypes().stream().findFirst())
                .orElse(null);
    }

    private static String defaultProfileName(AgentAppTemplate template) {
        return StringUtils.capitalize(template.getAppType().name().toLowerCase()) + " " + template.getCurrentVersion();
    }
}
