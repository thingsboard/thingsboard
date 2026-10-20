// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.AgentTemplateTestSupport;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionEventStats;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentBulkActionControllerTest extends AbstractControllerTest {

    @Autowired
    AgentBulkActionService agentBulkActionService;
    @Autowired
    AgentAppEventService agentAppEventService;
    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AppTemplateRegistry appTemplateRegistry;

    private AgentBulkAction bulkAction;
    private AgentApplication listingApplication;

    @Before
    public void setUpBulkAction() throws Exception {
        loginTenantAdmin();
        AgentProfile agentProfile = doPost("/api/agent/profile", named(new AgentProfile(), "Bulk Ctl Profile"), AgentProfile.class);
        AgentAppProfile appProfile = createAppProfile("Bulk Ctl App Profile");

        AgentBulkAction action = new AgentBulkAction();
        action.setTenantId(tenantId);
        action.setAgentProfileId(agentProfile.getId().getId());
        action.setApplicationProfileId(appProfile.getId().getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(AgentBulkActionStatus.STARTED);
        bulkAction = agentBulkActionService.save(tenantId, action);
        listingApplication = createListingApplication();
    }

    @Test
    public void testGetAgentBulkAction() throws Exception {
        AgentBulkAction found = doGet("/api/agent/bulk/" + bulkAction.getId().getId(), AgentBulkAction.class);
        Assert.assertEquals(bulkAction.getId(), found.getId());
        Assert.assertEquals(AgentAppEventActionType.UPDATE, found.getActionType());
    }

    @Test
    public void testGetAgentBulkActionEvents_paging() throws Exception {
        PageData<AgentAppEventInfo> events = doGetTypedWithPageLink(
                "/api/agent/bulk/" + bulkAction.getId().getId() + "/events?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(0, events.getTotalElements());
    }

    @Test
    public void testGetAgentBulkActionEvents_returnsOnlyEventsOfThisBulkAction() throws Exception {
        AgentAppEvent mine = saveEvent(bulkAction.getId().getId(), AgentAppEventActionType.UPDATE, AgentProcessingStatus.PENDING);
        AgentBulkAction otherBulkAction = saveBulkAction();
        saveEvent(otherBulkAction.getId().getId(), AgentAppEventActionType.UPDATE, AgentProcessingStatus.PENDING);

        PageData<AgentAppEventInfo> events = doGetTypedWithPageLink(
                "/api/agent/bulk/" + bulkAction.getId().getId() + "/events?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(1, events.getTotalElements());
        Assert.assertEquals(mine.getId(), events.getData().get(0).getId());
    }

    @Test
    public void testGetAgentBulkActionEvents_filtersByActionTypeAndProcessingStatus() throws Exception {
        AgentAppEvent pendingUpdate = saveEvent(bulkAction.getId().getId(), AgentAppEventActionType.UPDATE, AgentProcessingStatus.PENDING);
        saveEvent(bulkAction.getId().getId(), AgentAppEventActionType.INSTALL, AgentProcessingStatus.PENDING);
        saveEvent(bulkAction.getId().getId(), AgentAppEventActionType.UPDATE, AgentProcessingStatus.ERROR);

        String base = "/api/agent/bulk/" + bulkAction.getId().getId() + "/events?";

        PageData<AgentAppEventInfo> byActionType = doGetTypedWithPageLink(base + "actionType=INSTALL&",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(1, byActionType.getTotalElements());
        Assert.assertEquals(AgentAppEventActionType.INSTALL, byActionType.getData().get(0).getActionType());

        PageData<AgentAppEventInfo> byStatus = doGetTypedWithPageLink(base + "processingStatus=ERROR&",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(1, byStatus.getTotalElements());
        Assert.assertEquals(AgentProcessingStatus.ERROR, byStatus.getData().get(0).getProcessingStatus());

        PageData<AgentAppEventInfo> byBoth = doGetTypedWithPageLink(
                base + "actionType=UPDATE&processingStatus=PENDING&", new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(1, byBoth.getTotalElements());
        Assert.assertEquals(pendingUpdate.getId(), byBoth.getData().get(0).getId());
    }

    @Test
    public void testGetAgentBulkActionEventStats_emptyBulkAction() throws Exception {
        AgentBulkActionEventStats stats = doGet("/api/agent/bulk/" + bulkAction.getId().getId() + "/eventStats",
                AgentBulkActionEventStats.class);
        Assert.assertEquals(0, stats.getTotal());
        Assert.assertTrue(stats.getCountsByStatus().isEmpty());
    }

    @Test
    public void testGetAgentBulkActionEventStats_countsPerStatusOfThisBulkActionOnly() throws Exception {
        UUID mine = bulkAction.getId().getId();
        saveEvent(mine, AgentAppEventActionType.UPDATE, AgentProcessingStatus.FINISHED);
        saveEvent(mine, AgentAppEventActionType.UPDATE, AgentProcessingStatus.FINISHED);
        saveEvent(mine, AgentAppEventActionType.UPDATE, AgentProcessingStatus.ERROR);
        saveEvent(mine, AgentAppEventActionType.INSTALL, AgentProcessingStatus.START_FAILED);
        saveEvent(mine, AgentAppEventActionType.UPDATE, AgentProcessingStatus.PROCESSING);
        saveEvent(mine, AgentAppEventActionType.UPDATE, AgentProcessingStatus.QUEUED);
        saveEvent(mine, AgentAppEventActionType.UPDATE, AgentProcessingStatus.PENDING);
        saveEvent(saveBulkAction().getId().getId(), AgentAppEventActionType.UPDATE, AgentProcessingStatus.FINISHED);

        AgentBulkActionEventStats stats = doGet("/api/agent/bulk/" + mine + "/eventStats", AgentBulkActionEventStats.class);
        Assert.assertEquals(7, stats.getTotal());
        Assert.assertEquals(Map.of(
                AgentProcessingStatus.FINISHED, 2L,
                AgentProcessingStatus.ERROR, 1L,
                AgentProcessingStatus.START_FAILED, 1L,
                AgentProcessingStatus.PROCESSING, 1L,
                AgentProcessingStatus.QUEUED, 1L,
                AgentProcessingStatus.PENDING, 1L), stats.getCountsByStatus());
    }

    @Test
    public void testGetAgentBulkActionEventStats_crossTenantDenied() throws Exception {
        loginDifferentTenant();
        doGet("/api/agent/bulk/" + bulkAction.getId().getId() + "/eventStats").andExpect(status().isNotFound());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_customerForbidden() throws Exception {
        loginCustomerUser();
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_crossTenantDenied() throws Exception {
        loginDifferentTenant();
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isNotFound());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_allowedWithProfileReads() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(
                Resource.AGENT_PROFILE, List.of(Operation.READ),
                Resource.AGENT_APP_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isOk());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_deniedWithoutAgentProfileRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_APP_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_deniedWithoutAppProfileRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    private AgentAppEvent saveEvent(UUID bulkActionId, AgentAppEventActionType actionType, AgentProcessingStatus processingStatus) {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(listingApplication.getId());
        event.setAgentId(listingApplication.getAgentId());
        event.setApplicationName(listingApplication.getName());
        event.setActionType(actionType);
        event.setStartStatus(ProcessingStartStatus.DELIVERY_FAIL);
        event.setProcessingStatus(processingStatus);
        event.setUpdatedTime(System.currentTimeMillis());
        event.setBulkActionId(bulkActionId);
        return agentAppEventService.save(tenantId, event);
    }

    private AgentBulkAction saveBulkAction() {
        AgentBulkAction action = new AgentBulkAction();
        action.setTenantId(tenantId);
        action.setAgentProfileId(bulkAction.getAgentProfileId());
        action.setApplicationProfileId(bulkAction.getApplicationProfileId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(AgentBulkActionStatus.STARTED);
        return agentBulkActionService.save(tenantId, action);
    }

    private AgentApplication createListingApplication() throws Exception {
        Agent newAgent = new Agent();
        newAgent.setName("Bulk Listing Agent");
        newAgent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        newAgent.setSecret(StringUtils.randomAlphanumeric(20));
        Agent agent = doPost("/api/agent", newAgent, Agent.class);

        String genericVersion = AgentApplicationType.GENERIC.getDefaultVersion();
        AgentTemplateTestSupport.registerGenericTemplate(appTemplateRegistry, genericVersion);

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setName("Bulk Listing App");
        app.setTemplateVersion(genericVersion);
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        app.setConfig(config);
        return agentApplicationService.save(tenantId, app);
    }

    private AgentProfile named(AgentProfile profile, String name) {
        profile.setName(name);
        return profile;
    }

    private AgentAppProfile createAppProfile(String name) throws Exception {
        AgentAppTemplate template = AgentTemplateTestSupport.registerGenericTemplate(appTemplateRegistry);
        return doPost("/api/agent/app/profile", AgentTemplateTestSupport.appProfileFor(template, name), AgentAppProfile.class);
    }
}
