// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;

import java.util.concurrent.TimeUnit;

@DaoSqlTest
@TestPropertySource(properties = {
        "agents.event.watchdog_initial_delay_ms=1000",
        "agents.event.reconnect_resume_max_delay_ms=0"
})
public class EventWatchdogAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";
    private static final long WATCHDOG_DELAY_MS = 1000;
    private static final long PROGRESS_INTERVAL_MS = WATCHDOG_DELAY_MS / 4;
    private static final int PROGRESS_WINDOWS = 3;
    private static final String PROGRESS_MESSAGE = "Pulling images";

    @Test
    public void testWatchdogResendsStaleEvent() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null, null, null, null);
        installEdgeApp(template);

        // Receive first command
        AppCommand firstCommand = waitForCommand();
        AgentAppEventId eventId = extractEventId(firstCommand);

        // Do NOT respond — let the watchdog detect staleness and resend
        agentImitator.expectMessageAmount(1);
        agentImitator.waitForMessages();

        // Verify the resent command has the same commandId and stepId
        AppCommand resent = agentImitator.getLatestCommand();
        Assert.assertEquals("Resent command should have same commandId",
                firstCommand.getCommandId(), resent.getCommandId());
        Assert.assertEquals("Resent command should have same stepId",
                firstCommand.getStepId(), resent.getStepId());

        // Now complete the event
        completeAllSteps(resent);
        awaitEventStatus(eventId, AgentProcessingStatus.FINISHED);
    }

    /**
     * The reported-progress branch: the step is deliberately left incomplete, so the only thing standing
     * between the event and a resend is the {@code updatedTime > scheduledAt} reschedule. Progress is sent as
     * a heartbeat several times per watchdog window, and the assertion is that no *further* command arrives
     * while it runs - the count is snapshotted once the first progress is known to be applied, because the
     * window that fires before any progress has landed is the watchdog correctly acting on a stale event, not
     * the branch under test.
     */
    @Test
    public void testWatchdogDoesNotResendOnProgress() throws Exception {
        AgentAppTemplate template = createEdgeTemplateWithSteps(EDGE_VERSION,
                chainSteps(createComposeStep()),
                null, null, null, null);
        installEdgeApp(template);

        AppCommand command = waitForCommand();
        AgentAppEventId eventId = extractEventId(command);
        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        sendProgress(command);

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    AgentAppEvent event = agentAppEventService.findById(tenantId, eventId);
                    return event != null && event.getProcessingStatus() == AgentProcessingStatus.PROCESSING
                            && PROGRESS_MESSAGE.equals(event.getCurrentActivity());
                });
        int commandsBeforeHeartbeat = agentImitator.getReceivedCommands().size();

        long heartbeatUntil = System.currentTimeMillis() + PROGRESS_WINDOWS * WATCHDOG_DELAY_MS;
        while (System.currentTimeMillis() < heartbeatUntil) {
            sendProgress(command);
            Thread.sleep(PROGRESS_INTERVAL_MS);
        }

        Assert.assertEquals("The watchdog resent the step although the agent kept reporting progress",
                commandsBeforeHeartbeat, agentImitator.getReceivedCommands().size());
    }

    private void sendProgress(AppCommand command) {
        agentImitator.sendCommandProgress(command.getCommandId(), command.getStepId(), "pull", PROGRESS_MESSAGE);
    }

}
