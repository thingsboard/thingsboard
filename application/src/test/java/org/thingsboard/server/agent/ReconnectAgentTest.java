// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.step.ComposeRestartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;

import java.util.concurrent.TimeUnit;

@DaoSqlTest
public class ReconnectAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";

    @Test
    public void testReconnectResumesInFlightEvent() throws Exception {
        // Create template with 2 start steps
        ComposeStep composeStep = createComposeStep();
        ComposeStartStep startStep = createComposeStartStep();
        ComposeRestartStep restartStep = createComposeRestartStep();
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(composeStep, startStep), null, null, null,
                chainSteps(restartStep));

        installEdgeApp(template);

        // Receive first step command
        AppCommand firstCmd = waitForCommand();
        AgentAppEventId eventId = extractEventId(firstCmd);
        Assert.assertEquals(AppCommandAction.APP_INSTALL, firstCmd.getAction());
        Assert.assertEquals(2, firstCmd.getTotalSteps());

        // Ack to make event in-flight (delivered state)
        agentImitator.sendCommandAck(firstCmd.getCommandId(), AckStatus.ACCEPTED);

        // Disconnect and reconnect
        reconnectAgent();

        // Server should resume the in-flight event by resending the current step.
        // Use awaitCommand (poll-based) since the resume message may arrive
        // before a latch-based waitForCommand can be set up.
        AppCommand resumedCmd = awaitCommand(AppCommandAction.APP_INSTALL);

        Assert.assertEquals("Resumed command should have same event id",
                firstCmd.getCommandId(), resumedCmd.getCommandId());

        // Complete step 1
        agentImitator.sendCommandAck(resumedCmd.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(resumedCmd.getCommandId(), resumedCmd.getStepId(), true);

        // Wait for and complete step 2, skipping any duplicate resend of step 1
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentImitator.hasUnconsumedCommand(c -> !c.getStepId().equals(resumedCmd.getStepId())));
        AppCommand step2Cmd = agentImitator.consumeNextCommand(c -> !c.getStepId().equals(resumedCmd.getStepId()));
        agentImitator.sendCommandAck(step2Cmd.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(step2Cmd.getCommandId(), step2Cmd.getStepId(), true);

        awaitEventStatus(eventId, AgentProcessingStatus.FINISHED);
    }

    @Test
    public void testReconnectDispatchesPendingWhenNoInFlight() throws Exception {
        ComposeRestartStep restartStep = createComposeRestartStep();
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()), null, null, null, chainSteps(restartStep));

        AgentApplication app = installEdgeApp(template);

        // Complete install
        AppCommand installCmd = waitForCommand();
        completeAllSteps(installCmd);
        awaitEventStatus(extractEventId(installCmd), AgentProcessingStatus.FINISHED);

        // Create a RESTART event
        AgentAppEventRequest restartRequest = new AgentAppEventRequest();
        restartRequest.setActionType(AgentAppEventActionType.RESTART);
        createAppEvent(app.getId(), restartRequest);

        // Disconnect and reconnect
        reconnectAgent();

        // On reconnect, resumeEventsOnReconnect finds no in-flight event,
        // dispatches pending RESTART. Use poll-based wait.
        AppCommand restartCmd = awaitCommand(AppCommandAction.APP_RESTART);

        AgentAppEventId restartEventId = extractEventId(restartCmd);
        completeAllSteps(restartCmd);
        awaitEventStatus(restartEventId, AgentProcessingStatus.FINISHED);
    }
}
