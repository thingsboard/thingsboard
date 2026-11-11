// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.gen.agent.v1.StartLogStream;
import org.thingsboard.server.service.ws.log.cmd.LogsSubscriptionCmd;
import org.thingsboard.server.service.ws.log.cmd.LogsUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LogsUpdate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The log stream is long and entirely wiring: a websocket subscription on an {@code AGENT_APP_UNIT} has to
 * reach the core owning the gRPC session, turn into a {@code StartLogStream} on the wire, and the agent's
 * {@code AgentLogChunk} has to find its way back to that subscriber - and the mirror of all that on the last
 * unsubscribe, which is what stops an agent streaming forever. Every seam is otherwise only covered against
 * mocks.
 */
@DaoSqlTest
public class AgentLogStreamAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";
    private static final String PROJECT_NAME = "log-stream-project";
    private static final String UNIT_IDENTIFIER = "tb-edge";

    @Test
    public void testFirstSubscriberStartsTheStreamAndChunksReachTheSubscriber() throws Exception {
        AgentAppUnit unit = syncProjectAndAwaitUnit();

        TbTestWebSocketClient wsClient = getWsClient();
        wsClient.send(new LogsSubscriptionCmd(1, unit.getId().getEntityType().name(),
                unit.getId().getId().toString(), 0));

        StartLogStream start = awaitStartLogStream();
        assertThat(start.getUnitId()).isEqualTo(UNIT_IDENTIFIER);
        assertThat(start.getProjectName()).isEqualTo(PROJECT_NAME);

        wsClient.registerWaitForUpdate();
        agentImitator.sendLogChunk(AgentLogChunk.newBuilder()
                .setUnitId(UNIT_IDENTIFIER)
                .setProjectName(PROJECT_NAME)
                .addAllLines(List.of("starting edge", "edge started"))
                .setDropped(0)
                .setLastLineTs(System.currentTimeMillis())
                .build());

        LogsUpdate update = JacksonUtil.fromString(wsClient.waitForUpdate(true), LogsUpdate.class);
        assertThat(update.getCmdId()).isEqualTo(1);
        assertThat(update.getLines()).containsExactly("starting edge", "edge started");
        assertThat(update.getDroppedLines()).isZero();

        wsClient.send(new LogsUnsubscribeCmd(1));
        awaitStopLogStream();
    }

    @Test
    public void testChunkForAnUnknownUnitStopsTheStream() throws Exception {
        syncProjectAndAwaitUnit();

        agentImitator.sendLogChunk(AgentLogChunk.newBuilder()
                .setUnitId("no-such-container")
                .setProjectName(PROJECT_NAME)
                .addLines("orphan line")
                .setLastLineTs(System.currentTimeMillis())
                .build());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentImitator.getStopLogStreamRequests().stream()
                        .anyMatch(stop -> "no-such-container".equals(stop.getUnitId())));
    }

    private AgentAppUnit syncProjectAndAwaitUnit() {
        createEdgeTemplate();
        sendProjectSync(PROJECT_NAME, constructComposeJson(
                Map.of(UNIT_IDENTIFIER, "thingsboard/tb-edge-pe:" + EDGE_VERSION)), Collections.emptyMap());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> findUnit() != null);
        return findUnit();
    }

    private AgentAppUnit findUnit() {
        return agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), PROJECT_NAME, UNIT_IDENTIFIER, AgentAppUnitType.CONTAINER);
    }

    private StartLogStream awaitStartLogStream() {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !agentImitator.getStartLogStreamRequests().isEmpty());
        List<StartLogStream> requests = agentImitator.getStartLogStreamRequests();
        return requests.get(requests.size() - 1);
    }

    private void awaitStopLogStream() {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentImitator.getStopLogStreamRequests().stream()
                        .anyMatch(stop -> UNIT_IDENTIFIER.equals(stop.getUnitId())
                                && PROJECT_NAME.equals(stop.getProjectName())));
    }

    private void createEdgeTemplate() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.toJsonNode(constructComposeJson(
                Map.of(UNIT_IDENTIFIER, "thingsboard/tb-edge-pe:" + EDGE_VERSION))));
        createAgentAppTemplate(AgentApplicationType.EDGE, EDGE_VERSION, config, List.of(createComposeStep()));
    }
}
