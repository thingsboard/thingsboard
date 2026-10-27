// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeRestartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The executing half of a bulk action - the queue hop into the bulk-ops consumer, the eligibility filter, the
 * per-app event creation and the action row's own state machine - is otherwise only ever seen against mocks.
 */
@DaoSqlTest
public class AgentBulkActionAgentTest extends AbstractAgentTest {

    @Autowired
    AgentBulkActionService agentBulkActionService;

    private final List<AgentImitator> imitators = new ArrayList<>();

    private AgentProfile agentProfile;
    private AgentAppProfile appProfile;

    @After
    public void disconnectImitators() {
        imitators.forEach(imitator -> {
            try {
                imitator.disconnect();
            } catch (Exception ignored) {
            }
        });
        imitators.clear();
    }

    @Test
    public void testBulkRestartProducesOneCommandPerEligibleApp() throws Exception {
        prepareProfiles();
        AgentUnderTest first = installedAgent("bulk-agent-1");
        AgentUnderTest second = installedAgent("bulk-agent-2");

        AgentBulkAction action = submitBulk(AgentAppEventActionType.RESTART);

        awaitCommand(first.imitator(), AppCommandAction.APP_RESTART);
        awaitCommand(second.imitator(), AppCommandAction.APP_RESTART);

        AgentBulkAction started = awaitBulkStatus(action.getId(), AgentBulkActionStatus.STARTED);
        Assert.assertEquals(2, started.getTotal());
        Assert.assertEquals(2, started.getSubmitted());
        Assert.assertTrue("no application should have been skipped: " + started.getSkipCounts(),
                started.getSkipCounts() == null || started.getSkipCounts().isEmpty());
    }

    @Test
    public void testBulkRestartSkipsApplicationsWithAnActiveEvent() throws Exception {
        prepareProfiles();
        AgentUnderTest eligible = installedAgent("bulk-agent-eligible");
        // the install of the second agent is never acknowledged, so its app still has an active event
        AgentUnderTest busy = connectedAgent("bulk-agent-busy");
        installApp(busy.agent());

        AgentBulkAction action = submitBulk(AgentAppEventActionType.RESTART);

        awaitCommand(eligible.imitator(), AppCommandAction.APP_RESTART);

        AgentBulkAction started = awaitBulkStatus(action.getId(), AgentBulkActionStatus.STARTED);
        Assert.assertEquals(2, started.getTotal());
        Assert.assertEquals(1, started.getSubmitted());
        Assert.assertEquals(Integer.valueOf(1), started.getSkipCounts().get(SkipReason.ACTIVE_EVENT));
        Assert.assertFalse("the busy agent must not be handed a restart",
                busy.imitator().hasUnconsumedCommand(c -> c.getAction() == AppCommandAction.APP_RESTART));
    }

    private void prepareProfiles() throws Exception {
        AgentAppTemplate template = registerGenericTemplate();

        AgentProfile profile = new AgentProfile();
        profile.setName("bulk-agent-profile-" + System.nanoTime());
        profile.setProvisionType(AgentProvisionType.DISABLED);
        agentProfile = doPost("/api/agent/profile", profile, AgentProfile.class);

        AgentAppProfile appProfileToSave = new AgentAppProfile();
        appProfileToSave.setName("bulk-app-profile-" + System.nanoTime());
        appProfileToSave.setAppType(AgentApplicationType.GENERIC);
        appProfileToSave.setTemplateVersion(template.getCurrentVersion());
        appProfileToSave.setConfig(resolveAppConfig(template));
        appProfile = doPost("/api/agent/app/profile", appProfileToSave, AgentAppProfile.class);

        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());
    }

    private AgentAppTemplate registerGenericTemplate() {
        ComposeStep startStep = new ComposeStep();
        startStep.setId(UUID.randomUUID());
        startStep.setTitle("Deploy compose");
        ComposeRestartStep restartStep = new ComposeRestartStep();
        restartStep.setId(UUID.randomUUID());
        restartStep.setTitle("Restart compose");

        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion(AgentApplicationType.GENERIC.getDefaultVersion());
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setStartSteps(AgentTemplateTestSupport.withComposeTemplate(
                List.<AgentAppStep>of(startStep),
                JacksonUtil.toJsonNode(constructComposeJson(Map.of("my-app", "my-app:1.0")))));
        template.setRestartSteps(List.<AgentAppStep>of(restartStep));
        return AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }

    /** A connected agent of the bulk agent profile whose app install has been driven to completion. */
    private AgentUnderTest installedAgent(String name) throws Exception {
        AgentUnderTest underTest = connectedAgent(name);
        AgentApplication app = installApp(underTest.agent());

        AppCommand install = awaitCommand(underTest.imitator(), AppCommandAction.APP_INSTALL);
        completeAllSteps(underTest.imitator(), install);
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !agentAppEventService.hasActiveOrPendingEventForApplication(app.getId()));
        return underTest;
    }

    private AgentUnderTest connectedAgent(String name) throws Exception {
        Agent newAgent = new Agent();
        newAgent.setName(name + "-" + System.nanoTime());
        newAgent.setAgentProfileId(agentProfile.getId());
        newAgent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        newAgent.setSecret(StringUtils.randomAlphanumeric(20));
        Agent saved = doPost("/api/agent", newAgent, Agent.class);

        AgentImitator imitator = new AgentImitator(AGENT_HOST, AGENT_PORT, saved.getRoutingKey(), saved.getSecret());
        imitator.connect();
        imitators.add(imitator);
        return new AgentUnderTest(saved, imitator);
    }

    private AgentApplication installApp(Agent owner) {
        AgentApplication app = new AgentApplication();
        app.setName("bulk-app-" + System.nanoTime());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion(appProfile.getTemplateVersion());
        app.setApplicationProfileId(appProfile.getId());
        app.setAgentId(owner.getId());

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);
        return doPost("/api/agent/app/event", request, AgentAppInstallResponse.class).getApplication();
    }

    private AgentBulkAction submitBulk(AgentAppEventActionType actionType) throws Exception {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);
        return doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId() + "/bulk", request, AgentBulkAction.class);
    }

    private AppCommand awaitCommand(AgentImitator imitator, AppCommandAction action) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> imitator.hasUnconsumedCommand(c -> c.getAction() == action));
        return imitator.consumeNextCommand(c -> c.getAction() == action);
    }

    private void completeAllSteps(AgentImitator imitator, AppCommand cmd) {
        imitator.sendCommandAck(cmd.getCommandId(), AckStatus.ACCEPTED);
        imitator.sendCommandResult(cmd.getCommandId(), cmd.getStepId(), true);
        for (int i = 1; i < cmd.getTotalSteps(); i++) {
            AppCommand next = awaitCommand(imitator, cmd.getAction());
            imitator.sendCommandAck(next.getCommandId(), AckStatus.ACCEPTED);
            imitator.sendCommandResult(next.getCommandId(), next.getStepId(), true);
        }
    }

    private AgentBulkAction awaitBulkStatus(AgentBulkActionId actionId, AgentBulkActionStatus expected) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentBulkAction action = agentBulkActionService.findById(tenantId, actionId);
                    return action != null && action.getStatus() == expected;
                });
        return agentBulkActionService.findById(tenantId, actionId);
    }

    private record AgentUnderTest(Agent agent, AgentImitator imitator) {
    }
}
