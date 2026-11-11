// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.AgentTemplateTestSupport;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentProfileControllerTest extends AbstractControllerTest {

    @Autowired
    AppTemplateRegistry appTemplateRegistry;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveGetAndDeleteProfile() throws Exception {
        AgentProfile saved = createProfile("Controller Profile");
        Assert.assertNotNull(saved.getId());

        AgentProfile found = doGet("/api/agent/profile/" + saved.getId().getId(), AgentProfile.class);
        Assert.assertEquals(saved.getId(), found.getId());

        doDelete("/api/agent/profile/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/agent/profile/" + saved.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testSaveProfile_customerForbidden() throws Exception {
        AgentProfile profile = new AgentProfile();
        profile.setName("Customer Denied Profile");

        loginCustomerUser();
        doPost("/api/agent/profile", profile).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testSetDefaultAgentProfile() throws Exception {
        AgentProfile profile = createProfile("Default Profile");

        AgentProfile result = doPost("/api/agent/profile/" + profile.getId().getId() + "/default",
                "", AgentProfile.class);
        Assert.assertTrue(result.isDefault());
    }

    @Test
    public void testAssignAndUnassignAppProfile() throws Exception {
        AgentProfile agentProfile = createProfile("Relation Profile");
        AgentAppProfile appProfile = createAppProfile("Related App Profile");

        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        List<AgentAppProfileRelationInfo> infos = doGetTyped(
                "/api/agent/profile/" + agentProfile.getId().getId() + "/appProfilesInfo", new TypeReference<>() {});
        Assert.assertEquals(1, infos.size());

        doDelete("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        List<AgentAppProfileRelationInfo> afterUnassign = doGetTyped(
                "/api/agent/profile/" + agentProfile.getId().getId() + "/appProfilesInfo", new TypeReference<>() {});
        Assert.assertTrue(afterUnassign.isEmpty());
    }

    @Test
    public void testSetAppProfileRelatesOnAutoDiscovery_secondProfileOnSameTemplateRejected() throws Exception {
        AgentProfile agentProfile = createProfile("Auto-discovery Profile");
        AgentAppProfile first = createAppProfile("Auto-discovery App Profile 1");
        AgentAppProfile second = createAppProfile("Auto-discovery App Profile 2");
        assignAppProfile(agentProfile, first);
        assignAppProfile(agentProfile, second);

        setRelatesOnAutoDiscovery(agentProfile, first, true).andExpect(status().isOk());
        Assert.assertTrue(relatesOnAutoDiscovery(agentProfile, first));

        setRelatesOnAutoDiscovery(agentProfile, second, true).andExpect(status().isBadRequest());
        Assert.assertFalse(relatesOnAutoDiscovery(agentProfile, second));
        Assert.assertTrue(relatesOnAutoDiscovery(agentProfile, first));
    }

    @Test
    public void testSetAppProfileRelatesOnAutoDiscovery_reEnablingTheSameProfileIsAllowed() throws Exception {
        AgentProfile agentProfile = createProfile("Auto-discovery Re-enable Profile");
        AgentAppProfile appProfile = createAppProfile("Auto-discovery Re-enable App Profile");
        assignAppProfile(agentProfile, appProfile);

        setRelatesOnAutoDiscovery(agentProfile, appProfile, true).andExpect(status().isOk());
        setRelatesOnAutoDiscovery(agentProfile, appProfile, true).andExpect(status().isOk());
        Assert.assertTrue(relatesOnAutoDiscovery(agentProfile, appProfile));

        setRelatesOnAutoDiscovery(agentProfile, appProfile, false).andExpect(status().isOk());
        Assert.assertFalse(relatesOnAutoDiscovery(agentProfile, appProfile));

        // once the first profile is disabled, a sibling on the same template may take over
        AgentAppProfile other = createAppProfile("Auto-discovery Takeover App Profile");
        assignAppProfile(agentProfile, other);
        setRelatesOnAutoDiscovery(agentProfile, other, true).andExpect(status().isOk());
        Assert.assertTrue(relatesOnAutoDiscovery(agentProfile, other));
    }

    private void assignAppProfile(AgentProfile agentProfile, AgentAppProfile appProfile) throws Exception {
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());
    }

    private ResultActions setRelatesOnAutoDiscovery(AgentProfile agentProfile, AgentAppProfile appProfile, boolean relate) throws Exception {
        return doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId() + "/autoDiscovery?relate=" + relate);
    }

    private boolean relatesOnAutoDiscovery(AgentProfile agentProfile, AgentAppProfile appProfile) throws Exception {
        List<AgentAppProfileRelationInfo> infos = doGetTyped(
                "/api/agent/profile/" + agentProfile.getId().getId() + "/appProfilesInfo", new TypeReference<>() {});
        return infos.stream()
                .filter(info -> info.getId().equals(appProfile.getId()))
                .findFirst()
                .map(info -> info.getAdditionalInfo() != null
                        && info.getAdditionalInfo().path(AgentProfileService.RELATES_ON_AUTO_DISCOVERY).asBoolean(false))
                .orElse(false);
    }

    @Test
    public void testSaveProfile_appProfileIdsTriState() throws Exception {
        AgentProfile saved = createProfile("Tri-state Profile");
        AgentAppProfile appProfile = createAppProfile("Tri-state App Profile");

        saved = doPost("/api/agent/profile?appProfileIds=" + appProfile.getId().getId(), saved, AgentProfile.class);
        Assert.assertEquals(1, getAssignedAppProfiles(saved).size());

        saved = doPost("/api/agent/profile", saved, AgentProfile.class);
        Assert.assertEquals(1, getAssignedAppProfiles(saved).size());

        saved = doPost("/api/agent/profile?appProfileIds=", saved, AgentProfile.class);
        Assert.assertTrue(getAssignedAppProfiles(saved).isEmpty());
    }

    @Test
    public void testAssignAppProfiles_blankParamClearsAssignments() throws Exception {
        AgentProfile agentProfile = createProfile("Bulk Assign Profile");
        AgentAppProfile appProfile = createAppProfile("Bulk Assign App Profile");

        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfiles/assign?appProfileIds=" + appProfile.getId().getId()).andExpect(status().isOk());
        Assert.assertEquals(1, getAssignedAppProfiles(agentProfile).size());

        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfiles/assign?appProfileIds=").andExpect(status().isOk());
        Assert.assertTrue(getAssignedAppProfiles(agentProfile).isEmpty());
    }

    @Test
    public void testAssignAppProfiles_missingParamRejected() throws Exception {
        AgentProfile agentProfile = createProfile("Missing Param Profile");

        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfiles/assign").andExpect(status().isBadRequest());
    }

    private List<AgentAppProfileRelationInfo> getAssignedAppProfiles(AgentProfile agentProfile) throws Exception {
        return doGetTyped("/api/agent/profile/" + agentProfile.getId().getId() + "/appProfilesInfo", new TypeReference<>() {});
    }

    @Test
    public void testPreviewBulkOperation_noApps() throws Exception {
        AgentProfile agentProfile = createProfile("Preview Profile");
        AgentAppProfile appProfile = createAppProfile("Preview App Profile");
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        BulkOperationPreview preview = doPostAsync("/api/agent/profile/" + agentProfile.getId().getId()
                        + "/appProfile/" + appProfile.getId().getId() + "/bulk/preview",
                request, BulkOperationPreview.class, status().isOk());
        Assert.assertEquals(0, preview.getTotal());
        Assert.assertEquals(0, preview.getEligible());
    }

    @Test
    public void testPreviewBulkOperation_countsEligibleAndSkippedWithoutMaterializingTheFleet() throws Exception {
        AgentProfile agentProfile = createProfile("Preview Counting Profile");
        AgentAppProfile appProfile = createAppProfile("Preview Counting App Profile");
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        BulkOperationPreview preview = doPostAsync("/api/agent/profile/" + agentProfile.getId().getId()
                        + "/appProfile/" + appProfile.getId().getId() + "/bulk/preview",
                request, BulkOperationPreview.class, status().isOk());
        Assert.assertEquals(0, preview.getTotal());
        Assert.assertEquals(0, preview.getEligible());
        Assert.assertTrue(preview.getSkippedCountsByReason().isEmpty());
        Assert.assertTrue(preview.getSkippedSample().isEmpty());
    }

    @Test
    public void testBulkOperation_customerForbidden() throws Exception {
        AgentProfile agentProfile = createProfile("Bulk Customer Profile");
        AgentAppProfile appProfile = createAppProfile("Bulk Customer App Profile");
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        loginCustomerUser();
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId() + "/bulk", request)
                .andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testBulkOperation_crossTenantDenied() throws Exception {
        AgentProfile agentProfile = createProfile("Bulk Cross Tenant Profile");
        AgentAppProfile appProfile = createAppProfile("Bulk Cross Tenant App Profile");
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        loginDifferentTenant();
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId() + "/bulk", request)
                .andExpect(status().isNotFound());

        loginTenantAdmin();
    }

    @Test
    public void testPreviewBulkOperation_crossTenantDenied() throws Exception {
        AgentProfile agentProfile = createProfile("Preview Cross Tenant Profile");
        AgentAppProfile appProfile = createAppProfile("Preview Cross Tenant App Profile");
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        loginDifferentTenant();
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId() + "/bulk/preview", request)
                .andExpect(status().isNotFound());

        loginTenantAdmin();
    }

    @Test
    public void testGetAgentProfileInfoById() throws Exception {
        AgentProfile saved = createProfile("Info Profile");

        AgentProfileInfo info = doGet("/api/agent/profile/info/" + saved.getId().getId(), AgentProfileInfo.class);
        Assert.assertEquals(saved.getId(), info.getId());
        Assert.assertEquals(saved.getName(), info.getName());

        loginDifferentTenant();
        doGet("/api/agent/profile/info/" + saved.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testGetDefaultAgentProfileInfo() throws Exception {
        AgentProfile profile = createProfile("Default Info Profile");
        doPost("/api/agent/profile/" + profile.getId().getId() + "/default", "", AgentProfile.class);

        AgentProfileInfo defaultInfo = doGet("/api/agent/profile/info/default", AgentProfileInfo.class);
        Assert.assertEquals(profile.getId(), defaultInfo.getId());
    }

    @Test
    public void testGetTenantAgentProfileInfos_paging() throws Exception {
        AgentProfile saved = createProfile("Info Paging Profile");

        PageData<AgentProfileInfo> page = doGetTypedWithPageLink("/api/tenant/agent/profileInfos?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getData().stream().anyMatch(info -> info.getId().equals(saved.getId())));
    }

    @Test
    public void testGetAgentProvisionInstructions() throws Exception {
        AgentProfile saved = createProfile("Provision Instructions Profile");

        AgentInstructions instructions = doGet(
                "/api/agent/profile/instructions/provision/" + saved.getId().getId() + "/docker", AgentInstructions.class);
        Assert.assertNotNull(instructions.getInstructions());
        Assert.assertTrue(instructions.getInstructions().contains("AUTO_PROVISION=true"));
        Assert.assertTrue(instructions.getInstructions().contains("TB_PROVISION_KEY="));

        loginDifferentTenant();
        doGet("/api/agent/profile/instructions/provision/" + saved.getId().getId() + "/docker")
                .andExpect(status().isNotFound());
        loginTenantAdmin();
    }

    @Test
    public void testBulkOperation_createsBulkActionVisibleInBothProfileListings() throws Exception {
        AgentProfile agentProfile = createProfile("Bulk Op Profile");
        AgentAppProfile appProfile = createAppProfile("Bulk Op App Profile");
        assignAppProfile(agentProfile, appProfile);

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        AgentBulkAction created = doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId() + "/bulk", request, AgentBulkAction.class);
        Assert.assertNotNull(created.getId());
        Assert.assertEquals(AgentBulkActionStatus.QUEUED, created.getStatus());
        Assert.assertEquals(AgentAppEventActionType.UPDATE, created.getActionType());
        Assert.assertEquals(agentProfile.getId().getId(), created.getAgentProfileId());
        Assert.assertEquals(appProfile.getId().getId(), created.getApplicationProfileId());

        PageData<AgentBulkAction> byProfile = doGetTypedWithPageLink(
                "/api/agent/profile/" + agentProfile.getId().getId() + "/bulk?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(byProfile.getData().stream().anyMatch(a -> a.getId().equals(created.getId())));

        PageData<AgentBulkAction> byPair = doGetTypedWithPageLink(
                "/api/agent/profile/" + agentProfile.getId().getId()
                        + "/appProfile/" + appProfile.getId().getId() + "/bulk?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(byPair.getData().stream().anyMatch(a -> a.getId().equals(created.getId())));
    }

    @Test
    public void testBulkOperation_rejectsNotAllowedActionType() throws Exception {
        AgentProfile agentProfile = createProfile("Bulk Op Rejected Profile");
        AgentAppProfile appProfile = createAppProfile("Bulk Op Rejected App Profile");
        assignAppProfile(agentProfile, appProfile);

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);

        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId() + "/bulk", request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetTenantAgentProfiles_paging() throws Exception {
        createProfile("Paging Profile A");
        createProfile("Paging Profile B");

        PageData<AgentProfile> page = doGetTypedWithPageLink("/api/tenant/agent/profiles?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    public void testGetProfile_crossTenantDenied() throws Exception {
        AgentProfile saved = createProfile("Cross Tenant Profile");

        loginDifferentTenant();
        doGet("/api/agent/profile/" + saved.getId().getId()).andExpect(status().isNotFound());

        loginTenantAdmin();
    }

    private AgentProfile createProfile(String name) throws Exception {
        AgentProfile profile = new AgentProfile();
        profile.setName(name);
        return doPost("/api/agent/profile", profile, AgentProfile.class);
    }

    private AgentAppProfile createAppProfile(String name) throws Exception {
        AgentAppTemplate template = AgentTemplateTestSupport.registerGenericTemplate(appTemplateRegistry);
        return doPost("/api/agent/app/profile", AgentTemplateTestSupport.appProfileFor(template, name), AgentAppProfile.class);
    }
}
