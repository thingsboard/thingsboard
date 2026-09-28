// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DaoSqlTest
public class ProjectSyncAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";

    @Test
    public void testProjectSyncCreatesApplication() {
        createEdgeTemplate();

        String projectName = "test-project";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        // Wait for app to be created
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplicationInfo> apps = getAgentApps(
                agent.getId().getId().toString());
        Assert.assertEquals(1, apps.getTotalElements());

        AgentApplicationInfo app = apps.getData().get(0);
        Assert.assertEquals(AgentApplicationType.EDGE, app.getAppType());
        Assert.assertEquals(agent.getId(), app.getAgentId());
    }

    @Test
    public void testProjectSyncUpdatesContainerState() {
        createEdgeTemplate();

        String projectName = "state-project";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        // First sync to create app
        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        // Second sync with container states
        sendProjectSync(projectName, null,
                Map.of("tb-edge", ContainerInfo.newBuilder()
                        .setState("running")
                        .setImageDigest("sha256:abc123")
                        .build()));

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> findUnit(projectName, "tb-edge") != null);
        AgentAppUnit unit = findUnit(projectName, "tb-edge");
        Assert.assertEquals(AgentAppUnitType.CONTAINER, unit.getType());
        verifyAttribute(unit.getId(), "state", "running");
    }

    @Test
    public void testProjectRemoval() {
        createEdgeTemplate();

        String projectName = "removal-project";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        // Create app via sync
        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        AgentApplicationId appId = getAgentApps(agent.getId().getId().toString()).getData().get(0).getId();
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, appId).isEmpty());

        // Send removal — this deletes units, not the app itself
        sendProjectRemoval(projectName);

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, appId).isEmpty());

        // Verify app still exists (removal deletes units, not the application)
        PageData<AgentApplicationInfo> apps = getAgentApps(agent.getId().getId().toString());
        Assert.assertEquals(1, apps.getTotalElements());
    }

    private AgentAppUnit findUnit(String projectName, String identifier) {
        return agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), projectName, identifier, AgentAppUnitType.CONTAINER);
    }

    private void createEdgeTemplate() {
        DockerComposeConfig config = new DockerComposeConfig();
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));

        ComposeStep step = createComposeStep();

        createAgentAppTemplate(AgentApplicationType.EDGE, EDGE_VERSION,
                config, List.of(step));
    }
}
