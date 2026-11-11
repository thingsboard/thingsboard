// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.springframework.dao.DataIntegrityViolationException;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.agent.AgentUpgradeRequest;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.service.agent.DefaultAgentStateService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@DaoSqlTest
@TestPropertySource(properties = {
        "agents.upgrade.finalize_timeout_ms=5000",
        // Resume carries a jitter of up to 5s in production; pinned to 0 so a claim can never
        // land past a deliberately short deadline just because the resume was slow.
        "agents.event.reconnect_resume_max_delay_ms=0",
        // The sweeper is held off here: these tests assert who may win a claim, and a sweep
        // racing the deadline would make a denial ambiguous (terminal event vs winner taken).
        "agents.upgrade.finalize_grace_ms=600000"
})
public class SelfUpgradeAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";
    private static final String IMAGE_OLD = "thingsboard/tb-remote-agent:test-old";
    private static final String IMAGE_NEW = "thingsboard/tb-remote-agent:test-new";
    private static final String OLD_CONTAINER = "aaaaaaaaaaaa0000000000000000000000000000000000000000000000000001";
    private static final String NEW_CONTAINER = "bbbbbbbbbbbb0000000000000000000000000000000000000000000000000002";

    private AgentImitator takeover;

    @Override
    protected String agentVersion() {
        return IMAGE_OLD;
    }

    @Override
    protected String containerId() {
        return OLD_CONTAINER;
    }

    @After
    public void disconnectTakeover() {
        if (takeover != null) {
            try {
                takeover.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testSwapCompletesWhenTheCopyTakesOver() throws Exception {
        AgentAppEvent event = createUpgradeEvent(IMAGE_NEW);

        AppCommand prepare = awaitAgentStep(agentImitator, AgentAppStepType.AGENT_PREPARE);
        Assert.assertEquals(AppCommandAction.APP_AGENT_UPGRADE, prepare.getAction());
        Assert.assertEquals(IMAGE_NEW, prepare.getMetadataMap().get(AgentUpgradeKeys.IMAGE_REF));
        Assert.assertEquals("agent-scoped commands carry no application", "", prepare.getAppName());

        agentImitator.sendCommandAck(prepare.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(prepare.getCommandId(), prepare.getStepId(), true,
                Map.of(AgentUpgradeKeys.OLD_CONTAINER_ID, OLD_CONTAINER,
                        AgentUpgradeKeys.NEW_CONTAINER_ID, NEW_CONTAINER));

        // Finalize first reaches the instance that is still holding the session.
        AppCommand finalizeToOld = awaitAgentStep(agentImitator, AgentAppStepType.AGENT_FINALIZE);
        Assert.assertEquals(OLD_CONTAINER, finalizeToOld.getMetadataMap().get(AgentUpgradeKeys.OLD_CONTAINER_ID));
        Assert.assertEquals(NEW_CONTAINER, finalizeToOld.getMetadataMap().get(AgentUpgradeKeys.NEW_CONTAINER_ID));
        Assert.assertNotNull("finalize must carry a deadline on the cloud clock",
                finalizeToOld.getMetadataMap().get(AgentUpgradeKeys.FINALIZE_DEADLINE_TS));

        // The copy boots, takes the session, and the server resumes finalize to it.
        takeover = connectImitator(IMAGE_NEW, NEW_CONTAINER);
        // the reported version is written at hello time, so this is the takeover connect being recorded,
        // not the upgrade being applied
        verifyAttribute(agent.getId(), DefaultAgentStateService.AGENT_VERSION, IMAGE_NEW);
        AppCommand finalizeToNew = awaitAgentStep(takeover, AgentAppStepType.AGENT_FINALIZE);
        Assert.assertEquals("the resumed step is the same event",
                finalizeToOld.getCommandId(), finalizeToNew.getCommandId());

        Assert.assertTrue("before the deadline only the copy may win",
                claimFinalize(takeover, finalizeToNew, NEW_CONTAINER));

        takeover.sendCommandResult(finalizeToNew.getCommandId(), finalizeToNew.getStepId(), true);

        awaitEventStatus(event.getId(), AgentProcessingStatus.FINISHED);
        Assert.assertFalse("the gate must lift once the upgrade finishes",
                agentAppEventService.hasActiveOrPendingAgentEvent(agent.getId()));
    }

    /**
     * The copy never connects. Authority to reclaim comes from the deadline on the cloud
     * clock, not from the agent's own timer, so the previous instance can only win after it.
     */
    @Test
    public void testPreviousInstanceReclaimsOnlyAfterTheDeadline() throws Exception {
        AgentAppEvent event = createUpgradeEvent(IMAGE_NEW);
        AppCommand finalizeCmd = runPrepareAndAwaitFinalize();

        Assert.assertFalse("before the deadline the previous instance may not win",
                claimFinalize(agentImitator, finalizeCmd, OLD_CONTAINER));

        awaitDeadline(finalizeCmd);

        Assert.assertTrue("after the deadline the previous instance reclaims",
                claimFinalize(agentImitator, finalizeCmd, OLD_CONTAINER));

        agentImitator.sendCommandResult(finalizeCmd.getCommandId(), finalizeCmd.getStepId(), false);

        awaitEventStatus(event.getId(), AgentProcessingStatus.ERROR);
        Assert.assertFalse("the gate must lift so the agent can serve app commands again",
                agentAppEventService.hasActiveOrPendingAgentEvent(agent.getId()));
    }

    /**
     * The winner is single-shot: once assigned it is never re-assigned, on any clock. Each
     * claimant has to hold the session to claim at all, so the loser only gets its verdict
     * after reconnecting — which is exactly how the reclaim path reaches the cloud.
     */
    @Test
    public void testTheWinnerIsNeverReassigned() throws Exception {
        createUpgradeEvent(IMAGE_NEW);
        runPrepareAndAwaitFinalize();

        takeover = connectImitator(IMAGE_NEW, NEW_CONTAINER);
        AppCommand finalizeToNew = awaitAgentStep(takeover, AgentAppStepType.AGENT_FINALIZE);
        Assert.assertTrue(claimFinalize(takeover, finalizeToNew, NEW_CONTAINER));

        // The previous instance comes back and takes the session; it must still lose.
        reconnectAgent();
        AppCommand finalizeToOld = awaitAgentStep(agentImitator, AgentAppStepType.AGENT_FINALIZE);
        Assert.assertFalse("a second claimant must be denied while a winner exists",
                claimFinalize(agentImitator, finalizeToOld, OLD_CONTAINER));

        awaitDeadline(finalizeToOld);
        Assert.assertFalse("the deadline gates only the first assignment, not later claims",
                claimFinalize(agentImitator, finalizeToOld, OLD_CONTAINER));

        // A restarted winner must always be able to finish, deadline or not.
        takeover.disconnect();
        takeover = connectImitator(IMAGE_NEW, NEW_CONTAINER);
        AppCommand resumed = awaitAgentStep(takeover, AgentAppStepType.AGENT_FINALIZE);
        Assert.assertTrue("the current winner is re-granted unconditionally",
                claimFinalize(takeover, resumed, NEW_CONTAINER));
    }

    /** One arbitration domain per agent, so a second upgrade is refused while one is live. */
    @Test
    public void testASecondUpgradeIsRefusedWhileOneIsActive() throws Exception {
        createUpgradeEvent(IMAGE_NEW);
        awaitAgentStep(agentImitator, AgentAppStepType.AGENT_PREPARE);

        AgentUpgradeRequest second = new AgentUpgradeRequest();
        second.setImageRef(IMAGE_NEW);
        doPost("/api/agent/" + agent.getId().getId() + "/upgrade", second)
                .andExpect(status().isBadRequest());
    }

    /**
     * The admission pre-check races across nodes, so the partial unique index is what actually
     * enforces a single active agent event — this pins that the constraint exists.
     */
    @Test
    public void testTheUniqueIndexRefusesASecondActiveAgentEvent() throws Exception {
        createUpgradeEvent(IMAGE_NEW);
        awaitAgentStep(agentImitator, AgentAppStepType.AGENT_PREPARE);

        AgentAppEvent duplicate = new AgentAppEvent();
        duplicate.setTenantId(tenantId);
        duplicate.setAgentId(agent.getId());
        duplicate.setActionType(AgentAppEventActionType.AGENT_UPGRADE);
        duplicate.setStartStatus(ProcessingStartStatus.PENDING);
        duplicate.setUpdatedTime(System.currentTimeMillis());

        Assert.assertThrows("the index must reject a concurrent active agent event",
                DataIntegrityViolationException.class,
                () -> agentAppEventService.save(tenantId, duplicate));
    }

    /**
     * One active agent event blocks every app operation on that agent, and vice versa — the two
     * pipelines share one host, so they are never allowed to run at each other.
     */
    @Test
    public void testAppEventsAreRefusedWhileAnUpgradeRunsAndResumeAfterwards() throws Exception {
        AgentApplication app = installAndCompleteApp();

        AgentAppEvent upgrade = createUpgradeEvent(IMAGE_NEW);
        AppCommand finalizeCmd = runPrepareAndAwaitFinalize();

        doPost("/api/agent/app/" + app.getId().getId() + "/event", restartRequest())
                .andExpect(status().isBadRequest());

        // Let the upgrade end the only way it can with no replacement: reclaimed on the deadline.
        awaitDeadline(finalizeCmd);
        Assert.assertTrue(claimFinalize(agentImitator, finalizeCmd, OLD_CONTAINER));
        agentImitator.sendCommandResult(finalizeCmd.getCommandId(), finalizeCmd.getStepId(), false);
        awaitEventStatus(upgrade.getId(), AgentProcessingStatus.ERROR);

        // Gate lifted: the same request is accepted and reaches the agent.
        createAppEvent(app.getId(), restartRequest());
        awaitCommand(AppCommandAction.APP_RESTART);
    }

    @Test
    public void testAnUpgradeIsRefusedWhileAnAppEventIsActive() throws Exception {
        AgentApplication app = installAndCompleteApp();

        createAppEvent(app.getId(), restartRequest());
        awaitCommand(AppCommandAction.APP_RESTART);

        AgentUpgradeRequest upgrade = new AgentUpgradeRequest();
        upgrade.setImageRef(IMAGE_NEW);
        doPost("/api/agent/" + agent.getId().getId() + "/upgrade", upgrade)
                .andExpect(status().isBadRequest());
    }

    private AgentApplication installAndCompleteApp() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()), null, null, null,
                chainSteps(createComposeRestartStep()));
        AgentApplication app = installEdgeApp(template);
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentProcessingStatus.FINISHED);
        return app;
    }

    private AgentAppEventRequest restartRequest() {
        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);
        return request;
    }

    // --- helpers ---

    private AppCommand runPrepareAndAwaitFinalize() throws Exception {
        AppCommand prepare = awaitAgentStep(agentImitator, AgentAppStepType.AGENT_PREPARE);
        agentImitator.sendCommandAck(prepare.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(prepare.getCommandId(), prepare.getStepId(), true,
                Map.of(AgentUpgradeKeys.OLD_CONTAINER_ID, OLD_CONTAINER,
                        AgentUpgradeKeys.NEW_CONTAINER_ID, NEW_CONTAINER));
        return awaitAgentStep(agentImitator, AgentAppStepType.AGENT_FINALIZE);
    }

    private void awaitDeadline(AppCommand finalizeCmd) throws InterruptedException {
        long deadline = Long.parseLong(finalizeCmd.getMetadataMap().get(AgentUpgradeKeys.FINALIZE_DEADLINE_TS));
        long remaining = deadline - System.currentTimeMillis();
        if (remaining > 0) {
            TimeUnit.MILLISECONDS.sleep(remaining + 200);
        }
    }
}
