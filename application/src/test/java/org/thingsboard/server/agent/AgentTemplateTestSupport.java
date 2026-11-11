// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Seeds the in-memory template registry for tests, under the template's own app type only, keyed by its
// current version. The registry is a singleton shared by the cached Spring context, so registering under
// other app types would leave a version behind that relaxes the app-save validator
// (registry.get(app.getAppType(), configType, templateVersion) != null) for every later test in that context.
// Existing versions of the same app type are preserved so multiple templates can coexist.
public final class AgentTemplateTestSupport {

    public static final String GENERIC_TEMPLATE_VERSION = "1.0.0";

    private AgentTemplateTestSupport() {
    }

    public static AgentAppTemplate register(AppTemplateRegistry registry, AgentAppTemplate template) {
        AgentAppConfigType configType = template.getConfigType() != null
                ? template.getConfigType() : AgentAppConfigType.DOCKER_COMPOSE;
        AgentApplicationType appType = template.getAppType() != null
                ? template.getAppType() : AgentApplicationType.GENERIC;
        Map<String, AgentAppTemplate> byVersion = new HashMap<>();
        registry.list(appType, configType).forEach(t -> byVersion.put(t.getCurrentVersion(), t));
        byVersion.put(template.getCurrentVersion(), template);
        registry.replace(appType, configType, byVersion, template);
        return template;
    }

    /**
     * Registers a minimal GENERIC docker-compose template and returns it. The shared home for this fixture:
     * the app-save validator matches on (appType, configType, templateVersion), so the template and the
     * profile built from it by {@link #appProfileFor} must stay in sync.
     */
    public static AgentAppTemplate registerGenericTemplate(AppTemplateRegistry registry) {
        return registerGenericTemplate(registry, GENERIC_TEMPLATE_VERSION);
    }

    public static AgentAppTemplate registerGenericTemplate(AppTemplateRegistry registry, String version) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion(version);
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        return register(registry, template);
    }

    /** An app profile that validates against {@code template}: same app type, same template version. */
    public static AgentAppProfile appProfileFor(AgentAppTemplate template, String name) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setName(name);
        profile.setAppType(template.getAppType());
        profile.setTemplateVersion(template.getCurrentVersion());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);
        return profile;
    }

    /**
     * Carries the compose body in a template-only {@link ComposeTypeChoiceStep} (as production templates do),
     * linked as the chain head so the step list stays a valid linked list; it is filtered out before the agent
     * executes steps, so it does not affect step-completion assertions.
     */
    public static List<AgentAppStep> withComposeTemplate(List<AgentAppStep> startSteps, JsonNode compose) {
        List<AgentAppStep> steps = new ArrayList<>(startSteps != null ? startSteps : List.of());
        ComposeTypeChoiceStep choice = new ComposeTypeChoiceStep();
        choice.setId(UUID.randomUUID());
        choice.setTitle("Choose compose");
        choice.setTemplateOnly(true);
        choice.setComposeTemplates(Map.of("default", compose));
        AgentAppStep head = steps.isEmpty() ? null : StepLinkedListUtils.findFirstStep(steps);
        choice.setNextId(head != null ? head.getId() : null);
        steps.add(choice);
        return steps;
    }

    public static JsonNode resolveTemplateCompose(AgentAppTemplate template) {
        if (template.getStartSteps() == null) {
            return null;
        }
        return template.getStartSteps().stream()
                .filter(ComposeTypeChoiceStep.class::isInstance)
                .map(step -> ((ComposeTypeChoiceStep) step).getComposeTemplates().values().iterator().next())
                .findFirst()
                .orElse(null);
    }

    public static DockerComposeConfig resolveAppConfig(AgentAppTemplate template) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(resolveTemplateCompose(template));
        return config;
    }
}
