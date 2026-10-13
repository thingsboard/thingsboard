// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;

import java.util.Map;

/**
 * The deadline sweep is the only recovery when nobody is left to report an outcome: it runs
 * off the event's own deadline, with no session and no agent involved. Its own class because
 * it needs the sweep clocks short, which would race the fencing assertions elsewhere.
 */
@DaoSqlTest
@TestPropertySource(properties = {
        "agents.upgrade.finalize_timeout_ms=2000",
        "agents.event.reconnect_resume_max_delay_ms=0",
        "agents.upgrade.finalize_grace_ms=1000",
        "agents.upgrade.sweep_interval_ms=1000"
})
public class SelfUpgradeSweepAgentTest extends AbstractAgentTest {

    private static final String IMAGE_OLD = "thingsboard/tb-remote-agent:test-old";
    private static final String IMAGE_NEW = "thingsboard/tb-remote-agent:test-new";
    private static final String OLD_CONTAINER = "aaaaaaaaaaaa0000000000000000000000000000000000000000000000000001";
    private static final String NEW_CONTAINER = "bbbbbbbbbbbb0000000000000000000000000000000000000000000000000002";

    @Override
    protected String agentVersion() {
        return IMAGE_OLD;
    }

    @Override
    protected String containerId() {
        return OLD_CONTAINER;
    }

    @Test
    public void testTheSweepEndsAnUpgradeNobodyIsLeftToFinish() throws Exception {
        AgentAppEvent event = createUpgradeEvent(IMAGE_NEW);

        AppCommand prepare = awaitAgentStep(agentImitator, AgentAppStepType.AGENT_PREPARE);
        agentImitator.sendCommandAck(prepare.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(prepare.getCommandId(), prepare.getStepId(), true,
                Map.of(AgentUpgradeKeys.OLD_CONTAINER_ID, OLD_CONTAINER,
                        AgentUpgradeKeys.NEW_CONTAINER_ID, NEW_CONTAINER));
        awaitAgentStep(agentImitator, AgentAppStepType.AGENT_FINALIZE);

        // Nobody is connected any more: no claim can arrive and no result can be reported.
        agentImitator.disconnect();

        awaitEventStatus(event.getId(), AgentProcessingStatus.ERROR);
        Assert.assertFalse("the gate must lift with no agent involved",
                agentAppEventService.hasActiveOrPendingAgentEvent(agent.getId()));
    }
}
