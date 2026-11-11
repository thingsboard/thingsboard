// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MergeUpgradeImageRule implements AppConfigMergeRule {

    @Override
    public boolean supports(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        return ctx != null
                && ctx.getActionType() == AgentAppEventActionType.UPGRADE
                && ctx.getTemplate() != null
                && (ctx.getTemplate().getAppType() == AgentApplicationType.EDGE ||
                    ctx.getTemplate().getAppType() == AgentApplicationType.GATEWAY);
    }

    @Override
    public void apply(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        AgentAppTemplate template = ctx.getTemplate();
        AgentApplicationType appType = template.getAppType();
        if (appType == null || appType.getMainImagePattern() == null) {
            log.trace("Skipping upgrade image merge: appType [{}] has no main image pattern", appType);
            return;
        }
        Pattern mainImagePattern = appType.getMainImagePattern();

        JsonNode templateCompose = resolveTemplateCompose(template, ctx);
        if (templateCompose == null) {
            log.trace("Skipping upgrade image merge: no compose template found");
            return;
        }
        String newImage = DockerComposeUtils.getMainImage(templateCompose, mainImagePattern);
        if (StringUtils.isBlank(newImage)) {
            log.trace("Skipping upgrade image merge: no main image found in template compose");
            return;
        }

        JsonNode targetCompose = getCompose(hasAgentAppConfig.getConfig());
        if (targetCompose == null) {
            log.trace("Skipping upgrade image merge: target compose is null");
            return;
        }
        DockerComposeUtils.setMainImage(targetCompose, mainImagePattern, newImage);
    }

    // The main image is identical across compose-type variants, so any variant works;
    // honor the selected type when present, else take the first variant.
    private static JsonNode resolveTemplateCompose(AgentAppTemplate template, AppConfigMergeCtx ctx) {
        Optional<ComposeTypeChoiceStep> choiceStep = StepLinkedListUtils.getByType(
                AgentAppStepType.COMPOSE_TEMPLATE, ComposeTypeChoiceStep.class, template.getStartSteps());
        if (choiceStep.isPresent()) {
            Map<String, JsonNode> variants = choiceStep.get().getComposeTemplates();
            if (variants != null && !variants.isEmpty()) {
                String selected = ctx.getSelectedComposeType();
                if (StringUtils.isNotBlank(selected) && variants.containsKey(selected)) {
                    return variants.get(selected);
                }
                return variants.values().iterator().next();
            }
        }
        return null;
    }

    private static JsonNode getCompose(AgentAppConfig config) {
        return config instanceof DockerComposeConfig composeConfig ? composeConfig.getCompose() : null;
    }
}
