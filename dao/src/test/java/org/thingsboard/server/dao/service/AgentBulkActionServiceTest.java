// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentBulkActionService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@DaoSqlTest
public class AgentBulkActionServiceTest extends AbstractServiceTest {

    @Autowired
    AgentBulkActionService agentBulkActionService;

    private AgentProfileId agentProfileId;
    private AgentAppProfileId applicationProfileId;

    @Before
    public void setUpProfiles() {
        agentProfileId = createTestAgentProfile("Bulk Agent Profile").getId();
        applicationProfileId = createAppProfile("Bulk App Profile").getId();
    }

    @Test
    public void testSaveFindAndUpdate() {
        AgentBulkAction action = agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.QUEUED));
        Assert.assertNotNull(action.getId());

        AgentBulkAction found = agentBulkActionService.findById(tenantId, action.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(AgentBulkActionStatus.QUEUED, found.getStatus());

        // status / counters round-trip
        found.setStatus(AgentBulkActionStatus.STARTED);
        found.setTotal(10);
        found.setSubmitted(7);
        found.setSkipCounts(Map.of(SkipReason.VERSION_MISMATCH, 2, SkipReason.ACTIVE_EVENT, 1));
        agentBulkActionService.save(tenantId, found);

        AgentBulkAction reloaded = agentBulkActionService.findById(tenantId, found.getId());
        Assert.assertEquals(AgentBulkActionStatus.STARTED, reloaded.getStatus());
        Assert.assertEquals(10, reloaded.getTotal());
        Assert.assertEquals(7, reloaded.getSubmitted());
        Assert.assertEquals(Integer.valueOf(2), reloaded.getSkipCounts().get(SkipReason.VERSION_MISMATCH));
        Assert.assertEquals(Integer.valueOf(1), reloaded.getSkipCounts().get(SkipReason.ACTIVE_EVENT));
    }

    @Test
    public void testFindByAgentProfileIdAndApplicationProfileId() {
        AgentAppProfileId otherAppProfileId = createAppProfile("Other Bulk App Profile").getId();
        agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.QUEUED));
        AgentBulkAction other = newAction(AgentBulkActionStatus.QUEUED);
        other.setApplicationProfileId(otherAppProfileId.getId());
        agentBulkActionService.save(tenantId, other);

        PageData<AgentBulkAction> byProfile = agentBulkActionService.findByAgentProfileId(tenantId, agentProfileId, new PageLink(100));
        Assert.assertEquals(2, byProfile.getTotalElements());

        PageData<AgentBulkAction> byProfileAndApp = agentBulkActionService.findByAgentProfileIdAndApplicationProfileId(
                tenantId, agentProfileId, applicationProfileId, new PageLink(100));
        Assert.assertEquals(1, byProfileAndApp.getTotalElements());
        Assert.assertEquals(applicationProfileId.getId(), byProfileAndApp.getData().get(0).getApplicationProfileId());
    }

    @Test
    public void testFindStuckBulkActions_onlyQueuedOrInProgressBelowThreshold() {
        AgentBulkAction queued = agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.QUEUED));

        AgentBulkAction inProgress = newAction(AgentBulkActionStatus.IN_PROGRESS);
        inProgress.setProcessingStartedTime(System.currentTimeMillis());
        inProgress = agentBulkActionService.save(tenantId, inProgress);

        AgentBulkAction started = agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.STARTED));

        long futureThreshold = System.currentTimeMillis() + 3_600_000L;
        Set<AgentBulkActionId> stuck = agentBulkActionService.findStuckBulkActions(futureThreshold, new PageLink(100))
                .getData().stream().map(AgentBulkAction::getId).collect(Collectors.toSet());

        Assert.assertTrue("QUEUED below threshold is stuck", stuck.contains(queued.getId()));
        Assert.assertTrue("IN_PROGRESS below threshold is stuck", stuck.contains(inProgress.getId()));
        Assert.assertFalse("STARTED is terminal, never stuck", stuck.contains(started.getId()));
    }

    private AgentBulkAction newAction(AgentBulkActionStatus status) {
        AgentBulkAction action = new AgentBulkAction();
        action.setTenantId(tenantId);
        action.setAgentProfileId(agentProfileId.getId());
        action.setApplicationProfileId(applicationProfileId.getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(status);
        return action;
    }

    private AgentProfile createTestAgentProfile(String name) {
        return createAgentProfile(tenantId, name);
    }

    private AgentAppProfile createAppProfile(String name) {
        return createAgentAppProfile(tenantId, name);
    }
}

