// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;

@DaoSqlTest
public class AgentAppUnitServiceTest extends AbstractServiceTest {

    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppUnitService agentAppUnitService;
    @MockitoSpyBean
    LogChunkBuffer logChunkBuffer;

    @Test
    public void testSaveAgentAppUnit() {
        Agent agent = createAgent("My agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("unit-1");
        unit.setType(AgentAppUnitType.CONTAINER);

        AgentAppUnit saved = agentAppUnitService.saveAgentAppUnit(tenantId, unit);
        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(app.getId(), saved.getAgentApplicationId());
        Assert.assertEquals("unit-1", saved.getIdentifier());
        Assert.assertEquals(AgentAppUnitType.CONTAINER, saved.getType());

        AgentAppUnit found = agentAppUnitService.findAgentAppUnitById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());

        List<AgentAppUnit> byApp = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(1, byApp.size());
        Assert.assertEquals(saved.getId(), byApp.get(0).getId());

        agentAppUnitService.deleteAgentAppUnit(tenantId, saved.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentAppUnitWithNullAgentApplicationId() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setIdentifier("id");
        unit.setType(AgentAppUnitType.CONTAINER);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
    }

    @Test
    public void testSaveAgentAppUnitWithNonExistentAgentApplication() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(new AgentApplicationId(java.util.UUID.randomUUID()));
        unit.setIdentifier("id");
        unit.setType(AgentAppUnitType.CONTAINER);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
    }

    @Test
    public void testSaveAgentAppUnitWithBlankIdentifier() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("  ");
        unit.setType(AgentAppUnitType.CONTAINER);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentAppUnitWithNullType() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier("id");
        unit.setType(null);
        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppUnitService.saveAgentAppUnit(tenantId, unit));
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindAgentAppUnitsByAgentAppId() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit u1 = saveUnit(app, "id1", AgentAppUnitType.CONTAINER);
        AgentAppUnit u2 = saveUnit(app, "id2", AgentAppUnitType.VOLUME);

        List<AgentAppUnit> list = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(2, list.size());

        agentAppUnitService.deleteAgentAppUnit(tenantId, u1.getId());
        agentAppUnitService.deleteAgentAppUnit(tenantId, u2.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteAgentAppUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "toDelete", AgentAppUnitType.CONTAINER);

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        AgentAppUnit found = agentAppUnitService.findAgentAppUnitById(tenantId, unit.getId());
        Assert.assertNull(found);

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteByAgentApplicationId() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        saveUnit(app, "u1", AgentAppUnitType.CONTAINER);
        saveUnit(app, "u2", AgentAppUnitType.VOLUME);

        List<AgentAppUnit> before = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(2, before.size());

        agentAppUnitService.deleteByAgentApplicationId(tenantId, app.getId());
        List<AgentAppUnit> after = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertTrue(after.isEmpty());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteAgentApplicationRemovesAgentAppUnits() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        saveUnit(app, "c1", AgentAppUnitType.CONTAINER);
        saveUnit(app, "c2", AgentAppUnitType.NETWORK);

        List<AgentAppUnit> before = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertEquals(2, before.size());

        agentApplicationService.delete(tenantId, app.getId());
        List<AgentAppUnit> after = agentAppUnitService.findAgentAppUnitsByAgentAppId(tenantId, app.getId());
        Assert.assertTrue(after.isEmpty());

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteAgentAppUnitCallsLogChunkBufferDeleteUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "withLogs", AgentAppUnitType.CONTAINER);
        org.mockito.Mockito.reset(logChunkBuffer);

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());

        org.mockito.Mockito.verify(logChunkBuffer).deleteUnit(tenantId, unit.getId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testDeleteByAgentApplicationIdCallsLogChunkBufferDeleteUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit u1 = saveUnit(app, "u1", AgentAppUnitType.CONTAINER);
        AgentAppUnit u2 = saveUnit(app, "u2", AgentAppUnitType.CONTAINER);
        org.mockito.Mockito.reset(logChunkBuffer);

        agentAppUnitService.deleteByAgentApplicationId(tenantId, app.getId());

        org.mockito.Mockito.verify(logChunkBuffer).deleteUnit(tenantId, u1.getId());
        org.mockito.Mockito.verify(logChunkBuffer).deleteUnit(tenantId, u2.getId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testSaveAgentAppUnitDoesNotCallLogChunkBufferDeleteUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        org.mockito.Mockito.reset(logChunkBuffer);

        AgentAppUnit unit = saveUnit(app, "fresh", AgentAppUnitType.CONTAINER);
        unit.setIdentifier("fresh-updated");
        agentAppUnitService.saveAgentAppUnit(tenantId, unit);

        org.mockito.Mockito.verify(logChunkBuffer, org.mockito.Mockito.never())
                .deleteUnit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testUpdateAgentAppUnit() {
        Agent agent = createAgent("Agent");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "id1", AgentAppUnitType.CONTAINER);

        unit.setIdentifier("id1-updated");
        unit.setType(AgentAppUnitType.VOLUME);
        AgentAppUnit updated = agentAppUnitService.saveAgentAppUnit(tenantId, unit);
        Assert.assertEquals("id1-updated", updated.getIdentifier());
        Assert.assertEquals(AgentAppUnitType.VOLUME, updated.getType());

        AgentAppUnit found = agentAppUnitService.findAgentAppUnitById(tenantId, unit.getId());
        Assert.assertEquals("id1-updated", found.getIdentifier());

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByAgentAndProjectAndIdentifier() {
        Agent agent = createAgent("Agent lookup");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "svc-1", AgentAppUnitType.CONTAINER);

        AgentAppUnit found = agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), app.getProjectName(), "svc-1", AgentAppUnitType.CONTAINER);
        Assert.assertNotNull(found);
        Assert.assertEquals(unit.getId(), found.getId());

        // the composite key includes the type, the identifier and the project
        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), app.getProjectName(), "svc-1", AgentAppUnitType.VOLUME));
        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), app.getProjectName(), "svc-2", AgentAppUnitType.CONTAINER));
        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), "other-project", "svc-1", AgentAppUnitType.CONTAINER));

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByAgentAndProjectAndIdentifierShortCircuitsOnBlankArguments() {
        Agent agent = createAgent("Agent short circuit");

        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), null, "svc-1", AgentAppUnitType.CONTAINER));
        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), "", "svc-1", AgentAppUnitType.CONTAINER));
        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), "project", null, AgentAppUnitType.CONTAINER));
        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), "project", "", AgentAppUnitType.CONTAINER));
        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), "project", "svc-1", null));

        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByAgentAndProjectAndIdentifierEvictsRenamedIdentifier() {
        Agent agent = createAgent("Agent rename");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "svc-old", AgentAppUnitType.CONTAINER);

        // populate the composite cache key
        Assert.assertNotNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), app.getProjectName(), "svc-old", AgentAppUnitType.CONTAINER));

        unit.setIdentifier("svc-new");
        agentAppUnitService.saveAgentAppUnit(tenantId, unit);

        Assert.assertNull(agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), app.getProjectName(), "svc-old", AgentAppUnitType.CONTAINER));
        AgentAppUnit renamed = agentAppUnitService.findByAgentAndProjectAndIdentifier(
                tenantId, agent.getId(), app.getProjectName(), "svc-new", AgentAppUnitType.CONTAINER);
        Assert.assertNotNull(renamed);
        Assert.assertEquals(unit.getId(), renamed.getId());

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindAgentAppUnitInfoById() {
        Agent agent = createAgent("Agent info");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit unit = saveUnit(app, "svc-info", AgentAppUnitType.CONTAINER);

        AgentAppUnitInfo info = agentAppUnitService.findAgentAppUnitInfoById(tenantId, unit.getId());
        Assert.assertNotNull(info);
        Assert.assertEquals(unit.getId(), info.getId());
        Assert.assertEquals("svc-info", info.getIdentifier());
        Assert.assertEquals(agent.getId(), info.getAgentId());
        Assert.assertEquals(app.getProjectName(), info.getProjectName());

        agentAppUnitService.deleteAgentAppUnit(tenantId, unit.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    @Test
    public void testFindByFilter() {
        Agent agent = createAgent("Agent filter");
        AgentApplication app = saveApplication(agent);
        AgentAppUnit container = saveUnit(app, "svc-c", AgentAppUnitType.CONTAINER);
        AgentAppUnit volume = saveUnit(app, "svc-v", AgentAppUnitType.VOLUME);

        PageData<AgentAppUnit> all = agentAppUnitService.findByFilter(AgentAppUnitFilter.builder()
                .tenantId(tenantId).applicationId(app.getId()).build(), new PageLink(100));
        Assert.assertEquals(2, all.getTotalElements());

        PageData<AgentAppUnit> containersOnly = agentAppUnitService.findByFilter(AgentAppUnitFilter.builder()
                .tenantId(tenantId).applicationId(app.getId()).type(AgentAppUnitType.CONTAINER).build(),
                new PageLink(100));
        Assert.assertEquals(1, containersOnly.getTotalElements());
        Assert.assertEquals(container.getId(), containersOnly.getData().get(0).getId());

        agentAppUnitService.deleteAgentAppUnit(tenantId, container.getId());
        agentAppUnitService.deleteAgentAppUnit(tenantId, volume.getId());
        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
    }

    private Agent createAgent(String name) {
        return createAgent(tenantId, name);
    }

    private AgentApplication saveApplication(Agent agent) {
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
        return agentApplicationService.save(tenantId, app);
    }

    private com.fasterxml.jackson.databind.JsonNode createGenericComposeJson() {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private AgentAppTemplate createTemplate() {
        return registerAppTemplate(createAppTemplate(
                AgentApplicationType.GENERIC, AgentApplicationType.GENERIC.getDefaultVersion()));
    }

    private AgentAppUnit saveUnit(AgentApplication app, String identifier, AgentAppUnitType type) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(app.getId());
        unit.setIdentifier(identifier);
        unit.setType(type);
        return agentAppUnitService.saveAgentAppUnit(tenantId, unit);
    }
}
