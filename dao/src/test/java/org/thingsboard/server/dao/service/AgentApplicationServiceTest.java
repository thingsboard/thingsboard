// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppRelationService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class AgentApplicationServiceTest extends AbstractServiceTest {

    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppEventService agentAppEventService;
    @Autowired
    EdgeService edgeService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    DeviceCredentialsService deviceCredentialsService;
    @Autowired
    AgentAppRelationService agentAppRelationService;

    @Test
    public void testSave() {
        Agent agent = createAgent("My agent");
        AgentAppTemplate template = createTemplate();

        AgentApplication saved = saveApplicationWithEdgeConfig(agent, "app1", "routing-key1", template.getCurrentVersion());

        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(template.getCurrentVersion(), saved.getTemplateVersion());
        Assert.assertEquals(agent.getId(), saved.getAgentId());

        AgentApplication found = agentApplicationService.findById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());

        List<AgentApplication> byAgent = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, byAgent.size());
        Assert.assertEquals(saved.getId(), byAgent.get(0).getId());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentApplicationWithNullAgentId() {
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.save(tenantId, app));
    }

    @Test
    public void testSaveAgentApplicationWithNonExistentAgent() {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(new AgentId(UUID.randomUUID()));
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeComposeJson("routing-key"));
        app.setConfig(config);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.save(tenantId, app));
        Assert.assertEquals("Agent application is referencing non-existent agent!", exception.getMessage());
    }

    @Test
    public void testFindAllByAgentId() {
        Agent agent = createAgent("Agent for list");
        AgentApplication app1 = saveApplicationWithEdgeConfig(agent, "app1", "routing-key1");
        AgentApplication app2 = saveApplicationWithEdgeConfig(agent, "app2", "routing-key2");

        List<AgentApplication> list = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(2, list.size());

        agentApplicationService.delete(tenantId, app1.getId());
        agentApplicationService.delete(tenantId, app2.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDelete() throws Exception {
        Agent agent = createAgent("Agent for delete");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "toDelete", "routing-key");

        agentApplicationService.delete(tenantId, app.getId());
        AgentApplication found = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertNull(found);

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteByAgentId() {
        Agent agent = createAgent("Agent for deleteByAgentId");
        saveApplicationWithEdgeConfig(agent, "a1", "rk1");
        saveApplicationWithEdgeConfig(agent, "a2", "rk2");

        List<AgentApplication> before = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(2, before.size());

        agentApplicationService.deleteByAgentId(tenantId, agent.getId());
        List<AgentApplication> after = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertTrue(after.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdateAgentApplication() throws Exception {
        Agent agent = createAgent("Agent for update");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "v1", "rk1");

        app.setName("v2");
        AgentApplication updated = agentApplicationService.save(tenantId, app);
        Assert.assertEquals("v2", updated.getName());

        AgentApplication found = agentApplicationService.findById(tenantId, app.getId());
        Assert.assertEquals("v2", found.getName());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByEventId() {
        Agent agent = createAgent("Agent for findByEventId");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "eventApp", "rk1");

        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(app.getId());
        event.setAgentId(agent.getId());
        event.setApplicationName(app.getName());
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setStartStatus(ProcessingStartStatus.PENDING);
        event.setProcessingStatus(AgentProcessingStatus.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        AgentAppEvent savedEvent = agentAppEventService.save(tenantId, event);

        AgentApplication found = agentApplicationService.findByEventId(tenantId, savedEvent.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        AgentApplication notFound = agentApplicationService.findByEventId(tenantId, new AgentAppEventId(UUID.randomUUID()));
        Assert.assertNull(notFound);

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_projectNameIsPreserved() {
        Agent agent = createAgent("Agent for project name update");

        AgentApplication saved = saveApplicationWithEdgeConfig(agent, "app1", "rk1");
        String originalProjectName = saved.getProjectName();
        Assert.assertNotNull(originalProjectName);

        saved.setName("updated-name");
        AgentApplication updated = agentApplicationService.save(tenantId, saved);

        Assert.assertEquals("Project name should be preserved on update", originalProjectName, updated.getProjectName());

        agentApplicationService.delete(tenantId, updated.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveEdgeAppResolvesRelatedEntity() {
        Agent agent = createAgent("Agent for edge resolution");
        Edge edge = createEdge("Test Edge", "test-routing-key");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "edgeApp", edge.getRoutingKey());

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, edge.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveEdgeAppWithMissingRoutingKey() {
        Agent agent = createAgent("Agent for missing key");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "noKeyApp", null);

        Assert.assertNull(agentApplicationService.findInfoById(tenantId, app.getId()).getRelatedEntityId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveEdgeAppWithUnknownRoutingKey() {
        Agent agent = createAgent("Agent for unknown key");
        AgentApplication app = saveApplicationWithEdgeConfig(agent, "unknownKeyApp", "non-existent-key");

        Assert.assertNull(agentApplicationService.findInfoById(tenantId, app.getId()).getRelatedEntityId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGenericAppNoRelatedEntity() {
        Agent agent = createAgent("Agent for generic");
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createGenericComposeJson());
        app.setConfig(config);

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertNull(agentApplicationService.findInfoById(tenantId, saved.getId()).getRelatedEntityId());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testReassignEdge_movesRelationToNewEdge() {
        Agent agent = createAgent("Agent for update edge");
        Edge edge1 = createEdge("Edge 1", "routing-key-1");
        Edge edge2 = createEdge("Edge 2", "routing-key-2");

        AgentApplication app = saveApplicationWithEdgeConfig(agent, "updateEdgeApp", edge1.getRoutingKey());
        Assert.assertNotNull(agentApplicationService.findByRelatedEntity(tenantId, edge1.getId()));

        agentApplicationService.assignRelatedEntity(tenantId, app.getId(), edge2.getId());

        Assert.assertNull(agentApplicationService.findByRelatedEntity(tenantId, edge1.getId()));
        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, edge2.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        edgeService.deleteEdge(tenantId, edge1.getId());
        edgeService.deleteEdge(tenantId, edge2.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByRelatedEntity() {
        Agent agent = createAgent("Agent for findByRelated");
        Edge edge = createEdge("Related Edge", "related-key");

        AgentApplication app1 = saveApplicationWithEdgeConfig(agent, "related1", edge.getRoutingKey());

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, edge.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app1.getId(), found.getId());

        agentApplicationService.delete(tenantId, app1.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveApplicationWithOrigin() {
        Agent agent = createAgent("Agent for origin");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeComposeJson("rk1"));
        app.setConfig(config);

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertEquals(AgentApplicationOrigin.INSTALLED, saved.getOrigin());

        AgentApplication found = agentApplicationService.findById(tenantId, saved.getId());
        Assert.assertEquals(AgentApplicationOrigin.INSTALLED, found.getOrigin());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveApplicationWithDiscoveredOrigin() {
        Agent agent = createAgent("Agent for discovered origin");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.DISCOVERED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createGenericComposeJson());
        app.setConfig(config);


        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertEquals(AgentApplicationOrigin.DISCOVERED, saved.getOrigin());

        AgentApplication found = agentApplicationService.findById(tenantId, saved.getId());
        Assert.assertEquals(AgentApplicationOrigin.DISCOVERED, found.getOrigin());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveApplicationWithNullOrigin_thenException() {
        Agent agent = createAgent("Agent for null origin");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createGenericComposeJson());
        app.setConfig(config);

        Assertions.assertThrows(DataValidationException.class,
                () -> agentApplicationService.save(tenantId, app));

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindEventsByAgentId() {
        Agent agent = createAgent("Agent for events by agentId");
        AgentApplication app1 = saveApplicationWithEdgeConfig(agent, "app1", "rk1");
        AgentApplication app2 = saveApplicationWithEdgeConfig(agent, "app2", "rk2");

        AgentAppEvent event1 = new AgentAppEvent();
        event1.setTenantId(tenantId);
        event1.setApplicationId(app1.getId());
        event1.setAgentId(agent.getId());
        event1.setApplicationName(app1.getName());
        event1.setActionType(AgentAppEventActionType.INSTALL);
        event1.setStartStatus(ProcessingStartStatus.PENDING);
        event1.setUpdatedTime(System.currentTimeMillis());
        agentAppEventService.save(tenantId, event1);

        AgentAppEvent event2 = new AgentAppEvent();
        event2.setTenantId(tenantId);
        event2.setApplicationId(app2.getId());
        event2.setAgentId(agent.getId());
        event2.setApplicationName(app2.getName());
        event2.setActionType(AgentAppEventActionType.UPDATE);
        event2.setStartStatus(ProcessingStartStatus.PENDING);
        event2.setUpdatedTime(System.currentTimeMillis());
        agentAppEventService.save(tenantId, event2);

        AgentAppEvent event3 = new AgentAppEvent();
        event3.setTenantId(tenantId);
        event3.setApplicationId(app1.getId());
        event3.setAgentId(agent.getId());
        event3.setApplicationName(app1.getName());
        event3.setActionType(AgentAppEventActionType.RESTART);
        event3.setStartStatus(ProcessingStartStatus.PENDING);
        event3.setUpdatedTime(System.currentTimeMillis());
        agentAppEventService.save(tenantId, event3);

        List<AgentAppEvent> events = agentAppEventService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(3, events.size());

        // Verify events from both apps are included
        long app1Events = events.stream().filter(e -> e.getApplicationId().equals(app1.getId())).count();
        long app2Events = events.stream().filter(e -> e.getApplicationId().equals(app2.getId())).count();
        Assert.assertEquals(2, app1Events);
        Assert.assertEquals(1, app2Events);

        agentApplicationService.delete(tenantId, app1.getId());
        agentApplicationService.delete(tenantId, app2.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_accessToken() {
        Agent agent = createAgent("Agent for GW access token");
        Device device = createDevice("GW Device 1");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String accessToken = creds.getCredentialsId();

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp1",
                createGatewayAccessTokenCompose(accessToken));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_mqttBasicUserNameOnly() {
        Agent agent = createAgent("Agent for GW MQTT userName");
        Device device = createDevice("GW Device 2");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        creds.setCredentialsId("gwUser1");
        creds.setCredentialsValue("{\"userName\":\"gwUser1\",\"password\":\"pass\"}");
        deviceCredentialsService.updateDeviceCredentials(tenantId, creds);

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp2",
                createGatewayMqttBasicCompose(null, "gwUser1"));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_mqttBasicClientIdOnly() {
        Agent agent = createAgent("Agent for GW MQTT clientId");
        Device device = createDevice("GW Device 3");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String clientId = "myGwClient";
        creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        creds.setCredentialsId(EncryptionUtil.getSha3Hash(clientId));
        creds.setCredentialsValue("{\"clientId\":\"" + clientId + "\"}");
        deviceCredentialsService.updateDeviceCredentials(tenantId, creds);

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp3",
                createGatewayMqttBasicCompose(clientId, null));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveGatewayAppResolvesRelatedEntity_mqttBasicBoth() {
        Agent agent = createAgent("Agent for GW MQTT both");
        Device device = createDevice("GW Device 4");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String clientId = "myGwClient2";
        String userName = "myGwUser2";
        creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        creds.setCredentialsId(EncryptionUtil.getSha3Hash("|", clientId, userName));
        creds.setCredentialsValue("{\"clientId\":\"" + clientId + "\",\"userName\":\"" + userName + "\",\"password\":\"pass\"}");
        deviceCredentialsService.updateDeviceCredentials(tenantId, creds);

        AgentApplication app = saveGatewayApplicationWithCompose(agent, "gwApp4",
                createGatewayMqttBasicCompose(clientId, userName));

        AgentApplication found = agentApplicationService.findByRelatedEntity(tenantId, device.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(app.getId(), found.getId());

        agentApplicationService.delete(tenantId, app.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindEventsByAgentIdEmpty() {
        Agent agent = createAgent("Agent with no events");

        List<AgentAppEvent> events = agentAppEventService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertTrue(events.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    // ==================== Profile config resolution on save ====================

    @Test
    public void testSave_newApp_withProfile_resolvesProfileConfig() {
        Agent agent = createAgent("Agent profile new");
        Edge edge = createEdge("Profile Edge", "profile-rk-new");
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createProfile("Profile 1", template, createEdgeProfileComposeJson("profile-rk-new"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());

        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, edge.getId());
        Assert.assertNotNull(saved.getConfig());
        Assert.assertEquals(profile.getVersion(), saved.getProfileConfigVersion());

        agentApplicationService.delete(tenantId, saved.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSave_newApp_withProfile_noMatchingEntity_savesWithoutRelation() {
        // No edge exists for the profile's placeholder routing key and the incoming app
        // carries no creds of its own, so relation resolution yields null — but the app
        // must still save (no explicit relatedEntityId is required anymore).
        Agent agent = createAgent("Agent profile no matching entity");
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createProfile("Profile no match", template, createEdgeProfileComposeJson("no-edge-for-this-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertNotNull(saved);
        AgentApplicationInfo info = agentApplicationService.findInfoById(tenantId, saved.getId());
        Assert.assertNull(info.getRelatedEntityId());
        Assert.assertEquals(profile.getVersion(), saved.getProfileConfigVersion());

        agentApplicationService.delete(tenantId, saved.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSave_newApp_withProfile_edge_preservesIncomingCredsAndResolvesEntity() {
        // Profile carries placeholder creds; incoming compose carries the real
        // routing key. ProfileConfigResolver must preserve the incoming CLOUD_ROUTING_KEY
        // across the profile-config overwrite so that resolveRelatedEntityFromConfig
        // can look up the edge from it.
        Agent agent = createAgent("Agent preserve edge creds");
        Edge edge = createEdge("Preserve Creds Edge", "real-edge-rk");
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createProfile("Profile edge placeholder",
                template, createEdgeProfileComposeJson("PROFILE_PLACEHOLDER"));

        DockerComposeConfig incomingConfig = new DockerComposeConfig();
        incomingConfig.setCompose(createEdgeProfileComposeJson("real-edge-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());
        app.setConfig(incomingConfig);

        EntityId resolved = agentAppRelationService.findRelatedEntityByConfig(tenantId, app);
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, resolved);
        Assert.assertNotNull(saved.getConfig());
        AgentApplicationInfo info = agentApplicationService.findInfoById(tenantId, saved.getId());
        Assert.assertEquals(edge.getId(), info.getRelatedEntityId());
        Assert.assertEquals("real-edge-rk", saved.getConfig().getEdgeRoutingKey());
        Assert.assertEquals(profile.getVersion(), saved.getProfileConfigVersion());

        agentApplicationService.delete(tenantId, saved.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSave_newApp_withProfile_gateway_preservesAccessTokenAndResolvesDevice() {
        Agent agent = createAgent("Agent preserve gateway access token");
        Device device = createDevice("Preserve Token Device");
        DeviceCredentials deviceCreds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String accessToken = deviceCreds.getCredentialsId();

        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("Gateway profile placeholder",
                template, createGatewayAccessTokenCompose("PROFILE_PLACEHOLDER_TOKEN"));

        DockerComposeConfig incomingConfig = new DockerComposeConfig();
        incomingConfig.setCompose(createGatewayAccessTokenCompose(accessToken));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GATEWAY);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());
        app.setConfig(incomingConfig);

        EntityId resolved = agentAppRelationService.findRelatedEntityByConfig(tenantId, app);
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, resolved);
        Assert.assertNotNull(saved.getConfig());
        AgentApplicationInfo info = agentApplicationService.findInfoById(tenantId, saved.getId());
        Assert.assertEquals(device.getId(), info.getRelatedEntityId());
        String preservedToken = DockerComposeUtils.getEnvVariable(
                ((DockerComposeConfig) saved.getConfig()).getCompose(),
                AgentApplicationType.GATEWAY.getMainImagePattern(), "TB_GW_ACCESS_TOKEN");
        Assert.assertEquals(accessToken, preservedToken);

        agentApplicationService.delete(tenantId, saved.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSave_newApp_withProfile_gateway_preservesMqttBasicCredsAndResolvesDevice() {
        Agent agent = createAgent("Agent preserve gateway mqtt creds");
        Device device = createDevice("Preserve Mqtt Device");
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        creds.setCredentialsId("realGwUser");
        creds.setCredentialsValue("{\"userName\":\"realGwUser\",\"password\":\"pass\"}");
        deviceCredentialsService.updateDeviceCredentials(tenantId, creds);

        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("Gateway profile mqtt placeholder",
                template, createGatewayMqttBasicFullCompose("placeholderClient", "placeholderUser", "placeholderPass"));

        DockerComposeConfig incomingConfig = new DockerComposeConfig();
        // Clear TB_GW_CLIENT_ID explicitly so the cred carry-over overwrites the
        // profile's placeholder and relation resolution uses the username path.
        incomingConfig.setCompose(createGatewayMqttBasicFullCompose("", "realGwUser", "realPass"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GATEWAY);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());
        app.setConfig(incomingConfig);

        EntityId resolved = agentAppRelationService.findRelatedEntityByConfig(tenantId, app);
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, resolved);
        AgentApplicationInfo info = agentApplicationService.findInfoById(tenantId, saved.getId());
        Assert.assertEquals(device.getId(), info.getRelatedEntityId());
        JsonNode savedCompose = ((DockerComposeConfig) saved.getConfig()).getCompose();
        Assert.assertEquals("usernamePassword",
                DockerComposeUtils.getEnvVariable(savedCompose,
                        AgentApplicationType.GATEWAY.getMainImagePattern(), "TB_GW_SECURITY_TYPE"));
        Assert.assertEquals("realGwUser",
                DockerComposeUtils.getEnvVariable(savedCompose,
                        AgentApplicationType.GATEWAY.getMainImagePattern(), "TB_GW_USERNAME"));

        agentApplicationService.delete(tenantId, saved.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        deviceService.deleteDevice(tenantId, device.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_profileChanged_resolvesNewProfileConfig() {
        Agent agent = createAgent("Agent profile change");
        Edge edge = createEdge("Profile Change Edge", "profile-change-rk");
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile1 = createProfile("Profile Old", template, createEdgeProfileComposeJson("profile-change-rk"));
        AgentAppProfile profile2 = createProfile("Profile New", template, createEdgeProfileComposeJson("profile-change-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile1.getId());
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, edge.getId());

        saved.setApplicationProfileId(profile2.getId());
        AgentApplication updated = agentApplicationService.save(tenantId, saved);
        Assert.assertNotNull(updated.getProfileConfigVersion());

        agentApplicationService.delete(tenantId, updated.getId());
        agentAppProfileService.deleteProfile(tenantId, profile1.getId());
        agentAppProfileService.deleteProfile(tenantId, profile2.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_profileUnchanged_rejectsNonCredConfigChange() {
        // A config change on a non-credential field (image tag here) on a
        // profile-managed app must still be rejected — the profile is the
        // source of truth for everything except creds.
        Agent agent = createAgent("Agent profile unchanged");
        Edge edge = createEdge("Unchanged Edge", "unchanged-rk");
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createProfile("Profile Unchanged", template, createEdgeProfileComposeJson("unchanged-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, edge.getId());

        ObjectNode tweaked = ((DockerComposeConfig) saved.getConfig()).getCompose().deepCopy();
        ((ObjectNode) tweaked.get("services").get("mytbedge")).put("image", "thingsboard/tb-edge-pe:9.9.9");
        DockerComposeConfig differentConfig = new DockerComposeConfig();
        differentConfig.setCompose(tweaked);
        saved.setConfig(differentConfig);

        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.save(tenantId, saved));

        agentApplicationService.delete(tenantId, saved.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_profileRefetched_acceptsNewConfigFromProfile() {
        // Simulate an UPDATE action where the profile has drifted since the
        // app was last synced: the action handler has already replaced
        // application.config with the new profile compose and bumped
        // profileConfigVersion. save() must accept that (non-cred) whole-
        // compose replacement instead of throwing the "Direct config update
        // is not allowed" guard.
        Agent agent = createAgent("Agent profile refetch");
        Edge edge = createEdge("Refetch Edge", "refetch-rk");
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createProfile("Profile refetch", template, createEdgeProfileComposeJson("refetch-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, edge.getId());
        Long originalProfileVersion = saved.getProfileConfigVersion();

        // Profile drift: swap the image tag (non-cred change) and re-save the
        // profile so its version increments.
        ObjectNode driftedCompose = ((DockerComposeConfig) profile.getConfig()).getCompose().deepCopy();
        ((ObjectNode) driftedCompose.get("services").get("mytbedge")).put("image", "thingsboard/tb-edge-pe:9.9.9");
        ((DockerComposeConfig) profile.getConfig()).setCompose(driftedCompose);
        AgentAppProfile bumpedProfile = agentAppProfileService.saveProfile(profile);

        // Stand in for what UpdateActionHandler + ProfileConfigResolver.resolve
        // does: replace config with new profile compose and bump the version.
        DockerComposeConfig resolvedConfig = new DockerComposeConfig();
        resolvedConfig.setCompose(driftedCompose.deepCopy());
        saved.setConfig(resolvedConfig);
        saved.setProfileConfigVersion(bumpedProfile.getVersion());

        AgentApplication updated = agentApplicationService.save(tenantId, saved);
        Assert.assertEquals(bumpedProfile.getVersion(), updated.getProfileConfigVersion());
        Assert.assertNotEquals(originalProfileVersion, updated.getProfileConfigVersion());
        Assert.assertEquals("thingsboard/tb-edge-pe:9.9.9",
                ((DockerComposeConfig) updated.getConfig()).getCompose()
                        .get("services").get("mytbedge").get("image").asText());

        agentApplicationService.delete(tenantId, updated.getId());
        agentAppProfileService.deleteProfile(tenantId, bumpedProfile.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_profileChanged_differentTemplateVersion_flipsAppVersion() {
        Agent agent = createAgent("Agent version flip");
        Edge edge = createEdge("Version Flip Edge", "version-flip-rk");
        AgentAppTemplate template1 = createTemplate("1.0.0", "2.0.0");
        AgentAppTemplate template2 = createTemplate("2.0.0", null);
        registerTemplates(template1, template2);
        AgentAppProfile profile1 = createProfile("Profile v1", template1, createEdgeProfileComposeJson("version-flip-rk"));
        AgentAppProfile profile2 = createProfile("Profile v2", template2, createEdgeProfileComposeJson("version-flip-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template1.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile1.getId());
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, edge.getId());
        Assert.assertEquals("1.0.0", saved.getTemplateVersion());

        saved.setApplicationProfileId(profile2.getId());
        AgentApplication updated = agentApplicationService.save(tenantId, saved);
        Assert.assertEquals("2.0.0", updated.getTemplateVersion());
        Assert.assertNull(updated.getDesiredTemplateVersion());
        Assert.assertEquals(profile2.getVersion(), updated.getProfileConfigVersion());

        agentApplicationService.delete(tenantId, updated.getId());
        agentAppProfileService.deleteProfile(tenantId, profile1.getId());
        agentAppProfileService.deleteProfile(tenantId, profile2.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_profileDrifted_configPushRejected_appVersionKept() {
        // The assigned profile moved to a newer template after assignment.
        // Pushing the drifted profile's compose into the app must be rejected —
        // the app may only carry config of its own template version; moving
        // versions is the UPGRADE flow's job. Cred-only updates stay allowed.
        Agent agent = createAgent("Agent drifted profile");
        Edge edge = createEdge("Drifted Edge", "drifted-rk");
        AgentAppTemplate template1 = createTemplate("1.0.0", "2.0.0");
        AgentAppTemplate template2 = createTemplate("2.0.0", null);
        registerTemplates(template1, template2);
        AgentAppProfile profile = createProfile("Profile drifted", template1, createEdgeProfileComposeJson("drifted-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template1.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, edge.getId());

        ObjectNode driftedCompose = ((DockerComposeConfig) profile.getConfig()).getCompose().deepCopy();
        ((ObjectNode) driftedCompose.get("services").get("mytbedge")).put("image", "thingsboard/tb-edge-pe:9.9.9");
        ((DockerComposeConfig) profile.getConfig()).setCompose(driftedCompose);
        profile.setTemplateVersion(template2.getCurrentVersion());
        agentAppProfileService.saveProfile(profile);

        DockerComposeConfig resolvedConfig = new DockerComposeConfig();
        resolvedConfig.setCompose(driftedCompose.deepCopy());
        saved.setConfig(resolvedConfig);

        Assertions.assertThrows(DataValidationException.class, () ->
                agentApplicationService.save(tenantId, saved));

        AgentApplicationInfo info = agentApplicationService.findInfoById(tenantId, saved.getId());
        Assert.assertEquals("1.0.0", info.getTemplateVersion());
        Assert.assertEquals("2.0.0", info.getProfileTemplateVersion());
        Assert.assertEquals("2.0.0", info.getNextVersion());

        agentApplicationService.delete(tenantId, saved.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdate_credEdit_isOverwrittenByRelatedEntityCreds() {
        // When a related entity is assigned, the entity is the source of truth
        // for credentials. A user-typed cred edit gets reverted by save's
        // re-merge from the relation. Re-pointing the related entity must go
        // through explicit assignRelatedEntity.
        Agent agent = createAgent("Agent profile cred rotation");
        Edge edge = createEdge("Cred Rotation Edge", "old-rk");
        Edge newEdge = createEdge("Cred Rotation Edge new", "new-rk");
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createProfile("Profile Cred Rotation", template, createEdgeProfileComposeJson("old-rk"));

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(profile.getId());
        AgentApplication saved = agentApplicationService.saveWithRelatedEntity(tenantId, app, edge.getId());

        ObjectNode rotated = ((DockerComposeConfig) saved.getConfig()).getCompose().deepCopy();
        ((ObjectNode) rotated.get("services").get("mytbedge").get("environment"))
                .put("CLOUD_ROUTING_KEY", "new-rk");
        DockerComposeConfig newConfig = new DockerComposeConfig();
        newConfig.setCompose(rotated);
        saved.setConfig(newConfig);

        AgentApplication updated = agentApplicationService.save(tenantId, saved);
        Assert.assertEquals("old-rk", updated.getConfig().getEdgeRoutingKey());

        AgentApplication reassigned = agentApplicationService.assignRelatedEntity(tenantId, updated.getId(), newEdge.getId());
        AgentApplicationInfo info = agentApplicationService.findInfoById(tenantId, reassigned.getId());
        Assert.assertEquals(newEdge.getId(), info.getRelatedEntityId());
        Assert.assertEquals("new-rk", reassigned.getConfig().getEdgeRoutingKey());

        agentApplicationService.delete(tenantId, updated.getId());
        agentAppProfileService.deleteProfile(tenantId, profile.getId());
        edgeService.deleteEdge(tenantId, edge.getId());
        edgeService.deleteEdge(tenantId, newEdge.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSave_withoutProfile_skipsProfileResolution() {
        Agent agent = createAgent("Agent no profile");
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createGenericComposeJson());
        app.setConfig(config);

        AgentApplication saved = agentApplicationService.save(tenantId, app);
        Assert.assertNull(saved.getApplicationProfileId());
        Assert.assertNull(saved.getProfileConfigVersion());

        agentApplicationService.delete(tenantId, saved.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    private AgentAppProfile createProfile(String name, AgentAppTemplate template, JsonNode compose) {
        return createProfile(name, AgentApplicationType.EDGE, template, compose);
    }

    private AgentAppProfile createGatewayProfile(String name, AgentAppTemplate template, JsonNode compose) {
        return createProfile(name, AgentApplicationType.GATEWAY, template, compose);
    }

    private AgentAppProfile createProfile(String name, AgentApplicationType appType, AgentAppTemplate template, JsonNode compose) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(appType);
        profile.setTemplateVersion(template.getCurrentVersion());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        profile.setConfig(config);
        return agentAppProfileService.saveProfile(profile);
    }

    private JsonNode createEdgeProfileComposeJson(String routingKey) {
        ObjectNode env = JacksonUtil.newObjectNode();
        if (routingKey != null) {
            env.put("CLOUD_ROUTING_KEY", routingKey);
        }
        env.put("CLOUD_ROUTING_SECRET", "secret");
        env.put("CLOUD_RPC_HOST", "localhost");
        env.put("CLOUD_RPC_PORT", "7070");
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private Agent createAgent(String name) {
        return createAgent(tenantId, name);
    }

    private AgentApplication saveApplication(Agent agent, String name) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setName(name);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        return agentApplicationService.save(tenantId, app);
    }

    @Test
    public void testPromoteDesiredTemplate_movesVersionClearsDesiredAndBumpsOptimisticLockVersion() {
        Agent agent = createAgent("Agent promote");
        AgentAppTemplate template1 = createTemplate("1.0.0", "2.0.0");
        AgentAppTemplate template2 = createTemplate("2.0.0", null);
        registerTemplates(template1, template2);

        AgentApplication saved = saveApplicationWithEdgeConfig(agent, "promoteApp", "promote-rk", "1.0.0");
        saved.setDesiredTemplateVersion("2.0.0");
        AgentApplication withDesired = agentApplicationService.save(tenantId, saved);
        Assert.assertEquals("2.0.0", withDesired.getDesiredTemplateVersion());
        Long versionBefore = withDesired.getVersion();

        agentApplicationService.promoteDesiredTemplate(tenantId, withDesired.getId());

        AgentApplication promoted = agentApplicationService.findById(tenantId, withDesired.getId());
        Assert.assertEquals("2.0.0", promoted.getTemplateVersion());
        Assert.assertNull(promoted.getDesiredTemplateVersion());
        Assert.assertEquals(Long.valueOf(versionBefore + 1), promoted.getVersion());

        agentApplicationService.delete(tenantId, promoted.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testPromoteDesiredTemplate_noDesiredVersion_isNoOp() {
        Agent agent = createAgent("Agent promote noop");
        AgentAppTemplate template = createTemplate();

        AgentApplication saved = saveApplicationWithEdgeConfig(agent, "noopApp", "noop-rk", template.getCurrentVersion());
        Long versionBefore = saved.getVersion();

        agentApplicationService.promoteDesiredTemplate(tenantId, saved.getId());

        AgentApplication after = agentApplicationService.findById(tenantId, saved.getId());
        Assert.assertEquals(template.getCurrentVersion(), after.getTemplateVersion());
        Assert.assertNull(after.getDesiredTemplateVersion());
        Assert.assertEquals(versionBefore, after.getVersion());

        agentApplicationService.delete(tenantId, after.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testPromoteDesiredTemplate_unknownApplication_isNoOp() {
        Assertions.assertDoesNotThrow(() -> agentApplicationService.promoteDesiredTemplate(
                tenantId, new AgentApplicationId(UUID.randomUUID())));
    }

    private AgentAppTemplate createTemplate() {
        AgentAppTemplate template = createTemplate(AgentApplicationType.GENERIC.getDefaultVersion(), null);
        registerTemplates(template);
        return template;
    }

    private AgentAppTemplate createTemplate(String currentVersion, String nextVersion) {
        return createAppTemplate(AgentApplicationType.GENERIC, currentVersion, nextVersion);
    }

    private void registerTemplates(AgentAppTemplate... templates) {
        for (AgentApplicationType appType : AgentApplicationType.values()) {
            for (AgentAppTemplate template : templates) {
                registerAppTemplate(appType, template);
            }
        }
    }

    private Edge createEdge(String name, String routingKey) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName(name);
        edge.setType("default");
        edge.setRoutingKey(routingKey);
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setEdgeLicenseKey("test-license-key");
        edge.setCloudEndpoint("http://localhost:8080");
        return edgeService.saveEdge(edge);
    }

    private AgentApplication saveApplicationWithEdgeConfig(Agent agent, String name, String routingKey) {
        AgentAppTemplate template = createTemplate();
        return saveApplicationWithEdgeConfig(agent, name, routingKey, template.getCurrentVersion());
    }

    private AgentApplication saveApplicationWithEdgeConfig(Agent agent, String name, String routingKey, String templateVersion) {
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.EDGE);
        app.setName(name);
        app.setTemplateVersion(templateVersion);
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeComposeJson(routingKey));
        app.setConfig(config);

        EntityId resolved = agentAppRelationService.findRelatedEntityByConfig(tenantId, app);
        return agentApplicationService.saveWithRelatedEntity(tenantId, app, resolved);
    }

    private Device createDevice(String name) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(name);
        device.setType("default");
        return deviceService.saveDevice(device);
    }

    private AgentApplication saveGatewayApplicationWithCompose(Agent agent, String name, JsonNode compose) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GATEWAY);
        app.setName(name);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        app.setConfig(config);

        EntityId resolved = agentAppRelationService.findRelatedEntityByConfig(tenantId, app);
        return agentApplicationService.saveWithRelatedEntity(tenantId, app, resolved);
    }

    private JsonNode createGatewayAccessTokenCompose(String accessToken) {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("TB_GW_SECURITY_TYPE", "accessToken");
        env.put("TB_GW_ACCESS_TOKEN", accessToken);
        return createGatewayComposeWithEnv(env);
    }

    private JsonNode createGatewayMqttBasicCompose(String clientId, String userName) {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("TB_GW_SECURITY_TYPE", "usernamePassword");
        if (clientId != null) {
            env.put("TB_GW_CLIENT_ID", clientId);
        }
        if (userName != null) {
            env.put("TB_GW_USERNAME", userName);
        }
        return createGatewayComposeWithEnv(env);
    }

    private JsonNode createGatewayMqttBasicFullCompose(String clientId, String userName, String password) {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("TB_GW_SECURITY_TYPE", "usernamePassword");
        env.put("TB_GW_CLIENT_ID", clientId);
        env.put("TB_GW_USERNAME", userName);
        env.put("TB_GW_PASSWORD", password);
        return createGatewayComposeWithEnv(env);
    }

    private JsonNode createGatewayComposeWithEnv(ObjectNode env) {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.7");
        service.set("environment", env);
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-gateway", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createEdgeComposeJson(String routingKey) {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        if (routingKey != null) {
            ObjectNode env = JacksonUtil.newObjectNode();
            env.put("CLOUD_ROUTING_KEY", routingKey);
            service.set("environment", env);
        }
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode createGenericComposeJson() {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");

        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();

        compose.set("services", services);
        return compose;
    }
}
