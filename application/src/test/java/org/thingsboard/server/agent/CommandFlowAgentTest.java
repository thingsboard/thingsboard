// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeServicesStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;

import java.util.List;
import java.util.Map;

@DaoSqlTest
public class CommandFlowAgentTest extends AbstractAgentTest {

    @Test
    public void testAgentReceivesAppCommand() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("4.3.0EDGE");

        AgentApplication app = installEdgeApp(template);
        Assert.assertNotNull(app.getId());

        AppCommand command = waitForCommand();
        Assert.assertEquals(AppCommandAction.APP_INSTALL, command.getAction());
        Assert.assertFalse(command.getAppName().isEmpty());
    }

    @Test
    public void testAppCommandCarriesTheDeployableCompose() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("4.3.0EDGE");
        AgentApplication app = installEdgeApp(template);

        AppCommand command = waitForCommand();

        String compose = command.getMetadataMap().get(ComposeServicesStep.COMPOSE);
        Assert.assertNotNull("AppCommand carries no compose to deploy", compose);
        Assert.assertFalse("Compose still carries unsubstituted arguments: " + compose,
                compose.contains("${tb."));
        JsonNode composeNode = JacksonUtil.toJsonNode(compose);
        Assert.assertEquals("thingsboard/tb-edge-pe:4.3.0EDGE",
                composeNode.get("services").get("tb-edge").get("image").asText());
        Assert.assertEquals(app.getProjectName(), command.getMetadataMap().get("projectName"));
    }

    @Test
    public void testCommandAckAndResultFlow() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("4.3.0EDGE");
        installEdgeApp(template);

        AppCommand command = waitForCommand();
        AgentAppEventId eventId = extractEventId(command);

        completeAllSteps(command);

        awaitEventStatus(eventId, AgentProcessingStatus.FINISHED);
    }

    @Test
    public void testCommandResultFailure() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("4.3.0EDGE");
        installEdgeApp(template);

        AppCommand command = waitForCommand();
        AgentAppEventId eventId = extractEventId(command);

        agentImitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        agentImitator.sendCommandResult(command.getCommandId(), command.getStepId(), false);

        awaitEventStatus(eventId, AgentProcessingStatus.ERROR);
    }

    // --- Private helpers ---

    private AgentAppTemplate createEdgeTemplate(String version) {
        DockerComposeConfig config = new DockerComposeConfig();
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + version));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));

        ComposeStep step = createComposeStep();

        return createAgentAppTemplate(AgentApplicationType.EDGE, version,
                config, List.of(step));
    }

    // installEdgeApp is inherited from AbstractAgentTest
}
