// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class DefaultAgentAppEventStepsResolver implements AgentAppEventStepsResolver {

    private final AppTemplateRegistry templateRegistry;
    private final Map<AgentAppEventActionType, AgentScopedStepsProvider> agentStepsProviders;

    public DefaultAgentAppEventStepsResolver(AppTemplateRegistry templateRegistry,
                                             List<AgentScopedStepsProvider> agentStepsProviders) {
        this.templateRegistry = templateRegistry;
        this.agentStepsProviders = agentStepsProviders.stream()
                .collect(Collectors.toMap(AgentScopedStepsProvider::actionType, Function.identity()));
    }

    @Override
    public List<AgentAppStep> resolveAgentSteps(AgentAppEventActionType actionType) {
        AgentScopedStepsProvider provider = agentStepsProviders.get(actionType);
        if (provider == null) {
            throw new IllegalArgumentException("No steps provider for agent-scoped action type: " + actionType);
        }
        return provider.steps();
    }

    @Override
    public List<AgentAppStep> resolveSteps(AgentApplication app, AgentAppEventActionType actionType) {
        String version = app.getTemplateVersion();
        AgentAppConfigType configType = app.getConfig() != null ? app.getConfig().getType() : AgentAppConfigType.DOCKER_COMPOSE;
        AgentAppTemplate template = templateRegistry.get(app.getAppType(), configType, version);
        if (template == null) {
            throw new IllegalStateException("Template not found for application " + app.getId());
        }
        List<AgentAppStep> steps = getAgentAppSteps(actionType, template);
        Predicate<AgentAppStep> nonTemplateOnly = s -> !s.isTemplateOnly();
        return StepLinkedListUtils.filter(steps, nonTemplateOnly);
    }

    private List<AgentAppStep> getAgentAppSteps(AgentAppEventActionType actionType, AgentAppTemplate template) {
        List<AgentAppStep> steps = switch (actionType) {
            case INSTALL, UPDATE -> template.getStartSteps();
            case UPGRADE -> template.getUpgradeSteps();
            case DELETE -> template.getDeleteSteps();
            case ROLLBACK -> template.getRollbackSteps();
            case RESTART -> template.getRestartSteps();
            case AGENT_UPGRADE ->
                    throw new IllegalArgumentException("Agent-scoped events do not resolve steps from an application template");
        };
        if (steps == null || steps.isEmpty()) {
            throw new IllegalStateException("No steps resolved for template " + template.getAppType()
                    + "-" + template.getCurrentVersion() + " and action " + actionType);
        }
        return steps;
    }
}
