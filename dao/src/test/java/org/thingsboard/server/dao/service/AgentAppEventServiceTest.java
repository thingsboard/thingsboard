// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@DaoSqlTest
public class AgentAppEventServiceTest extends AbstractServiceTest {

    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppEventService agentAppEventService;
    @Autowired
    AgentBulkActionService agentBulkActionService;

    @Test
    public void testSaveAndFind_withStepStatesJsonRoundTrip() {
        Agent agent = createAgent("Agent events round trip");
        AgentApplication app = createApp(agent, "round-trip-app");

        UUID stepId = UUID.randomUUID();
        ComposeStepState composeStepState = new ComposeStepState();
        composeStepState.setPullImages(new StepField<>(true, true));
        Map<UUID, AgentAppStepState> stepStates = Map.of(stepId, composeStepState);

        AgentAppEvent event = newEvent(agent, app, AgentAppEventActionType.INSTALL,
                ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);
        event.setStepStates(stepStates);
        AgentAppEvent saved = agentAppEventService.save(tenantId, event, false);
        Assert.assertNotNull(saved.getId());

        AgentAppEvent found = agentAppEventService.findById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertNotNull(found.getStepStates());
        Assert.assertTrue(found.getStepStates().containsKey(stepId));
        Assert.assertTrue("step state should round-trip to its concrete type",
                found.getStepStates().get(stepId) instanceof ComposeStepState);

        cleanup(agent, app);
    }

    @Test
    public void testFindByFilter_filtersByApplicationAndStatus() {
        Agent agent = createAgent("Agent filter");
        AgentApplication app1 = createApp(agent, "filter-app-1");
        AgentApplication app2 = createApp(agent, "filter-app-2");

        saveEvent(agent, app1, AgentAppEventActionType.INSTALL, ProcessingStartStatus.DELIVERED, AgentProcessingStatus.FINISHED);
        saveEvent(agent, app1, AgentAppEventActionType.UPDATE, ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);
        saveEvent(agent, app2, AgentAppEventActionType.INSTALL, ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);

        // by application
        PageData<AgentAppEvent> byApp1 = agentAppEventService.findByFilter(
                AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app1.getId()).build(), new PageLink(100));
        Assert.assertEquals(2, byApp1.getTotalElements());
        Assert.assertTrue(byApp1.getData().stream().allMatch(e -> e.getApplicationId().equals(app1.getId())));

        // by application + status (applicationId is mandatory for findByFilter)
        PageData<AgentAppEvent> app1Finished = agentAppEventService.findByFilter(
                AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app1.getId())
                        .processingStatus(AgentProcessingStatus.FINISHED).build(), new PageLink(100));
        Assert.assertEquals(1, app1Finished.getTotalElements());
        Assert.assertEquals(AgentProcessingStatus.FINISHED, app1Finished.getData().get(0).getProcessingStatus());

        // by application + actionType
        PageData<AgentAppEvent> app1Update = agentAppEventService.findByFilter(
                AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app1.getId())
                        .actionType(AgentAppEventActionType.UPDATE).build(), new PageLink(100));
        Assert.assertEquals(1, app1Update.getTotalElements());

        cleanup(agent, app1, app2);
    }

    @Test
    public void testFindByFilter_sortsByEveryMappedProperty() {
        Agent agent = createAgent("Agent sort");
        AgentApplication app = createApp(agent, "sort-app");

        saveEvent(agent, app, AgentAppEventActionType.INSTALL, ProcessingStartStatus.DELIVERED, AgentProcessingStatus.FINISHED);
        saveEvent(agent, app, AgentAppEventActionType.UPDATE, ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);
        saveEvent(agent, app, AgentAppEventActionType.RESTART, ProcessingStartStatus.PENDING, AgentProcessingStatus.ERROR);

        AgentAppEventFilter filter = AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app.getId()).build();
        for (String sortProperty : List.of("createdTime", "updatedTime", "actionType", "startStatus", "processingStatus")) {
            for (SortOrder.Direction direction : SortOrder.Direction.values()) {
                PageLink pageLink = new PageLink(100, 0, null, new SortOrder(sortProperty, direction));
                PageData<AgentAppEvent> page = agentAppEventService.findByFilter(filter, pageLink);
                Assert.assertEquals("sort by " + sortProperty + " " + direction, 3, page.getTotalElements());
            }
        }

        PageData<AgentAppEvent> ascByActionType = agentAppEventService.findByFilter(filter,
                new PageLink(100, 0, null, new SortOrder("actionType", SortOrder.Direction.ASC)));
        Assert.assertEquals(List.of(AgentAppEventActionType.INSTALL, AgentAppEventActionType.RESTART, AgentAppEventActionType.UPDATE),
                ascByActionType.getData().stream().map(AgentAppEvent::getActionType).toList());

        cleanup(agent, app);
    }

    @Test
    public void testFindByFilter_rejectsUnmappedSortProperty() {
        Agent agent = createAgent("Agent bad sort");
        AgentApplication app = createApp(agent, "bad-sort-app");
        saveEvent(agent, app, AgentAppEventActionType.INSTALL, ProcessingStartStatus.DELIVERED, AgentProcessingStatus.FINISHED);

        AgentAppEventFilter filter = AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app.getId()).build();
        PageLink pageLink = new PageLink(100, 0, null, new SortOrder("errorMessage", SortOrder.Direction.ASC));

        Assertions.assertThrows(Exception.class, () -> agentAppEventService.findByFilter(filter, pageLink));

        cleanup(agent, app);
    }

    @Test
    public void testFindInfosByBulkActionId() {
        Agent agent = createAgent("Agent bulk events");
        AgentApplication app = createApp(agent, "bulk-app");

        AgentBulkActionId bulkActionId = createBulkAction().getId();

        AgentAppEvent inBulk1 = newEvent(agent, app, AgentAppEventActionType.UPDATE, ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);
        inBulk1.setBulkActionId(bulkActionId.getId());
        agentAppEventService.save(tenantId, inBulk1, false);

        AgentAppEvent inBulk2 = newEvent(agent, app, AgentAppEventActionType.UPDATE, ProcessingStartStatus.DELIVERED, AgentProcessingStatus.FINISHED);
        inBulk2.setBulkActionId(bulkActionId.getId());
        agentAppEventService.save(tenantId, inBulk2, false);

        // an event not part of the bulk action
        saveEvent(agent, app, AgentAppEventActionType.RESTART, ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);

        PageData<AgentAppEventInfo> all = agentAppEventService.findInfosByBulkActionId(
                bulkActionId, null, null, new PageLink(100));
        Assert.assertEquals(2, all.getTotalElements());
        Assert.assertTrue(all.getData().stream().allMatch(e -> bulkActionId.getId().equals(e.getBulkActionId())));

        // filter the bulk results by status
        PageData<AgentAppEventInfo> finishedOnly = agentAppEventService.findInfosByBulkActionId(
                bulkActionId, null, AgentProcessingStatus.FINISHED, new PageLink(100));
        Assert.assertEquals(1, finishedOnly.getTotalElements());

        cleanup(agent, app);
    }

    @Test
    public void testHasActiveOrPendingEventForApplication() {
        Agent agent = createAgent("Agent pending check");
        AgentApplication withEvent = createApp(agent, "with-event");
        AgentApplication withoutEvent = createApp(agent, "without-event");

        saveEvent(agent, withEvent, AgentAppEventActionType.INSTALL, ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);

        Assert.assertTrue(agentAppEventService.hasActiveOrPendingEventForApplication(withEvent.getId()));
        Assert.assertFalse(agentAppEventService.hasActiveOrPendingEventForApplication(withoutEvent.getId()));

        cleanup(agent, withEvent, withoutEvent);
    }

    @Test
    public void testMergeContextMetadata_mergesKeysAndKeepsExistingOnes() {
        Agent agent = createAgent("Agent context metadata");
        AgentApplication app = createApp(agent, "context-metadata-app");

        AgentAppEvent event = newEvent(agent, app, AgentAppEventActionType.UPGRADE,
                ProcessingStartStatus.DELIVERED, AgentProcessingStatus.PROCESSING);
        event.setContextMetadata(Map.of("existing", "1", "overwritten", "old"));
        AgentAppEvent saved = agentAppEventService.save(tenantId, event, false);

        Assert.assertTrue(agentAppEventService.mergeContextMetadata(saved, Map.of("overwritten", "new", "added", "2")));

        Assert.assertEquals(Map.of("existing", "1", "overwritten", "new", "added", "2"), saved.getContextMetadata());
        AgentAppEvent found = agentAppEventService.findById(tenantId, saved.getId());
        Assert.assertEquals(Map.of("existing", "1", "overwritten", "new", "added", "2"), found.getContextMetadata());

        cleanup(agent, app);
    }

    @Test
    public void testMergeContextMetadata_onEventWithoutMetadata() {
        Agent agent = createAgent("Agent empty context metadata");
        AgentApplication app = createApp(agent, "empty-context-metadata-app");

        AgentAppEvent saved = agentAppEventService.save(tenantId,
                newEvent(agent, app, AgentAppEventActionType.UPGRADE, ProcessingStartStatus.DELIVERED,
                        AgentProcessingStatus.PROCESSING), false);
        Assert.assertNull(saved.getContextMetadata());

        Assert.assertTrue(agentAppEventService.mergeContextMetadata(saved, Map.of("added", "1")));

        Assert.assertEquals(Map.of("added", "1"),
                agentAppEventService.findById(tenantId, saved.getId()).getContextMetadata());

        cleanup(agent, app);
    }

    @Test
    public void testMergeContextMetadata_returnsFalseForAMissingEvent() {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(new AgentAppEventId(UUID.randomUUID()));

        Assert.assertFalse(agentAppEventService.mergeContextMetadata(event, Map.of("added", "1")));
    }

    @Test
    public void testDeleteAllPendingByApplicationId() {
        Agent agent = createAgent("Agent delete pending");
        AgentApplication app = createApp(agent, "delete-pending-app");

        saveEvent(agent, app, AgentAppEventActionType.INSTALL, ProcessingStartStatus.PENDING, AgentProcessingStatus.QUEUED);
        Assert.assertTrue(agentAppEventService.hasActiveOrPendingEventForApplication(app.getId()));

        agentAppEventService.deleteAllPendingByApplicationId(app.getId());

        Assert.assertFalse(agentAppEventService.hasActiveOrPendingEventForApplication(app.getId()));

        cleanup(agent, app);
    }

    // ==================== helpers ====================

    private void saveEvent(Agent agent, AgentApplication app, AgentAppEventActionType actionType,
                           ProcessingStartStatus startStatus, AgentProcessingStatus processingStatus) {
        agentAppEventService.save(tenantId, newEvent(agent, app, actionType, startStatus, processingStatus), false);
    }

    private AgentAppEvent newEvent(Agent agent, AgentApplication app, AgentAppEventActionType actionType,
                                   ProcessingStartStatus startStatus, AgentProcessingStatus processingStatus) {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(app.getId());
        event.setAgentId(agent.getId());
        event.setApplicationName(app.getName());
        event.setActionType(actionType);
        event.setStartStatus(startStatus);
        event.setProcessingStatus(processingStatus);
        event.setUpdatedTime(System.currentTimeMillis());
        return event;
    }

    private AgentBulkAction createBulkAction() {
        AgentProfile agentProfile = new AgentProfile();
        agentProfile.setTenantId(tenantId);
        agentProfile.setName("Bulk Agent Profile " + UUID.randomUUID());
        agentProfile = agentProfileService.saveProfile(agentProfile);

        AgentAppTemplate template = createTemplate();
        AgentAppProfile appProfile = new AgentAppProfile();
        appProfile.setTenantId(tenantId);
        appProfile.setName("Bulk App Profile " + UUID.randomUUID());
        appProfile.setAppType(AgentApplicationType.GENERIC);
        appProfile.setTemplateVersion(template.getCurrentVersion());
        DockerComposeConfig cfg = new DockerComposeConfig();
        cfg.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        appProfile.setConfig(cfg);
        appProfile = agentAppProfileService.saveProfile(appProfile);

        AgentBulkAction action = new AgentBulkAction();
        action.setTenantId(tenantId);
        action.setAgentProfileId(agentProfile.getId().getId());
        action.setApplicationProfileId(appProfile.getId().getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(AgentBulkActionStatus.STARTED);
        return agentBulkActionService.save(tenantId, action);
    }


    private Agent createAgent(String name) {
        return createAgent(tenantId, name);
    }

    private AgentAppTemplate createTemplate() {
        return registerAppTemplate(createAppTemplate(
                AgentApplicationType.GENERIC, AgentApplicationType.GENERIC.getDefaultVersion()));
    }

    private AgentApplication createApp(Agent agent, String name) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setName(name);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        config.setCompose(compose);
        app.setConfig(config);
        return agentApplicationService.save(tenantId, app);
    }

    private void cleanup(Agent agent, AgentApplication... apps) {
        for (AgentApplication app : apps) {
            agentApplicationService.delete(tenantId, app.getId());
        }
        agentService.deleteAgent(tenantId, agent.getId());
    }
}
