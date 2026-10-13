// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import io.grpc.Status;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.dao.service.DaoSqlTest;

@DaoSqlTest
public class ConnectionAgentTest extends AbstractAgentTest {

    @Test
    public void testAgentConnectsSuccessfully() {
        // Agent connects in @Before — just verify the activity attribute
        verifyAttribute(agent.getId(), "active", true);
    }

    @Test
    public void testAgentAuthFailsWithWrongSecret() throws Exception {
        AgentImitator badImitator = new AgentImitator(AGENT_HOST, AGENT_PORT,
                agent.getRoutingKey(), "wrong-secret");
        try {
            Status status = badImitator.connectExpectingError();
            Assert.assertNotNull("Expected an error status", status);
            Assert.assertEquals(Status.Code.UNAUTHENTICATED, status.getCode());
        } finally {
            badImitator.disconnect();
        }
    }

    @Test
    public void testAgentDisconnect() throws Exception {
        // Verify agent is active
        verifyAttribute(agent.getId(), "active", true);

        // Disconnect
        agentImitator.disconnect();

        // Verify agent becomes inactive
        verifyAttribute(agent.getId(), "active", false);

        // Reconnect so teardown works cleanly
        agentImitator = new AgentImitator(AGENT_HOST, AGENT_PORT,
                agent.getRoutingKey(), agent.getSecret());
        agentImitator.connect();
    }
}
