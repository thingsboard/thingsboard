// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.edge.BaseEdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@DaoSqlTest
public class EdgeProfileServiceTest extends AbstractServiceTest {

    @Getter
    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private EdgeService edgeService;

    @Test
    public void testSaveFindDelete() {
        EdgeProfile profile = edgeProfileService.saveEdgeProfile(newProfile("Retail Store"));
        Assert.assertNotNull(profile.getId());

        EdgeProfile found = edgeProfileService.findEdgeProfileById(tenantId, profile.getId());
        Assert.assertEquals("Retail Store", found.getName());
        Assert.assertNotNull(found.getProfileData());
        Assert.assertTrue(found.getProfileData().getRuleChainIds().isEmpty());

        edgeProfileService.deleteEdgeProfile(tenantId, profile.getId());
        Assert.assertNull(edgeProfileService.findEdgeProfileById(tenantId, profile.getId()));
    }

    @Test
    public void testRenameRestampsEdgeTypeAndEvictsOldNameFromCache() {
        RuleChain root = saveEdgeRuleChain("Before Rename RC");
        EdgeProfile profile = newProfile("Before Rename");
        profile.getProfileData().getRuleChainIds().add(root.getId());
        profile.setRootRuleChainId(root.getId());
        profile = edgeProfileService.saveEdgeProfile(profile);
        Edge edge = constructEdge(tenantId, "Edge on renamed profile", "Before Rename");
        edge.setEdgeProfileId(profile.getId());
        Edge savedEdge = edgeService.saveEdge(edge);
        Assert.assertEquals("Before Rename", savedEdge.getType());
        // warm the by-name cache so the eviction below is actually exercised
        Assert.assertNotNull(edgeProfileService.findEdgeProfileByName(tenantId, "Before Rename"));

        profile.setName("After Rename");
        edgeProfileService.saveEdgeProfile(profile);

        Assert.assertNotNull(edgeProfileService.findEdgeProfileByName(tenantId, "After Rename"));
        // a stale by-name entry would keep resolving the old name to this profile
        Assert.assertNull(edgeProfileService.findEdgeProfileByName(tenantId, "Before Rename"));
        // edge.type mirrors the profile name, and nothing else keeps it in step after a rename
        Assert.assertEquals("After Rename", edgeService.findEdgeById(tenantId, savedEdge.getId()).getType());

        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testNameUniquePerTenant() {
        edgeProfileService.saveEdgeProfile(newProfile("Dup"));
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(newProfile("Dup")));
    }

    @Test
    public void testTenantGetsDefaultProfileOnCreate() {
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertNotNull(def);
        Assert.assertEquals(BaseEdgeProfileService.DEFAULT_EDGE_PROFILE_NAME, def.getName());
        Assert.assertTrue(def.isDefault());
    }

    @Test
    public void testDefaultProfileTakesTheEdgeRootRuleChainCreatedWithTheTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Tenant with edge root");
        RuleChainId[] rootId = new RuleChainId[1];
        Tenant saved = tenantService.saveTenant(tenant, id -> {
            RuleChain root = new RuleChain();
            root.setTenantId(id);
            root.setName("Edge Root Rule Chain");
            root.setType(RuleChainType.EDGE);
            root.setRoot(true);
            rootId[0] = ruleChainService.saveRuleChain(root).getId();
            entityGroupService.createDefaultTenantEntityGroups(id);
        });
        try {
            EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(saved.getId());
            Assert.assertNotNull(def);
            Assert.assertEquals(rootId[0], def.getRootRuleChainId());
            Assert.assertTrue(def.getProfileData().getRuleChainIds().contains(rootId[0]));
        } finally {
            tenantService.deleteTenant(saved.getId());
        }
    }

    @Test
    public void testSetDefaultSwitchesAndRejectsRootless() {
        RuleChain rc = saveEdgeRuleChain("RC default switch");
        EdgeProfile rooted = newProfile("Rooted");
        rooted.getProfileData().getRuleChainIds().add(rc.getId());
        rooted.setRootRuleChainId(rc.getId());
        rooted = edgeProfileService.saveEdgeProfile(rooted);
        EdgeProfile rootless = edgeProfileService.saveEdgeProfile(newProfile("Rootless"));

        Assert.assertTrue(edgeProfileService.setDefaultEdgeProfile(tenantId, rooted.getId()));
        Assert.assertEquals(rooted.getId(), edgeProfileService.findDefaultEdgeProfile(tenantId).getId());
        Assert.assertFalse(edgeProfileService.findEdgeProfileByName(tenantId, BaseEdgeProfileService.DEFAULT_EDGE_PROFILE_NAME).isDefault());

        EdgeProfileId rootlessId = rootless.getId();
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.setDefaultEdgeProfile(tenantId, rootlessId));
    }

    @Test
    public void testDeleteDefaultProhibited() {
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.deleteEdgeProfile(tenantId, def.getId()));
    }

    @Test
    public void testRootMustBeListedAndCannotBeCleared() {
        RuleChain rc = saveEdgeRuleChain("RC root rules");
        EdgeProfile p = newProfile("Root rules");
        p.setRootRuleChainId(rc.getId());
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(p));

        p.getProfileData().getRuleChainIds().add(rc.getId());
        EdgeProfile saved = edgeProfileService.saveEdgeProfile(p);
        Assert.assertEquals(rc.getId(), saved.getRootRuleChainId());

        saved.setRootRuleChainId(null);
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(saved));

        EdgeProfileId id = saved.getId();
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.removeRuleChain(tenantId, id, rc.getId()));
    }

    @Test
    public void testRejectsCoreRuleChainAndBadGroupTypeAndAllGroup() {
        RuleChain core = new RuleChain();
        core.setTenantId(tenantId);
        core.setName("Core RC");
        core.setType(RuleChainType.CORE);
        core = ruleChainService.saveRuleChain(core);
        EdgeProfile p1 = newProfile("Core ref");
        p1.getProfileData().getRuleChainIds().add(core.getId());
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(p1));

        EntityGroup customers = createEntityGroup(tenantId, EntityType.CUSTOMER, "Customers grp");
        EdgeProfile p2 = newProfile("Bad group type");
        p2.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.CUSTOMER, customers.getId()));
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(p2));

        EntityGroup all = getEntityGroupService().findEntityGroupByTypeAndName(tenantId, tenantId, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME).orElseThrow();
        EdgeProfile p3 = newProfile("All group");
        p3.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, all.getId()));
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(p3));

        EdgeProfile p4 = newProfile("Ghost group");
        p4.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, new EntityGroupId(UUID.randomUUID())));
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(p4));
    }

    @Test
    public void testFindOrCreateCopiesRootFromDefault() {
        RuleChain rc = saveEdgeRuleChain("RC copy");
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        def.getProfileData().getRuleChainIds().add(rc.getId());
        def.setRootRuleChainId(rc.getId());
        edgeProfileService.saveEdgeProfile(def);

        EdgeProfile created = edgeProfileService.findOrCreateEdgeProfile(tenantId, "typeA");
        Assert.assertEquals("typeA", created.getName());
        Assert.assertEquals(rc.getId(), created.getRootRuleChainId());
        Assert.assertTrue(created.getProfileData().getRuleChainIds().contains(rc.getId()));
        Assert.assertFalse(created.isDefault());
        Assert.assertEquals(created.getId(), edgeProfileService.findOrCreateEdgeProfile(tenantId, "typeA").getId());
    }

    @Test
    public void testAddRemoveListsAreIdempotent() {
        RuleChain rc = saveEdgeRuleChain("RC lists");
        EdgeProfile p = edgeProfileService.saveEdgeProfile(newProfile("Lists"));
        p = edgeProfileService.addRuleChain(tenantId, p.getId(), rc.getId());
        p = edgeProfileService.addRuleChain(tenantId, p.getId(), rc.getId());
        Assert.assertEquals(1, p.getProfileData().getRuleChainIds().size());
        p = edgeProfileService.setRootRuleChain(tenantId, p.getId(), rc.getId());
        Assert.assertEquals(rc.getId(), p.getRootRuleChainId());

        EntityGroup grp = createEntityGroup(tenantId, EntityType.DEVICE, "Devices grp");
        p = edgeProfileService.addEntityGroup(tenantId, p.getId(), grp.getId(), EntityType.DEVICE);
        Assert.assertEquals(1, p.getProfileData().getEntityGroups().size());
        p = edgeProfileService.removeEntityGroup(tenantId, p.getId(), grp.getId());
        Assert.assertTrue(p.getProfileData().getEntityGroups().isEmpty());
        p = edgeProfileService.removeEntityGroup(tenantId, p.getId(), grp.getId());
        Assert.assertTrue(p.getProfileData().getEntityGroups().isEmpty());
    }

    @Test
    public void testEntityGroupMustBelongToSameTenant() {
        Tenant otherTenant = new Tenant();
        otherTenant.setTitle("Other tenant");
        otherTenant = tenantService.saveTenant(otherTenant);
        TenantId otherTenantId = otherTenant.getId();

        EntityGroup otherTenantGroup = createEntityGroup(otherTenantId, EntityType.DEVICE, "Other tenant devices");
        EdgeProfile p = newProfile("Cross tenant group");
        p.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, otherTenantGroup.getId()));
        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.saveEdgeProfile(p));

        tenantService.deleteTenant(otherTenantId);
    }

    @Test
    public void testDeletedReferencedEntityIsStrippedFromProfiles() {
        RuleChain root = saveEdgeRuleChain("RC root keep");
        RuleChain extra = saveEdgeRuleChain("RC extra");
        EdgeProfile p = newProfile("Cleanup");
        p.getProfileData().getRuleChainIds().add(root.getId());
        p.getProfileData().getRuleChainIds().add(extra.getId());
        p.setRootRuleChainId(root.getId());
        p = edgeProfileService.saveEdgeProfile(p);

        ruleChainService.deleteRuleChainById(tenantId, extra.getId());
        EdgeProfile after = edgeProfileService.findEdgeProfileById(tenantId, p.getId());
        Assert.assertEquals(1, after.getProfileData().getRuleChainIds().size());
        Assert.assertEquals(root.getId(), after.getProfileData().getRuleChainIds().get(0));

        Assertions.assertThrows(DataValidationException.class, () -> ruleChainService.deleteRuleChainById(tenantId, root.getId()));
    }

    @Test
    public void testConcurrentFindOrCreateReturnsOneProfileInsteadOfFailing() throws Exception {
        // saving several edges of a brand-new type at once makes every thread miss the lookup and try to create the
        // profile; the unique constraint lets one through and the rest must adopt it rather than fail the edge save
        int threads = 8;
        CyclicBarrier startTogether = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<EdgeProfile>> racers = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                racers.add(() -> {
                    startTogether.await();
                    return edgeProfileService.findOrCreateEdgeProfile(tenantId, "Raced profile");
                });
            }
            List<EdgeProfileId> ids = new ArrayList<>();
            for (Future<EdgeProfile> future : pool.invokeAll(racers)) {
                // an unhandled constraint violation surfaces here, which is what a losing edge save used to get
                ids.add(future.get().getId());
            }
            Assert.assertEquals("every caller must end up on the same profile", 1, Set.copyOf(ids).size());
        } finally {
            pool.shutdownNow();
        }
        Assert.assertNotNull(edgeProfileService.findEdgeProfileByName(tenantId, "Raced profile"));
    }

    @Test
    public void testDeleteProhibitedWhileAppProfileLinksIt() {
        RuleChain rc = saveEdgeRuleChain("RC linked by app profile");
        EdgeProfile profile = newProfile("Linked by app profile");
        profile.getProfileData().getRuleChainIds().add(rc.getId());
        profile.setRootRuleChainId(rc.getId());
        EdgeProfileId profileId = edgeProfileService.saveEdgeProfile(profile).getId();

        AgentAppProfile appProfile = new AgentAppProfile();
        appProfile.setTenantId(tenantId);
        appProfile.setName("Edge app profile");
        appProfile.setAppType(AgentApplicationType.EDGE);
        appProfile.setTemplateVersion("1.0.0");
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(edgeCompose());
        appProfile.setConfig(config);
        AgentAppProfileId appProfileId = agentAppProfileService.saveProfile(appProfile).getId();
        agentAppProfileService.setEdgeProfile(tenantId, appProfileId, profileId);

        Assertions.assertThrows(DataValidationException.class, () -> edgeProfileService.deleteEdgeProfile(tenantId, profileId));

        agentAppProfileService.setEdgeProfile(tenantId, appProfileId, null);
        edgeProfileService.deleteEdgeProfile(tenantId, profileId);
        Assert.assertNull(edgeProfileService.findEdgeProfileById(tenantId, profileId));
    }

    private ObjectNode edgeCompose() {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("CLOUD_ROUTING_KEY", "placeholder");
        env.put("CLOUD_ROUTING_SECRET", "placeholder");
        env.put("CLOUD_RPC_HOST", "localhost");

        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);

        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);

        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private EdgeProfile newProfile(String name) {
        EdgeProfile profile = new EdgeProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        return profile;
    }

    private RuleChain saveEdgeRuleChain(String name) {
        RuleChain rc = new RuleChain();
        rc.setTenantId(tenantId);
        rc.setName(name);
        rc.setType(RuleChainType.EDGE);
        return ruleChainService.saveRuleChain(rc);
    }

}
