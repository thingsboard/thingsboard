// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAgentAppEventStepsResolverTest {

    private static final String VERSION = "4.2.0EDGEPE";

    private AppTemplateRegistry registry;
    private AgentAppTemplate template;

    @BeforeEach
    void setUp() {
        registry = new AppTemplateRegistry();
        template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.EDGE);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setCurrentVersion(VERSION);
        template.setStartSteps(List.of(step("start", false)));
        template.setUpgradeSteps(List.of(step("upgrade", false)));
        template.setDeleteSteps(List.of(step("delete", false)));
        template.setRollbackSteps(List.of(step("rollback", false)));
        template.setRestartSteps(List.of(step("restart", false)));
        registry.replace(AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, Map.of(VERSION, template), template);
    }

    @Test
    void resolveStepsPicksTheListMatchingTheActionType() {
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry, List.of());
        AgentApplication app = app(new DockerComposeConfig());

        assertThat(titles(resolver.resolveSteps(app, AgentAppEventActionType.INSTALL))).containsExactly("start");
        assertThat(titles(resolver.resolveSteps(app, AgentAppEventActionType.UPDATE))).containsExactly("start");
        assertThat(titles(resolver.resolveSteps(app, AgentAppEventActionType.UPGRADE))).containsExactly("upgrade");
        assertThat(titles(resolver.resolveSteps(app, AgentAppEventActionType.DELETE))).containsExactly("delete");
        assertThat(titles(resolver.resolveSteps(app, AgentAppEventActionType.ROLLBACK))).containsExactly("rollback");
        assertThat(titles(resolver.resolveSteps(app, AgentAppEventActionType.RESTART))).containsExactly("restart");
    }

    @Test
    void resolveStepsFallsBackToDockerComposeWhenConfigIsNull() {
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry, List.of());

        assertThat(titles(resolver.resolveSteps(app(null), AgentAppEventActionType.INSTALL))).containsExactly("start");
    }

    @Test
    void resolveStepsDropsTemplateOnlySteps() {
        template.setStartSteps(List.of(step("template-only", true), step("start", false)));
        registry.replace(AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, Map.of(VERSION, template), template);
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry, List.of());

        assertThat(titles(resolver.resolveSteps(app(new DockerComposeConfig()), AgentAppEventActionType.INSTALL)))
                .containsExactly("start");
    }

    @Test
    void resolveStepsThrowsWhenTemplateIsNotRegistered() {
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry, List.of());
        AgentApplication app = app(new DockerComposeConfig());
        app.setTemplateVersion("9.9.9");

        assertThatThrownBy(() -> resolver.resolveSteps(app, AgentAppEventActionType.INSTALL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    void resolveStepsThrowsWhenTheActionListIsEmpty() {
        template.setDeleteSteps(List.of());
        registry.replace(AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, Map.of(VERSION, template), template);
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry, List.of());

        assertThatThrownBy(() -> resolver.resolveSteps(app(new DockerComposeConfig()), AgentAppEventActionType.DELETE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No steps resolved");
    }

    @Test
    void resolveStepsRejectsAgentScopedActionType() {
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry, List.of());

        assertThatThrownBy(() -> resolver.resolveSteps(app(new DockerComposeConfig()), AgentAppEventActionType.AGENT_UPGRADE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveAgentStepsUsesTheRegisteredProvider() {
        AgentAppStep agentStep = step("agent-upgrade", false);
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry,
                List.of(provider(AgentAppEventActionType.AGENT_UPGRADE, List.of(agentStep))));

        assertThat(titles(resolver.resolveAgentSteps(AgentAppEventActionType.AGENT_UPGRADE))).containsExactly("agent-upgrade");
    }

    @Test
    void resolveAgentStepsThrowsWhenNoProviderIsRegistered() {
        DefaultAgentAppEventStepsResolver resolver = new DefaultAgentAppEventStepsResolver(registry, List.of());

        assertThatThrownBy(() -> resolver.resolveAgentSteps(AgentAppEventActionType.AGENT_UPGRADE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No steps provider");
    }

    private static AgentScopedStepsProvider provider(AgentAppEventActionType actionType, List<AgentAppStep> steps) {
        return new AgentScopedStepsProvider() {
            @Override
            public AgentAppEventActionType actionType() {
                return actionType;
            }

            @Override
            public List<AgentAppStep> steps() {
                return steps;
            }
        };
    }

    private static AgentApplication app(DockerComposeConfig config) {
        AgentApplication app = new AgentApplication(new AgentApplicationId(UUID.randomUUID()));
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(VERSION);
        app.setConfig(config);
        return app;
    }

    private static AgentAppStep step(String title, boolean templateOnly) {
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle(title);
        step.setTemplateOnly(templateOnly);
        return step;
    }

    private static List<String> titles(List<AgentAppStep> steps) {
        return steps.stream().map(AgentAppStep::getTitle).toList();
    }
}
