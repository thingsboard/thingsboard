// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentUpgradeRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeDownStep;
import org.thingsboard.server.common.data.agent.step.ComposeRestartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.step.RollBackStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;

import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "edges.enabled=true",
        "agents.enabled=true",
        "agents.event.reconnect_resume_max_delay_ms=0",
        "queue.rule-engine.stats.enabled=false"
})
@Slf4j
abstract public class AbstractAgentTest extends AbstractControllerTest {

    public static final String AGENT_HOST = "localhost";
    public static final int AGENT_PORT = TestSocketUtils.findAvailableTcpPort();

    @DynamicPropertySource
    static void agentTestProps(DynamicPropertyRegistry registry) {
        registry.add("edges.rpc.port", () -> AGENT_PORT);
    }

    protected AgentImitator agentImitator;
    protected Agent agent;

    @Autowired
    protected AppTemplateRegistry appTemplateRegistry;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected AgentAppEventService agentAppEventService;

    @Autowired
    protected AgentApplicationService agentApplicationService;

    @Autowired
    protected AgentAppUnitService agentAppUnitService;

    @Before
    public void setupAgentTest() throws Exception {
        loginTenantAdmin();

        agent = createAgent("Test Agent");

        agentImitator = new AgentImitator(AGENT_HOST, AGENT_PORT,
                agent.getRoutingKey(), agent.getSecret())
                .withIdentity(agentVersion(), containerId());
        agentImitator.connect();

        Assert.assertNotNull("HelloAck should not be null", agentImitator.getHelloAck());
        Assert.assertTrue("HelloAck should be successful", agentImitator.getHelloAck().getSuccess());
    }

    @After
    public void teardownAgentTest() {
        try {
            agentImitator.disconnect();
        } catch (Exception ignored) {
        }
        try {
            loginTenantAdmin();
            doDelete("/api/agent/" + agent.getId().getId().toString())
                    .andExpect(status().isOk());
        } catch (Exception ignored) {
        }
    }

    /**
     * What the default imitator reports about itself at hello. Overridden by tests that
     * need a self-upgrade identity, since finalize dispatches on the container id.
     */
    protected String agentVersion() {
        return "";
    }

    protected String containerId() {
        return "";
    }

    // --- Agent CRUD helpers ---

    protected Agent createAgent(String name) {
        Agent newAgent = new Agent();
        newAgent.setName(name);
        newAgent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        newAgent.setSecret(StringUtils.randomAlphanumeric(20));
        return doPost("/api/agent", newAgent, Agent.class);
    }

    // --- Template helpers ---

    protected AgentAppTemplate createAgentAppTemplate(AgentApplicationType appType, String version,
                                                       AgentAppConfig config, List<AgentAppStep> startSteps) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(appType);
        template.setCurrentVersion(version);
        template.setConfigType(config != null ? config.getType() : null);
        JsonNode compose = config instanceof DockerComposeConfig dockerConfig ? dockerConfig.getCompose() : null;
        template.setStartSteps(compose != null ? withComposeTemplate(startSteps, compose) : startSteps);
        return AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }


    // Carries the compose body in a template-only ComposeTypeChoiceStep (as production templates do),
    // linked as the chain head so the step list stays a valid linked list; it is filtered out before
    // the agent executes steps, so it does not affect step-completion assertions.
    protected List<AgentAppStep> withComposeTemplate(List<AgentAppStep> startSteps, JsonNode compose) {
        return AgentTemplateTestSupport.withComposeTemplate(startSteps, compose);
    }

    protected JsonNode resolveTemplateCompose(AgentAppTemplate template) {
        return AgentTemplateTestSupport.resolveTemplateCompose(template);
    }

    protected DockerComposeConfig resolveAppConfig(AgentAppTemplate template) {
        return AgentTemplateTestSupport.resolveAppConfig(template);
    }

    protected ComposeStep createComposeStep() {
        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy compose");
        return step;
    }

    protected ComposeRestartStep createComposeRestartStep() {
        ComposeRestartStep step = new ComposeRestartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy compose restart");
        return step;
    }

    // --- Application helpers ---

    protected AgentApplication installApp(AgentAppEventRequest request) {
        return doPost("/api/agent/app/event", request, AgentAppInstallResponse.class).getApplication();
    }

    protected void createAppEvent(AgentApplicationId appId, AgentAppEventRequest request) throws Exception {
        doPost("/api/agent/app/" + appId.getId().toString() + "/event", request)
                .andExpect(status().isOk());
    }

    protected PageData<AgentApplicationInfo> getAgentApps(String agentId) {
        try {
            return doGetTypedWithPageLink("/api/agent/" + agentId + "/apps?",
                    new TypeReference<>() {}, new PageLink(100));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- Project sync helpers ---

    protected void sendProjectSync(String projectName, String composeJson,
                                    Map<String, ContainerInfo> containerStates) {
        ComposeState.Builder composeBuilder = ComposeState.newBuilder();
        if (composeJson != null) {
            composeBuilder.setComposeJson(composeJson);
        }
        if (containerStates != null) {
            composeBuilder.putAllContainerStates(containerStates);
        }
        ProjectStateSync projectSync = ProjectStateSync.newBuilder()
                .setProjectName(projectName)
                .setCompose(composeBuilder.build())
                .build();
        agentImitator.sendProjectSync(projectSync);
    }

    protected void sendProjectRemoval(String projectName) {
        ProjectStateSync projectSync = ProjectStateSync.newBuilder()
                .setProjectName(projectName)
                .setRemoved(true)
                .build();
        agentImitator.sendProjectSync(projectSync);
    }

    // --- Command helpers ---

    protected AppCommand waitForCommand() throws InterruptedException {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentImitator.hasUnconsumedCommand(c -> true));
        return agentImitator.consumeNextCommand(c -> true);
    }

    // --- Attribute verification ---

    protected void verifyAttribute(EntityId entityId, String key, Object expectedValue) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<AttributeKvEntry> attr = attributesService
                            .find(tenantId, entityId, AttributeScope.SERVER_SCOPE, key).get();
                    return attr.filter(attributeKvEntry -> String.valueOf(expectedValue).equals(
                            String.valueOf(attributeKvEntry.getValue()))).isPresent();
                });
    }

    // --- Compose JSON construction ---

    protected String constructComposeJson(Map<String, String> serviceImageMap) {
        ObjectNode root = JacksonUtil.newObjectNode();
        ObjectNode services = JacksonUtil.newObjectNode();
        serviceImageMap.forEach((name, image) -> {
            ObjectNode service = JacksonUtil.newObjectNode();
            service.put("image", image);
            services.set(name, service);
        });
        root.set("services", services);
        return root.toString();
    }

    // --- Step creation helpers ---

    protected ComposeStartStep createComposeStartStep() {
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Start compose");
        return step;
    }

    protected ComposeDownStep createComposeDownStep() {
        ComposeDownStep step = new ComposeDownStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Compose down");
        return step;
    }

    protected RollBackStep createRollbackStep() {
        RollBackStep step = new RollBackStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Rollback");
        return step;
    }

    protected List<AgentAppStep> chainSteps(AgentAppStep... steps) {
        if (steps.length == 0) return List.of();
        for (int i = 0; i < steps.length - 1; i++) {
            steps[i].setNextId(steps[i + 1].getId());
        }
        steps[steps.length - 1].setNextId(null);
        return Arrays.asList(steps);
    }

    // --- Full template creation ---

    protected AgentAppTemplate createEdgeTemplateWithSteps(String version,
                                                            List<AgentAppStep> startSteps,
                                                            List<AgentAppStep> upgradeSteps,
                                                            List<AgentAppStep> deleteSteps,
                                                            List<AgentAppStep> rollbackSteps,
                                                            List<AgentAppStep> restartSteps) {
        return createEdgeTemplateWithSteps(version, null, startSteps, upgradeSteps, deleteSteps, rollbackSteps, restartSteps);
    }

    protected AgentAppTemplate createEdgeTemplateWithSteps(String version, String nextVersion,
                                                            List<AgentAppStep> startSteps,
                                                            List<AgentAppStep> upgradeSteps,
                                                            List<AgentAppStep> deleteSteps,
                                                            List<AgentAppStep> rollbackSteps,
                                                            List<AgentAppStep> restartSteps) {
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + version));
        JsonNode compose = JacksonUtil.toJsonNode(composeJson);

        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(AgentApplicationType.EDGE);
        template.setCurrentVersion(version);
        template.setNextVersion(nextVersion);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setStartSteps(withComposeTemplate(startSteps, compose));
        template.setUpgradeSteps(upgradeSteps);
        template.setDeleteSteps(deleteSteps);
        template.setRollbackSteps(rollbackSteps);
        template.setRestartSteps(restartSteps);
        return AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }

    // --- Install helpers ---

    protected AgentApplication installEdgeApp(AgentAppTemplate template) {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agent.getId());
        app.setName("Test Edge App");
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setConfig(resolveAppConfig(template));

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        agentImitator.expectMessageAmount(1);
        return installApp(request);
    }

    // --- Event helpers ---

    protected AgentAppEventId extractEventId(AppCommand command) {
        return new AgentAppEventId(new UUID(
                command.getCommandId().getIdMSB(),
                command.getCommandId().getIdLSB()));
    }

    protected void awaitEventStatus(AgentAppEventId eventId, AgentProcessingStatus expected) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentAppEvent event = agentAppEventService.findById(tenantId, eventId);
                    return event != null && event.getProcessingStatus() == expected;
                });
    }

    protected void completeAllSteps(AppCommand cmd) throws InterruptedException {
        agentImitator.sendCommandAck(cmd.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(cmd.getCommandId(), cmd.getStepId(), true);
        for (int i = 1; i < cmd.getTotalSteps(); i++) {
            AppCommand next = waitForCommand();
            agentImitator.sendCommandAck(next.getCommandId(), AckStatus.ACCEPTED);
            agentImitator.sendCommandResult(next.getCommandId(), next.getStepId(), true);
        }
    }

    /**
     * Polls for the next unconsumed command with the given action and advances the
     * consumption cursor past it. Use this when commands of other actions may be
     * interleaved (e.g. an auto-rollback or a resumed event after reconnect);
     * {@link #waitForCommand()} returns the next command regardless of action.
     */
    protected AppCommand awaitCommand(AppCommandAction expectedAction) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentImitator.hasUnconsumedCommand(c -> c.getAction() == expectedAction));
        return agentImitator.consumeNextCommand(c -> c.getAction() == expectedAction);
    }

    // --- Self-upgrade helpers ---

    protected AgentAppEvent createUpgradeEvent(String imageRef) throws Exception {
        AgentUpgradeRequest request = new AgentUpgradeRequest();
        request.setImageRef(imageRef);
        return doPost("/api/agent/" + agent.getId().getId() + "/upgrade", request, AgentAppEvent.class);
    }

    protected AgentImitator connectImitator(String agentVersion, String containerId) throws InterruptedException {
        AgentImitator imitator = new AgentImitator(AGENT_HOST, AGENT_PORT,
                agent.getRoutingKey(), agent.getSecret())
                .withIdentity(agentVersion, containerId);
        imitator.connect();
        return imitator;
    }

    protected AppCommand awaitAgentStep(AgentImitator imitator, AgentAppStepType stepType) {
        Predicate<AppCommand> isStep = command -> command.getAction() == AppCommandAction.APP_AGENT_UPGRADE
                && stepType.name().equals(command.getMetadataMap().get("stepType"));
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> imitator.hasUnconsumedCommand(isStep));
        return imitator.consumeNextCommand(isStep);
    }

    protected boolean claimFinalize(AgentImitator imitator, AppCommand finalizeCmd, String containerId) {
        long verdictsBefore = imitator.countClaimResults(containerId);
        imitator.sendFinalizeClaim(finalizeCmd.getCommandId(), finalizeCmd.getStepId(), containerId);
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> imitator.countClaimResults(containerId) > verdictsBefore);
        return imitator.findClaimResult(containerId).orElseThrow().getGranted();
    }

    protected AgentAppEvent findLatestAgentEvent() {
        return agentAppEventService.findLatestAgentEventByAgentId(agent.getId()).orElse(null);
    }

    // --- Reconnect helpers ---

    protected void reconnectAgent() throws InterruptedException {
        agentImitator.disconnect();
        agentImitator = new AgentImitator(AGENT_HOST, AGENT_PORT,
                agent.getRoutingKey(), agent.getSecret())
                .withIdentity(agentVersion(), containerId());
        agentImitator.connect();
        Assert.assertNotNull("HelloAck should not be null after reconnect", agentImitator.getHelloAck());
        Assert.assertTrue("HelloAck should be successful after reconnect", agentImitator.getHelloAck().getSuccess());
    }
}
