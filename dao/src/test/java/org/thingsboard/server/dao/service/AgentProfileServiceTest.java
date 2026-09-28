// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class AgentProfileServiceTest extends AbstractServiceTest {

    @Test
    public void testSaveFindDelete() {
        AgentProfile profile = agentProfileService.saveProfile(newProfile("Profile 1"));
        Assert.assertNotNull(profile.getId());

        AgentProfile found = agentProfileService.findProfileById(tenantId, profile.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("Profile 1", found.getName());

        agentProfileService.deleteProfile(tenantId, profile.getId());
        Assert.assertNull(agentProfileService.findProfileById(tenantId, profile.getId()));
    }

    @Test
    public void testSetDefaultAgentProfile_switchesDefault() {
        AgentProfile p1 = agentProfileService.saveProfile(newProfile("Default P1"));
        AgentProfile p2 = agentProfileService.saveProfile(newProfile("Default P2"));

        Assert.assertTrue(agentProfileService.setDefaultAgentProfile(tenantId, p1.getId()));
        Assert.assertEquals(p1.getId(), agentProfileService.findDefaultAgentProfile(tenantId).getId());

        Assert.assertTrue(agentProfileService.setDefaultAgentProfile(tenantId, p2.getId()));
        Assert.assertEquals(p2.getId(), agentProfileService.findDefaultAgentProfile(tenantId).getId());
        Assert.assertFalse("previous default must be cleared",
                agentProfileService.findProfileById(tenantId, p1.getId()).isDefault());
        // default profiles cannot be deleted directly; tenant teardown cleans them up
    }

    @Test
    public void testSaveSecondDefault_throws() {
        AgentProfile p1 = agentProfileService.saveProfile(newProfile("Uniqueness P1"));
        agentProfileService.setDefaultAgentProfile(tenantId, p1.getId());

        AgentProfile p2 = newProfile("Uniqueness P2");
        p2.setDefault(true);
        Assertions.assertThrows(DataValidationException.class, () -> agentProfileService.saveProfile(p2));
        // p1 is now the default and cannot be deleted directly; tenant teardown cleans it up
    }

    @Test
    public void testUpdateNonExisting_throws() {
        AgentProfile profile = newProfile("Ghost Profile");
        profile.setId(new AgentProfileId(UUID.randomUUID()));
        Assertions.assertThrows(DataValidationException.class, () -> agentProfileService.saveProfile(profile));
    }

    @Test
    public void testAssignAndUnassignAppProfile() {
        AgentProfile agentProfile = agentProfileService.saveProfile(newProfile("Relation Profile"));
        AgentAppProfile appProfile = createAppProfile("Related App Profile");

        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), appProfile.getId());
        Assert.assertEquals(1, agentAppProfileService
                .findProfileRelationInfosByAgentProfileId(tenantId, agentProfile.getId()).size());

        agentProfileService.unassignAppProfileFromAgentProfile(tenantId, agentProfile.getId(), appProfile.getId());
        Assert.assertTrue(agentAppProfileService
                .findProfileRelationInfosByAgentProfileId(tenantId, agentProfile.getId()).isEmpty());

        agentAppProfileService.deleteProfile(tenantId, appProfile.getId());
        agentProfileService.deleteProfile(tenantId, agentProfile.getId());
    }

    // ==================== helpers ====================

    private AgentProfile newProfile(String name) {
        AgentProfile profile = new AgentProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        return profile;
    }

    private AgentAppProfile createAppProfile(String name) {
        return createAgentAppProfile(tenantId, name);
    }
}
