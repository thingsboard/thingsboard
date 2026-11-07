// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeAssignmentRow;
import org.thingsboard.server.common.data.edge.EdgeAssignmentSource;
import org.thingsboard.server.common.data.edge.EdgeAssignments;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.edge.EdgeProfileInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.job.EdgeProfileApplyJobConfiguration;
import org.thingsboard.server.common.data.job.EdgeProfileApplyOp;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EdgeProfileControllerTest extends AbstractControllerTest {

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveGetDeleteProfile() throws Exception {
        EdgeProfile saved = createProfile("Controller Profile");
        Assert.assertNotNull(saved.getId());

        EdgeProfile found = doGet("/api/edgeProfile/" + saved.getId().getId(), EdgeProfile.class);
        Assert.assertEquals(saved.getId(), found.getId());
        Assert.assertEquals("Controller Profile", found.getName());

        doDelete("/api/edgeProfile/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/edgeProfile/" + saved.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testTenantHasRootedDefaultProfile() throws Exception {
        EdgeProfile def = doGet("/api/edgeProfile/default", EdgeProfile.class);
        Assert.assertTrue(def.isDefault());
        Assert.assertEquals("default", def.getName());
        Assert.assertNotNull(def.getRootRuleChainId());
        Assert.assertTrue(def.getProfileData().getRuleChainIds().contains(def.getRootRuleChainId()));
    }

    @Test
    public void testListProfiles() throws Exception {
        createProfile("List A");
        createProfile("List B");
        PageData<EdgeProfile> page = doGetTypedWithPageLink("/api/edgeProfiles?", new TypeReference<>() {}, new PageLink(10, 0, "List"));
        Assert.assertEquals(2, page.getData().size());
    }

    @Test
    public void testDuplicateNameIsRejectedWithReadableMessage() throws Exception {
        EdgeProfile existing = createProfile("Duplicate Name");
        EdgeProfile duplicate = new EdgeProfile();
        duplicate.setName(existing.getName());
        doPost("/api/edgeProfile", duplicate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Edge profile with such name already exists")));
        EdgeProfile renamed = createProfile("Duplicate Rename");
        renamed.setName(existing.getName());
        doPost("/api/edgeProfile", renamed)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Edge profile with such name already exists")));
    }

    @Test
    public void testCustomerUserReadOnly() throws Exception {
        EdgeProfile saved = createRootedProfile("Customer RO");
        loginCustomerAdminUser(); // plain loginCustomerUser() carries no generic permissions to satisfy profilePermissionChecker
        doGet("/api/edgeProfile/" + saved.getId().getId()).andExpect(status().isForbidden());
        doGet("/api/edgeProfile/default").andExpect(status().isForbidden());
        doGet("/api/edgeProfiles?pageSize=10&page=0").andExpect(status().isForbidden());

        EdgeProfileInfo info = doGet("/api/edgeProfile/info/" + saved.getId().getId(), EdgeProfileInfo.class);
        Assert.assertEquals(saved.getName(), info.getName());
        Assert.assertNull(info.getRootRuleChainId());
        Assert.assertTrue(info.getProfileData().getRuleChainIds().isEmpty());
        Assert.assertNotNull(doGet("/api/edgeProfile/info/default", EdgeProfileInfo.class).getId());
        PageData<EdgeProfileInfo> infos = doGetTypedWithPageLink("/api/edgeProfileInfos?", new TypeReference<>() {}, new PageLink(10, 0, "Customer RO"));
        Assert.assertEquals(1, infos.getData().size());
        Assert.assertEquals(saved.getId(), infos.getData().get(0).getId());
        Assert.assertTrue(infos.getData().get(0).getProfileData().getRuleChainIds().isEmpty());

        EdgeProfile fresh = new EdgeProfile();
        fresh.setName("Customer Denied");
        doPost("/api/edgeProfile", fresh).andExpect(status().isForbidden());
        doDelete("/api/edgeProfile/" + saved.getId().getId()).andExpect(status().isForbidden());
        // the resolved rows can name groups of other customers of the tenant, so they are tenant-admin only
        doGet("/api/edgeProfile/" + saved.getId().getId() + "/assignments").andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testCustomerUserSeesNoTenantRuleChainsOrIntegrationsOnEdgeAssignments() throws Exception {
        EdgeProfile profile = createRootedProfile("Customer Edge View");
        RuleChain tenantChain = createEdgeRuleChain("Tenant only chain");
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + tenantChain.getId().getId(), EdgeProfile.class);
        Edge edge = constructEdge("Customer visible edge", null);
        edge.setEdgeProfileId(profile.getId());
        edge.setCustomerId(customerId);
        edge = doPost("/api/edge", edge, Edge.class);

        // unlike the profile-level view this endpoint is open to customer users, so the per-caller filtering in
        // EdgeAssignmentsService.forEdge is the only thing keeping the tenant's rule chains and integrations out of it
        loginCustomerAdminUser();
        EdgeAssignments view = doGet("/api/edge/" + edge.getId().getId() + "/assignments", EdgeAssignments.class);
        Assert.assertTrue(view.getRuleChains().isEmpty());
        Assert.assertTrue(view.getIntegrations().isEmpty());
        loginTenantAdmin();
    }

    @Test
    public void testProfileSwitchDropsRowsOfPreviousProfileAndKeepsHandMadeOnes() throws Exception {
        EdgeProfile oldProfile = createRootedProfile("Switch old");
        RuleChain oldExtra = createEdgeRuleChain("Switch old extra");
        RuleChain shared = createEdgeRuleChain("Switch shared");
        doPost("/api/edgeProfile/" + oldProfile.getId().getId() + "/ruleChain/" + oldExtra.getId().getId(), EdgeProfile.class);
        doPost("/api/edgeProfile/" + oldProfile.getId().getId() + "/ruleChain/" + shared.getId().getId(), EdgeProfile.class);
        EdgeProfile newProfile = createRootedProfile("Switch new");
        doPost("/api/edgeProfile/" + newProfile.getId().getId() + "/ruleChain/" + shared.getId().getId(), EdgeProfile.class);
        RuleChain handMade = createEdgeRuleChain("Switch hand-made");

        Edge edge = saveEdgeOnProfile("Switch edge", oldProfile);
        doPost("/api/edge/" + edge.getId().getId() + "/ruleChain/" + handMade.getId().getId(), RuleChain.class);
        awaitAssigned(edge, oldProfile.getRootRuleChainId(), true);
        awaitAssigned(edge, oldExtra.getId(), true);
        awaitAssigned(edge, shared.getId(), true);

        Edge reloaded = doGet("/api/edge/" + edge.getId().getId(), Edge.class);
        reloaded.setEdgeProfileId(newProfile.getId());
        Edge switched = doPost("/api/edge", reloaded, Edge.class);
        Assert.assertEquals(newProfile.getId(), switched.getEdgeProfileId());
        Assert.assertEquals(newProfile.getRootRuleChainId(), switched.getRootRuleChainId());
        Assert.assertEquals("Switch new", switched.getType());

        // rows the old profile stamped are gone, including its root; shared and hand-made rows stay
        awaitAssigned(edge, oldProfile.getRootRuleChainId(), false);
        awaitAssigned(edge, oldExtra.getId(), false);
        awaitAssigned(edge, newProfile.getRootRuleChainId(), true);
        awaitAssigned(edge, shared.getId(), true);
        awaitAssigned(edge, handMade.getId(), true);

        EdgeAssignments view = doGet("/api/edge/" + edge.getId().getId() + "/assignments", EdgeAssignments.class);
        Assert.assertTrue(view.getRuleChains().stream().anyMatch(r -> r.getId().equals(handMade.getId()) && r.getSource() == EdgeAssignmentSource.EDGE));
        Assert.assertTrue(view.getRuleChains().stream().anyMatch(r -> r.getId().equals(shared.getId()) && r.getSource() == EdgeAssignmentSource.PROFILE));
        Assert.assertTrue(view.getRuleChains().stream().noneMatch(r -> r.getId().equals(oldExtra.getId())));
    }

    @Test
    public void testHandMadeRowStaysEdgeOwnedAfterItsEntityJoinsTheProfile() throws Exception {
        EdgeProfile profile = createRootedProfile("Late join");
        RuleChain chain = createEdgeRuleChain("Late join chain");
        Edge edge = saveEdgeOnProfile("Late join edge", profile);

        // assigned by hand on the edge: the relation is created unstamped, so it belongs to the edge, not the profile
        doPost("/api/edge/" + edge.getId().getId() + "/ruleChain/" + chain.getId().getId(), RuleChain.class);
        awaitAssigned(edge, chain.getId(), true);

        // the same chain later joins the profile; the applier leaves the edge's existing unstamped relation alone
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + chain.getId().getId(), EdgeProfile.class);
        awaitAssigned(edge, chain.getId(), true);

        // Source has to follow the stamp rather than profile membership: reported as PROFILE, the UI locks the row
        // on the edge page while the profile page skips it as not-stamped-by-me, stranding it on both screens
        EdgeAssignments view = doGet("/api/edge/" + edge.getId().getId() + "/assignments", EdgeAssignments.class);
        Assert.assertTrue("a hand-made row must stay edge-owned once its entity joins the profile",
                view.getRuleChains().stream()
                        .anyMatch(r -> r.getId().equals(chain.getId()) && r.getSource() == EdgeAssignmentSource.EDGE));
    }

    @Test
    public void testUpdateIgnoresProfileDataAndRoot() throws Exception {
        EdgeProfile saved = createRootedProfile("Update Ignores Rows");
        RuleChainId root = saved.getRootRuleChainId();
        RuleChain other = createEdgeRuleChain("Update Ignores Rows other RC");

        saved.setName("Update Ignores Rows renamed");
        saved.getProfileData().getRuleChainIds().clear();
        saved.getProfileData().getRuleChainIds().add(other.getId());
        saved.setRootRuleChainId(other.getId());
        EdgeProfile updated = doPost("/api/edgeProfile", saved, EdgeProfile.class);

        Assert.assertEquals("Update Ignores Rows renamed", updated.getName());
        Assert.assertEquals(root, updated.getRootRuleChainId());
        Assert.assertEquals(List.of(root), updated.getProfileData().getRuleChainIds());
        EdgeProfile found = doGet("/api/edgeProfile/" + saved.getId().getId(), EdgeProfile.class);
        Assert.assertEquals(root, found.getRootRuleChainId());
        Assert.assertEquals(List.of(root), found.getProfileData().getRuleChainIds());
    }

    @Test
    public void testCreateWithUnreadableEntityGroupIsForbidden() throws Exception {
        loginDifferentTenant();
        EntityGroup group = new EntityGroup();
        group.setName("Other tenant device group");
        group.setType(EntityType.DEVICE);
        EntityGroup otherTenantGroup = doPost("/api/entityGroup", group, EntityGroup.class);
        loginTenantAdmin();

        EdgeProfile profile = new EdgeProfile();
        profile.setName("Unreadable group profile");
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, otherTenantGroup.getId()));
        doPost("/api/edgeProfile", profile).andExpect(status().isForbidden());

        PageData<EdgeProfile> page = doGetTypedWithPageLink("/api/edgeProfiles?", new TypeReference<>() {}, new PageLink(10, 0, "Unreadable group profile"));
        Assert.assertTrue(page.getData().isEmpty());
    }

    @Test
    public void testCrossTenantAccessDenied() throws Exception {
        EdgeProfile saved = createProfile("Cross Tenant Profile");

        loginDifferentTenant();
        doGet("/api/edgeProfile/" + saved.getId().getId()).andExpect(status().isNotFound());
        doDelete("/api/edgeProfile/" + saved.getId().getId()).andExpect(status().isNotFound());
        loginTenantAdmin();

        doGet("/api/edgeProfile/" + saved.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testSetDefaultRequiresRoot() throws Exception {
        EdgeProfile rootless = createProfile("Rootless default");
        doPost("/api/edgeProfile/" + rootless.getId().getId() + "/default")
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("no root rule chain")));

        RuleChain rc = createEdgeRuleChain("Default RC");
        EdgeProfile rooted = new EdgeProfile();
        rooted.setName("Rooted default");
        rooted.getProfileData().getRuleChainIds().add(rc.getId());
        rooted.setRootRuleChainId(rc.getId());
        rooted = doPost("/api/edgeProfile", rooted, EdgeProfile.class);
        EdgeProfile result = doPost("/api/edgeProfile/" + rooted.getId().getId() + "/default", "", EdgeProfile.class);
        Assert.assertTrue(result.isDefault());
        Assert.assertEquals(rooted.getId(), doGet("/api/edgeProfile/default", EdgeProfile.class).getId());
    }

    @Test
    public void testDeleteDefaultAndInUseProhibited() throws Exception {
        EdgeProfile def = doGet("/api/edgeProfile/default", EdgeProfile.class);
        doDelete("/api/edgeProfile/" + def.getId().getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Default Edge Profile is prohibited")));

        EdgeProfile inUse = createRootedProfile("In use");
        Edge edge = constructEdge("Edge on profile", null);
        edge.setEdgeProfileId(inUse.getId());
        edge = doPost("/api/edge", edge, Edge.class);
        Assert.assertEquals("In use", edge.getType());
        doDelete("/api/edgeProfile/" + inUse.getId().getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("in use")));
        doDelete("/api/edge/" + edge.getId().getId()).andExpect(status().isOk());
        doDelete("/api/edgeProfile/" + inUse.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testSaveEdgeWithTypeCreatesProfileWithDefaultRoot() throws Exception {
        EdgeProfile def = doGet("/api/edgeProfile/default", EdgeProfile.class);
        Edge edge = doPost("/api/edge", constructEdge("Typed edge", "storeType"), Edge.class);
        Assert.assertEquals("storeType", edge.getType());
        EdgeProfile created = doGet("/api/edgeProfile/" + edge.getEdgeProfileId().getId(), EdgeProfile.class);
        Assert.assertEquals("storeType", created.getName());
        Assert.assertEquals(def.getRootRuleChainId(), created.getRootRuleChainId());
        Assert.assertEquals(def.getRootRuleChainId(), edge.getRootRuleChainId());
    }

    @Test
    public void testSaveEdgeOnRootlessProfileRejected() throws Exception {
        EdgeProfile rootless = createProfile("Rootless edge target");
        Edge edge = constructEdge("Edge rootless", null);
        edge.setEdgeProfileId(rootless.getId());
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("no root rule chain")));
    }

    @Test
    public void testAddRowFansOutToEdgesAndRemoveUnassignsStampedOnly() throws Exception {
        EdgeProfile profile = createRootedProfile("Fanout");
        Edge e1 = saveEdgeOnProfile("Fanout edge 1", profile);
        Edge e2 = saveEdgeOnProfile("Fanout edge 2", profile);
        RuleChain extra = createEdgeRuleChain("Fanout extra");
        doPost("/api/edge/" + e2.getId().getId() + "/ruleChain/" + extra.getId().getId(), RuleChain.class);

        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + extra.getId().getId(), EdgeProfile.class);
        awaitAssigned(e1, extra.getId(), true);
        awaitAssigned(e2, extra.getId(), true);

        EdgeAssignments edgeView = doGet("/api/edge/" + e1.getId().getId() + "/assignments", EdgeAssignments.class);
        Assert.assertTrue(edgeView.getRuleChains().stream().anyMatch(r -> r.getId().equals(extra.getId()) && r.getSource() == EdgeAssignmentSource.PROFILE));
        EdgeAssignments profileView = doGet("/api/edgeProfile/" + profile.getId().getId() + "/assignments", EdgeAssignments.class);
        Assert.assertEquals(2, profileView.getEdgesCount());
        Assert.assertEquals(2, profileView.getRuleChains().size());
        Assert.assertTrue(profileView.getRuleChains().stream().anyMatch(r -> r.isRoot() && r.getId().equals(profile.getRootRuleChainId())));

        doDelete("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + extra.getId().getId()).andExpect(status().isOk());
        Job removal = awaitLatestJob(profile, JobStatus.COMPLETED);
        Assert.assertEquals(EdgeProfileApplyOp.REMOVE_ROW, ((EdgeProfileApplyJobConfiguration) removal.getConfiguration()).getOp());
        Assert.assertEquals(Integer.valueOf(2), removal.getResult().getTotalCount());
        awaitAssigned(e1, extra.getId(), false);
        awaitAssigned(e2, extra.getId(), true);
    }

    @Test
    public void testAddEntityGroupRowFansOutAndRemovePreservesHandMadeAssignment() throws Exception {
        EdgeProfile profile = createRootedProfile("Group Fanout");
        Edge e1 = saveEdgeOnProfile("Group Fanout edge 1", profile);
        Edge e2 = saveEdgeOnProfile("Group Fanout edge 2", profile);
        EntityGroup group = new EntityGroup();
        group.setName("Group Fanout device group");
        group.setType(EntityType.DEVICE);
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        // hand-made assignment on e2, made before the profile ever lists the group
        doPost("/api/edge/" + e2.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/DEVICE", EntityGroup.class);

        doPost("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/DEVICE", EdgeProfile.class);
        awaitGroupAssigned(e1, savedGroup.getId(), true);
        awaitGroupAssigned(e2, savedGroup.getId(), true);

        doDelete("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/DEVICE").andExpect(status().isOk());
        awaitLatestJob(profile, JobStatus.COMPLETED);
        awaitGroupAssigned(e1, savedGroup.getId(), false);
        // hand-made assignment on e2 must survive the profile's removal
        awaitGroupAssigned(e2, savedGroup.getId(), true);
    }

    @Test
    public void testProfileRootChangeMovesFollowersAndKeepsOverriddenEdges() throws Exception {
        EdgeProfile profile = createRootedProfile("Root prop");
        Edge follower = saveEdgeOnProfile("Root prop follower", profile);
        Edge overridden = saveEdgeOnProfile("Root prop overridden", profile);
        RuleChain ownRoot = createEdgeRuleChain("Root prop own");
        doPost("/api/edge/" + overridden.getId().getId() + "/" + ownRoot.getId().getId() + "/root", "", Edge.class);

        RuleChain newRoot = createEdgeRuleChain("Root prop new");
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + newRoot.getId().getId(), EdgeProfile.class);
        EdgeProfile updated = doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + newRoot.getId().getId() + "/root", "", EdgeProfile.class);
        Assert.assertEquals(newRoot.getId(), updated.getRootRuleChainId());
        awaitRoot(follower, newRoot.getId());
        awaitLatestJob(profile, JobStatus.COMPLETED);
        // the new root still reaches the overridden edge as a profile row; only the root flag stays where the edge put it
        awaitAssigned(overridden, newRoot.getId(), true);
        Assert.assertEquals(ownRoot.getId(), doGet("/api/edge/" + overridden.getId().getId(), Edge.class).getRootRuleChainId());

        // a re-apply repairs rows but leaves the override alone; the repaired row shows when the re-apply has run
        doDelete("/api/edge/" + overridden.getId().getId() + "/ruleChain/" + newRoot.getId().getId(), RuleChain.class);
        awaitAssigned(overridden, newRoot.getId(), false);
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/apply").andExpect(status().isOk());
        awaitAssigned(overridden, newRoot.getId(), true);
        Assert.assertEquals(ownRoot.getId(), doGet("/api/edge/" + overridden.getId().getId(), Edge.class).getRootRuleChainId());

        doDelete("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + newRoot.getId().getId())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSetEdgeRootRuleChainOverridesProfileRootAndAssignsTheChain() throws Exception {
        EdgeProfile profile = createRootedProfile("Edge root");
        Edge edge = saveEdgeOnProfile("Edge root edge", profile);
        RuleChain ownRoot = createEdgeRuleChain("Edge root own");

        Edge updated = doPost("/api/edge/" + edge.getId().getId() + "/" + ownRoot.getId().getId() + "/root", "", Edge.class);
        Assert.assertEquals(ownRoot.getId(), updated.getRootRuleChainId());

        // the chain was not assigned yet: making it root assigns it as a hand-made row, and the profile root stays assigned
        EdgeAssignments view = doGet("/api/edge/" + edge.getId().getId() + "/assignments", EdgeAssignments.class);
        Assert.assertEquals(ownRoot.getId(), view.getRootRuleChainId());
        Assert.assertEquals(profile.getRootRuleChainId(), view.getProfileRootRuleChainId());
        EdgeAssignmentRow own = view.getRuleChains().stream().filter(r -> r.getId().equals(ownRoot.getId())).findFirst().orElseThrow();
        Assert.assertTrue(own.isRoot());
        Assert.assertEquals(EdgeAssignmentSource.EDGE, own.getSource());
        EdgeAssignmentRow profileRoot = view.getRuleChains().stream().filter(r -> r.getId().equals(profile.getRootRuleChainId())).findFirst().orElseThrow();
        Assert.assertFalse(profileRoot.isRoot());
        Assert.assertEquals(EdgeAssignmentSource.PROFILE, profileRoot.getSource());

        doDelete("/api/edge/" + edge.getId().getId() + "/ruleChain/" + ownRoot.getId().getId()).andExpect(status().isBadRequest());

        RuleChain core = new RuleChain();
        core.setName("Edge root core");
        core.setType(RuleChainType.CORE);
        core = doPost("/api/ruleChain", core, RuleChain.class);
        doPost("/api/edge/" + edge.getId().getId() + "/" + core.getId().getId() + "/root").andExpect(status().isBadRequest());

        // moving to another profile replaces the override with that profile's root
        EdgeProfile other = createRootedProfile("Edge root other");
        Edge reloaded = doGet("/api/edge/" + edge.getId().getId(), Edge.class);
        reloaded.setEdgeProfileId(other.getId());
        Edge switched = doPost("/api/edge", reloaded, Edge.class);
        Assert.assertEquals(other.getRootRuleChainId(), switched.getRootRuleChainId());
    }

    @Test
    public void testReapplyRepairsMissingRow() throws Exception {
        EdgeProfile profile = createRootedProfile("Repair");
        Edge edge = saveEdgeOnProfile("Repair edge", profile);
        RuleChain extra = createEdgeRuleChain("Repair extra");
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + extra.getId().getId(), EdgeProfile.class);
        awaitAssigned(edge, extra.getId(), true);
        doDelete("/api/edge/" + edge.getId().getId() + "/ruleChain/" + extra.getId().getId(), RuleChain.class);
        awaitAssigned(edge, extra.getId(), false);
        Job apply = doPost("/api/edgeProfile/" + profile.getId().getId() + "/apply", Job.class);
        Assert.assertEquals(JobType.EDGE_PROFILE_APPLY, apply.getType());
        Assert.assertEquals(profile.getId(), apply.getEntityId());
        Assert.assertEquals(EdgeProfileApplyOp.APPLY, ((EdgeProfileApplyJobConfiguration) apply.getConfiguration()).getOp());
        Job completed = awaitLatestJob(profile, JobStatus.COMPLETED);
        Assert.assertEquals(apply.getId(), completed.getId());
        Assert.assertEquals(1, completed.getResult().getSuccessfulCount());
        awaitAssigned(edge, extra.getId(), true);

        List<Job> listed = findJobs(List.of(JobType.EDGE_PROFILE_APPLY), List.of(profile.getId().getId()));
        Assert.assertTrue(listed.stream().allMatch(job -> profile.getName().equals(job.getEntityName())));
    }

    @Test
    public void testRowChangesOfATenantRunAsJobsInSubmissionOrder() throws Exception {
        EdgeProfile profile = createRootedProfile("Ordered");
        Edge edge = saveEdgeOnProfile("Ordered edge", profile);
        RuleChain first = createEdgeRuleChain("Ordered first");
        RuleChain second = createEdgeRuleChain("Ordered second");

        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + first.getId().getId(), EdgeProfile.class);
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + second.getId().getId(), EdgeProfile.class);
        doDelete("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + first.getId().getId()).andExpect(status().isOk());

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                findJobs(List.of(JobType.EDGE_PROFILE_APPLY), List.of(profile.getId().getId())).stream()
                        .filter(job -> job.getCreatedTime() >= edge.getCreatedTime())
                        .allMatch(job -> job.getStatus() == JobStatus.COMPLETED));
        List<Job> jobs = findJobs(List.of(JobType.EDGE_PROFILE_APPLY), List.of(profile.getId().getId())).stream()
                .filter(job -> job.getCreatedTime() >= edge.getCreatedTime())
                .sorted(Comparator.comparingLong(Job::getCreatedTime)).toList();
        Assert.assertEquals(List.of(EdgeProfileApplyOp.ADD_ROWS, EdgeProfileApplyOp.ADD_ROWS, EdgeProfileApplyOp.REMOVE_ROW),
                jobs.stream().map(job -> ((EdgeProfileApplyJobConfiguration) job.getConfiguration()).getOp()).toList());
        for (int i = 1; i < jobs.size(); i++) {
            Assert.assertTrue(jobs.get(i).getResult().getStartTs() >= jobs.get(i - 1).getResult().getFinishTs());
        }
        awaitAssigned(edge, first.getId(), false);
        awaitAssigned(edge, second.getId(), true);
    }

    @Test
    public void testCustomerOwnerChangeRunsAsJobOnTheMovedCustomer() throws Exception {
        Customer parent = doPost("/api/customer", customer("Owner job parent"), Customer.class);
        Customer moved = doPost("/api/customer", customer("Owner job moved"), Customer.class);

        doPost("/api/owner/CUSTOMER/" + parent.getId().getId() + "/CUSTOMER/" + moved.getId().getId()).andExpect(status().isOk());

        Job job = Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                findJobs(List.of(JobType.EDGE_PROFILE_APPLY), List.of(moved.getId().getId())).stream().findFirst().orElse(null),
                found -> found != null && found.getStatus() == JobStatus.COMPLETED);
        Assert.assertEquals(EdgeProfileApplyOp.OWNER_CHANGE, ((EdgeProfileApplyJobConfiguration) job.getConfiguration()).getOp());
        Assert.assertEquals(moved.getId(), job.getEntityId());
    }

    @Test
    public void testEntityGroupRowRejectsAllGroup() throws Exception {
        EdgeProfile profile = createRootedProfile("Group rules");
        EntityGroup all = doGet("/api/entityGroup/TENANT/" + tenantId.getId() + "/DEVICE/All", EntityGroup.class);
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + all.getId().getId() + "/DEVICE")
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddRowsSavesOnceAndFansOutInOneJob() throws Exception {
        EdgeProfile profile = createRootedProfile("Bulk rows");
        Edge edge = saveEdgeOnProfile("Bulk rows edge", profile);
        RuleChain rc1 = createEdgeRuleChain("Bulk rows RC 1");
        RuleChain rc2 = createEdgeRuleChain("Bulk rows RC 2");
        EdgeProfileData rows = new EdgeProfileData();
        rows.getRuleChainIds().addAll(List.of(rc1.getId(), rc2.getId()));
        for (String name : List.of("Bulk rows group 1", "Bulk rows group 2")) {
            EntityGroup group = new EntityGroup();
            group.setName(name);
            group.setType(EntityType.DEVICE);
            group = doPost("/api/entityGroup", group, EntityGroup.class);
            rows.getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, group.getId()));
        }

        EdgeProfile updated = doPost("/api/edgeProfile/" + profile.getId().getId() + "/rows", rows, EdgeProfile.class);

        Assert.assertTrue(updated.getProfileData().getRuleChainIds().containsAll(rows.getRuleChainIds()));
        Assert.assertTrue(updated.getProfileData().getEntityGroups().containsAll(rows.getEntityGroups()));
        Job job = awaitLatestJob(profile, JobStatus.COMPLETED);
        EdgeProfileApplyJobConfiguration configuration = (EdgeProfileApplyJobConfiguration) job.getConfiguration();
        Assert.assertEquals(EdgeProfileApplyOp.ADD_ROWS, configuration.getOp());
        Assert.assertEquals(4, configuration.getRows().size());
        awaitAssigned(edge, rc1.getId(), true);
        awaitAssigned(edge, rc2.getId(), true);
        for (EdgeProfileEntityGroup group : rows.getEntityGroups()) {
            Assert.assertTrue(groupAssignedToEdge(edge, group.getEntityGroupId()));
        }
    }

    @Test
    public void testAddRowsWithUnreadableRowAddsNothing() throws Exception {
        loginDifferentTenant();
        EntityGroup group = new EntityGroup();
        group.setName("Bulk rows other tenant group");
        group.setType(EntityType.DEVICE);
        EntityGroup otherTenantGroup = doPost("/api/entityGroup", group, EntityGroup.class);
        loginTenantAdmin();
        EdgeProfile profile = createRootedProfile("Bulk rows forbidden");
        RuleChain rc = createEdgeRuleChain("Bulk rows forbidden RC");
        EdgeProfileData rows = new EdgeProfileData();
        rows.getRuleChainIds().add(rc.getId());
        rows.getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, otherTenantGroup.getId()));

        doPost("/api/edgeProfile/" + profile.getId().getId() + "/rows", rows).andExpect(status().isForbidden());

        EdgeProfile found = doGet("/api/edgeProfile/" + profile.getId().getId(), EdgeProfile.class);
        Assert.assertFalse(found.getProfileData().getRuleChainIds().contains(rc.getId()));
    }

    @Test
    public void testProfileAssignmentsListRowsInProfileOrder() throws Exception {
        EdgeProfile profile = createRootedProfile("Assignments order");
        RuleChain second = createEdgeRuleChain("Assignments order RC 2");
        RuleChain third = createEdgeRuleChain("Assignments order RC 3");
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + second.getId().getId(), EdgeProfile.class);
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/ruleChain/" + third.getId().getId(), EdgeProfile.class);
        List<EntityGroup> groups = new ArrayList<>();
        for (String name : List.of("Assignments order group B", "Assignments order group A")) {
            EntityGroup group = new EntityGroup();
            group.setName(name);
            group.setType(EntityType.DEVICE);
            group = doPost("/api/entityGroup", group, EntityGroup.class);
            doPost("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + group.getId().getId() + "/DEVICE", EdgeProfile.class);
            groups.add(group);
        }

        EdgeAssignments view = doGet("/api/edgeProfile/" + profile.getId().getId() + "/assignments", EdgeAssignments.class);

        Assert.assertEquals(List.of(profile.getRootRuleChainId(), second.getId(), third.getId()),
                view.getRuleChains().stream().map(EdgeAssignmentRow::getId).toList());
        Assert.assertTrue(view.getRuleChains().get(0).isRoot());
        Assert.assertEquals(List.of(groups.get(0).getId(), groups.get(1).getId()),
                view.getEntityGroups().stream().map(EdgeAssignmentRow::getId).toList());
        Assert.assertEquals(List.of("Assignments order group B", "Assignments order group A"),
                view.getEntityGroups().stream().map(EdgeAssignmentRow::getName).toList());
    }

    @Test
    public void testRemoveEntityGroupRowRejectsMismatchedGroupType() throws Exception {
        EdgeProfile profile = createRootedProfile("Group type on remove");
        EntityGroup group = new EntityGroup();
        group.setName("Group type on remove devices");
        group.setType(EntityType.DEVICE);
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/DEVICE", EdgeProfile.class);

        doDelete("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/ASSET")
                .andExpect(status().isBadRequest());

        EdgeProfile found = doGet("/api/edgeProfile/" + profile.getId().getId(), EdgeProfile.class);
        Assert.assertTrue(found.getProfileData().getEntityGroups().stream().anyMatch(row -> savedGroup.getId().equals(row.getEntityGroupId())));
    }

    @Test
    public void testEdgeCreateInheritsProfileRuleChainAndEntityGroup() throws Exception {
        EdgeProfile profile = createRootedProfile("Inherit");
        EntityGroup group = new EntityGroup();
        group.setName("Inherit device group");
        group.setType(EntityType.DEVICE);
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/DEVICE", EdgeProfile.class);

        Edge edge = saveEdgeOnProfile("Inherit edge", profile);

        awaitAssigned(edge, profile.getRootRuleChainId(), true);
        PageData<EntityGroupInfo> edgeGroups = doGetTypedWithPageLink("/api/entityGroups/edge/" + edge.getId().getId() + "/DEVICE?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(edgeGroups.getData().stream().anyMatch(g -> g.getId().equals(savedGroup.getId())));
    }

    @Test
    public void testOwnerChangeReappliesProfileWithNewOwnerEligibility() throws Exception {
        Customer customerA = doPost("/api/customer", customer("Eligibility Customer A"), Customer.class);
        Customer customerB = doPost("/api/customer", customer("Eligibility Customer B"), Customer.class);

        EntityGroup group = new EntityGroup();
        group.setName("Eligibility group");
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customerA.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        RuleChain rc = createEdgeRuleChain("Eligibility RC");
        EdgeProfile profile = new EdgeProfile();
        profile.setName("Eligibility profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, savedGroup.getId()));
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);

        // edge is created directly owned by customer B, unrelated to customer A: the group is ineligible
        Edge edge = constructEdge("Eligibility edge", null);
        edge.setEdgeProfileId(profile.getId());
        edge.setCustomerId(customerB.getId());
        edge = doPost("/api/edge", edge, Edge.class);
        Assert.assertFalse(groupAssignedToEdge(edge, savedGroup.getId()));

        // move the edge to customer A: the owner-change hook must re-apply and pick up the now-eligible group
        doPost("/api/owner/CUSTOMER/" + customerA.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        awaitEdgeOwnerChangeJob(edge);
        Assert.assertTrue(groupAssignedToEdge(edge, savedGroup.getId()));
    }

    @Test
    public void testOwnerChangeUnassignsProfileGroupIneligibleForNewOwner() throws Exception {
        Customer customerA = doPost("/api/customer", customer("Ineligibility Customer A"), Customer.class);
        Customer customerB = doPost("/api/customer", customer("Ineligibility Customer B"), Customer.class);

        EntityGroup group = new EntityGroup();
        group.setName("Ineligibility group");
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customerA.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        RuleChain rc = createEdgeRuleChain("Ineligibility RC");
        EdgeProfile profile = new EdgeProfile();
        profile.setName("Ineligibility profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, savedGroup.getId()));
        profile = doPost("/api/edgeProfile", profile, EdgeProfile.class);

        // edge starts under customer A, so the profile stamps the group onto it
        Edge edge = constructEdge("Ineligibility edge", null);
        edge.setEdgeProfileId(profile.getId());
        edge.setCustomerId(customerA.getId());
        edge = doPost("/api/edge", edge, Edge.class);
        Assert.assertTrue(groupAssignedToEdge(edge, savedGroup.getId()));

        // move the edge to customer B: customer A's group is no longer eligible and must not stay assigned
        doPost("/api/owner/CUSTOMER/" + customerB.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        awaitEdgeOwnerChangeJob(edge);
        Assert.assertFalse(groupAssignedToEdge(edge, savedGroup.getId()));
    }

    @Test
    public void testOwnerChangeKeepsHandMadeAssignmentOfProfileGroupIneligibleForNewOwner() throws Exception {
        Customer customerA = doPost("/api/customer", customer("Hand-made Customer A"), Customer.class);
        Customer customerB = doPost("/api/customer", customer("Hand-made Customer B"), Customer.class);

        EntityGroup group = new EntityGroup();
        group.setName("Hand-made group");
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customerA.getId());
        EntityGroup savedGroup = doPost("/api/entityGroup", group, EntityGroup.class);

        EdgeProfile profile = createRootedProfile("Hand-made");
        Edge edge = constructEdge("Hand-made edge", null);
        edge.setEdgeProfileId(profile.getId());
        edge.setCustomerId(customerA.getId());
        edge = doPost("/api/edge", edge, Edge.class);

        // assigned by hand first, so the later profile row never stamps this relation
        doPost("/api/edge/" + edge.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/DEVICE", EntityGroup.class);
        doPost("/api/edgeProfile/" + profile.getId().getId() + "/entityGroup/" + savedGroup.getId().getId() + "/DEVICE", EdgeProfile.class);
        awaitGroupAssigned(edge, savedGroup.getId(), true);

        doPost("/api/owner/CUSTOMER/" + customerB.getId().getId() + "/EDGE/" + edge.getId().getId()).andExpect(status().isOk());
        awaitEdgeOwnerChangeJob(edge);
        Assert.assertTrue(groupAssignedToEdge(edge, savedGroup.getId()));
    }

    private Customer customer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return customer;
    }

    private boolean groupAssignedToEdge(Edge edge, EntityGroupId groupId) throws Exception {
        PageData<EntityGroupInfo> groups = doGetTypedWithPageLink("/api/entityGroups/edge/" + edge.getId().getId() + "/DEVICE?",
                new TypeReference<>() {}, new PageLink(100));
        return groups.getData().stream().anyMatch(g -> g.getId().equals(groupId));
    }

    private void awaitGroupAssigned(Edge edge, EntityGroupId groupId, boolean expected) {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> groupAssignedToEdge(edge, groupId) == expected);
    }

    private Edge saveEdgeOnProfile(String name, EdgeProfile profile) throws Exception {
        Edge edge = constructEdge(name, null);
        edge.setEdgeProfileId(profile.getId());
        return doPost("/api/edge", edge, Edge.class);
    }

    private Job awaitLatestJob(EdgeProfile profile, JobStatus expected) {
        return Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
                () -> doGet("/api/edgeProfile/" + profile.getId().getId() + "/job", Job.class),
                job -> job != null && job.getStatus() == expected);
    }

    private void awaitEdgeOwnerChangeJob(Edge edge) {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            List<Job> jobs = findJobs(List.of(JobType.EDGE_PROFILE_APPLY), List.of(edge.getId().getId()));
            return !jobs.isEmpty() && jobs.stream().allMatch(job -> job.getStatus() == JobStatus.COMPLETED);
        });
    }

    private void awaitRoot(Edge edge, RuleChainId ruleChainId) {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                ruleChainId.equals(doGet("/api/edge/" + edge.getId().getId(), Edge.class).getRootRuleChainId()));
    }

    private void awaitAssigned(Edge edge, RuleChainId ruleChainId, boolean expected) {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            PageData<RuleChain> chains = doGetTypedWithPageLink("/api/edge/" + edge.getId().getId() + "/ruleChains?",
                    new TypeReference<>() {}, new PageLink(100));
            return chains.getData().stream().anyMatch(rc -> rc.getId().equals(ruleChainId)) == expected;
        });
    }

    protected EdgeProfile createProfile(String name) throws Exception {
        EdgeProfile profile = new EdgeProfile();
        profile.setName(name);
        return doPost("/api/edgeProfile", profile, EdgeProfile.class);
    }

    protected EdgeProfile createRootedProfile(String name) throws Exception {
        RuleChain rc = createEdgeRuleChain(name + " RC");
        EdgeProfile profile = new EdgeProfile();
        profile.setName(name);
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        return doPost("/api/edgeProfile", profile, EdgeProfile.class);
    }

    protected RuleChain createEdgeRuleChain(String name) throws Exception {
        RuleChain rc = new RuleChain();
        rc.setName(name);
        rc.setType(RuleChainType.EDGE);
        return doPost("/api/ruleChain", rc, RuleChain.class);
    }
}
