// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.edge.profile.EdgeProfileApplier;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EdgeProfileMigrationServiceTest extends AbstractControllerTest {

    @Autowired
    EdgeProfileMigrationService migrationService;
    @Autowired
    EdgeProfileService edgeProfileService;
    @Autowired
    EdgeService edgeService;
    @Autowired
    RelationService relationService;
    // spied (not just @Autowired) so testCrashBetweenAssignmentAndStampingRollsBackTheChunk can fault-inject
    // a failure into one specific statement; a plain spy delegates to the real bean for every other test/call
    @MockitoSpyBean
    JdbcTemplate jdbcTemplate;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testTenantWithoutTemplateRootStillGetsARootedDefaultProfile() throws Exception {
        // An edge has no root rule chain other than its profile's, so a rootless default would leave this tenant
        // unable to create edges at all — the migration has to provision one rather than leave the field null.
        Edge orphan = doPost("/api/edge", constructEdge("Orphan edge", null), Edge.class);
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, root_rule_chain_id = NULL, type = 'default' WHERE id = ?",
                orphan.getId().getId());
        jdbcTemplate.update("UPDATE rule_chain SET root = false WHERE tenant_id = ? AND type = 'EDGE'", tenantId.getId());
        deleteEdgeProfiles();

        migrationService.ensureEdgeProfiles(tenantId);

        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertNotNull(migratedDefault);
        Assert.assertNotNull("default edge profile must end up with a root rule chain", migratedDefault.getRootRuleChainId());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, orphan.getId()).getEdgeProfileId());
        // and the invariant now actually holds: a new edge can be created on this tenant
        doPost("/api/edge", constructEdge("Edge after migration", null), Edge.class);
    }

    @Test
    public void testDefaultTypeEdgesShareTheDefaultProfileKeepingTheirRootAndFoldAutoAssignChains() throws Exception {
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        UUID templateRoot = def.getRootRuleChainId().getId();
        RuleChain otherRoot = saveEdgeRuleChain("Other Root");
        RuleChain autoAssign = saveEdgeRuleChain("Auto assigned");

        Edge e1 = doPost("/api/edge", constructEdge("Mig edge 1", null), Edge.class);
        Edge e2 = doPost("/api/edge", constructEdge("Mig edge 2", null), Edge.class);
        Edge e3 = doPost("/api/edge", constructEdge("Mig edge 3", null), Edge.class);

        // simulate the pre-upgrade state
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        // e3 runs a root of its own, assigned to it the way the edge page does it
        jdbcTemplate.update("UPDATE edge SET root_rule_chain_id = ? WHERE id = ?", otherRoot.getId().getId(), e3.getId().getId());
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'EDGE', ?, 'RULE_CHAIN', 'EDGE', 'Contains', NULL, 1) ON CONFLICT DO NOTHING",
                e3.getId().getId(), otherRoot.getId().getId());
        deleteEdgeProfiles();
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'TENANT', ?, 'RULE_CHAIN', 'EDGE_AUTO_ASSIGN_RULE_CHAIN', 'Contains', NULL, 1)",
                tenantId.getId(), autoAssign.getId().getId());
        jdbcTemplate.update("UPDATE relation SET additional_info = NULL WHERE from_type = 'EDGE' AND relation_type_group = 'EDGE' AND to_type = 'RULE_CHAIN'");

        migrationService.ensureEdgeProfiles(tenantId);

        PageData<EdgeProfile> profiles = edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100));
        Assert.assertEquals(1, profiles.getTotalElements());
        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertEquals("default", migratedDefault.getName());
        Assert.assertEquals(templateRoot, migratedDefault.getRootRuleChainId().getId());
        Assert.assertTrue(migratedDefault.getProfileData().getRuleChainIds().contains(autoAssign.getId()));

        for (Edge edge : List.of(e1, e2, e3)) {
            Edge migrated = edgeService.findEdgeById(tenantId, edge.getId());
            Assert.assertEquals(migratedDefault.getId(), migrated.getEdgeProfileId());
            Assert.assertEquals("default", migrated.getType());
        }
        // e3 keeps its root as an override; the profile root reaches it as an ordinary chain, its own root stays hand-made
        Assert.assertEquals(otherRoot.getId(), edgeService.findEdgeById(tenantId, e3.getId()).getRootRuleChainId());
        Assert.assertEquals(Integer.valueOf(1), stampedRelations(e3, templateRoot, migratedDefault));
        Assert.assertEquals(Integer.valueOf(1), edgeRuleChainRelations(e3, otherRoot));
        Assert.assertEquals(Integer.valueOf(0), stampedRelations(e3, otherRoot.getId().getId(), migratedDefault));

        Integer stamped = jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND to_id = ? AND additional_info LIKE ?",
                Integer.class, e1.getId().getId(), templateRoot, "%" + migratedDefault.getId().getId() + "%");
        Assert.assertEquals(Integer.valueOf(1), stamped);
        Integer autoAssignRows = jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND relation_type_group = 'EDGE_AUTO_ASSIGN_RULE_CHAIN'",
                Integer.class, tenantId.getId());
        Assert.assertEquals(Integer.valueOf(0), autoAssignRows);

        migrationService.ensureEdgeProfiles(tenantId);
        Assert.assertEquals(1, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
    }

    @Test
    public void testAutoAssignChainsAreAssignedToPreExistingEdges() throws Exception {
        RuleChain autoAssign = saveEdgeRuleChain("Auto assigned later");
        Edge edge = doPost("/api/edge", constructEdge("Pre-existing edge", null), Edge.class);

        // pre-upgrade state: the chain was marked auto-assign after the edge was created, so the edge never received it
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        deleteEdgeProfiles();
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'TENANT', ?, 'RULE_CHAIN', 'EDGE_AUTO_ASSIGN_RULE_CHAIN', 'Contains', NULL, 1)",
                tenantId.getId(), autoAssign.getId().getId());
        jdbcTemplate.update("UPDATE relation SET additional_info = NULL WHERE from_type = 'EDGE' AND relation_type_group = 'EDGE' AND to_type = 'RULE_CHAIN'");
        Assert.assertEquals(Integer.valueOf(0), edgeRuleChainRelations(edge, autoAssign));

        migrationService.ensureEdgeProfiles(tenantId);

        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertTrue(migratedDefault.getProfileData().getRuleChainIds().contains(autoAssign.getId()));
        Integer stamped = jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND to_id = ? AND relation_type_group = 'EDGE' AND additional_info LIKE ?",
                Integer.class, edge.getId().getId(), autoAssign.getId().getId(), "%" + migratedDefault.getId().getId() + "%");
        Assert.assertEquals(Integer.valueOf(1), stamped);

        migrationService.ensureEdgeProfiles(tenantId);
        Assert.assertEquals(Integer.valueOf(1), edgeRuleChainRelations(edge, autoAssign));
    }

    @Test
    public void testMigrationEvictsEdgesCachedByName() throws Exception {
        Edge edge = doPost("/api/edge", constructEdge("Cached edge", null), Edge.class);

        // pre-upgrade state under a name no cache entry exists for yet, then warm the by-name cache with that legacy row
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        jdbcTemplate.update("UPDATE edge SET type = 'legacy', name = 'Legacy cached edge' WHERE id = ?", edge.getId().getId());
        deleteEdgeProfiles();
        Assert.assertNull(edgeService.findEdgeByTenantIdAndName(tenantId, "Legacy cached edge").getEdgeProfileId());

        migrationService.ensureEdgeProfiles(tenantId);

        Edge migrated = edgeService.findEdgeByTenantIdAndName(tenantId, "Legacy cached edge");
        EdgeProfile legacy = edgeProfileService.findEdgeProfileByName(tenantId, "legacy");
        Assert.assertNotNull(legacy);
        Assert.assertEquals(legacy.getId(), migrated.getEdgeProfileId());
        Assert.assertEquals("legacy", migrated.getType());
    }

    @Test
    public void testMigrationEvictsTheRelationsItStampsWithRawSql() throws Exception {
        RuleChain autoAssign = saveEdgeRuleChain("Raw stamped chain");
        Edge edge = doPost("/api/edge", constructEdge("Relation cached edge", null), Edge.class);

        // pre-upgrade state: the chain is auto-assign and the edge already carries it unstamped, which is exactly
        // the shape stampRuleChainRelations fixes up with a raw UPDATE rather than through the relation DAO
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        deleteEdgeProfiles();
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'TENANT', ?, 'RULE_CHAIN', 'EDGE_AUTO_ASSIGN_RULE_CHAIN', 'Contains', NULL, 1)",
                tenantId.getId(), autoAssign.getId().getId());
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'EDGE', ?, 'RULE_CHAIN', 'EDGE', 'Contains', NULL, 1) ON CONFLICT DO NOTHING",
                edge.getId().getId(), autoAssign.getId().getId());

        // warm the relation cache with the unstamped row, which is what a node still serving traffic does during
        // a no-downtime upgrade -- the migration's applyAfterCommit runs against exactly such a node
        EntityRelation before = relationService.getRelation(tenantId, edge.getId(), autoAssign.getId(),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        Assert.assertNotNull(before);
        Assert.assertFalse("precondition: the cached copy is unstamped", EdgeProfileApplier.isStamped(before));

        migrationService.ensureEdgeProfiles(tenantId);

        // read back through the cache, not the DB: a raw UPDATE that evicts nothing leaves the applier seeing the
        // stale unstamped copy for the whole cache TTL and skipping the row as not-managed-by-this-profile
        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        EntityRelation after = relationService.getRelation(tenantId, edge.getId(), autoAssign.getId(),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        Assert.assertNotNull(after);
        Assert.assertTrue("the relation cache must not keep serving the pre-migration copy",
                EdgeProfileApplier.isStampedBy(after, migratedDefault.getId()));
    }

    @Test
    public void testNonDefaultTypeBecomesProfileNamedAfterType() throws Exception {
        UUID templateRoot = edgeProfileService.findDefaultEdgeProfile(tenantId).getRootRuleChainId().getId();
        RuleChain storeRoot = saveEdgeRuleChain("Store Root");

        Edge plain = doPost("/api/edge", constructEdge("Typed edge default", null), Edge.class);
        Edge gw1 = doPost("/api/edge", constructEdge("Typed edge gateway 1", null), Edge.class);
        Edge gw2 = doPost("/api/edge", constructEdge("Typed edge gateway 2", null), Edge.class);
        Edge st1 = doPost("/api/edge", constructEdge("Typed edge store 1", null), Edge.class);
        Edge st2 = doPost("/api/edge", constructEdge("Typed edge store 2", null), Edge.class);

        // pre-upgrade state: the type is free text, and the gateways never left the template root
        legacyState(plain, "default", templateRoot);
        legacyState(gw1, "gateway", templateRoot);
        legacyState(gw2, "gateway", templateRoot);
        legacyState(st1, "store", storeRoot.getId().getId());
        legacyState(st2, "store", storeRoot.getId().getId());
        deleteEdgeProfiles();
        jdbcTemplate.update("UPDATE relation SET additional_info = NULL WHERE from_type = 'EDGE' AND relation_type_group = 'EDGE' AND to_type = 'RULE_CHAIN'");

        migrationService.ensureEdgeProfiles(tenantId);

        Assert.assertEquals(3, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        EdgeProfile gateway = edgeProfileService.findEdgeProfileByName(tenantId, "gateway");
        EdgeProfile store = edgeProfileService.findEdgeProfileByName(tenantId, "store");
        Assert.assertNotNull(gateway);
        Assert.assertNotNull(store);
        Assert.assertFalse(gateway.isDefault());
        Assert.assertEquals(templateRoot, gateway.getRootRuleChainId().getId());
        Assert.assertEquals(storeRoot.getId(), store.getRootRuleChainId());

        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, plain.getId()).getEdgeProfileId());
        for (Edge gw : List.of(gw1, gw2)) {
            Edge migrated = edgeService.findEdgeById(tenantId, gw.getId());
            Assert.assertEquals(gateway.getId(), migrated.getEdgeProfileId());
            Assert.assertEquals("gateway", migrated.getType());
            Assert.assertEquals(templateRoot, migrated.getRootRuleChainId().getId());
        }
        for (Edge st : List.of(st1, st2)) {
            Edge migrated = edgeService.findEdgeById(tenantId, st.getId());
            Assert.assertEquals(store.getId(), migrated.getEdgeProfileId());
            Assert.assertEquals("store", migrated.getType());
            Assert.assertEquals(storeRoot.getId(), migrated.getRootRuleChainId());
        }
        // two profiles share the template root: each edge's relation must carry the stamp of its own profile
        Assert.assertEquals(Integer.valueOf(1), stampedRelations(gw1, templateRoot, gateway));
        Assert.assertEquals(Integer.valueOf(0), stampedRelations(gw1, templateRoot, migratedDefault));
        Assert.assertEquals(Integer.valueOf(1), stampedRelations(plain, templateRoot, migratedDefault));
    }

    @Test
    public void testMixedRootsWithinTypeShareOneProfileOnTheMostUsedRoot() throws Exception {
        RuleChain rootA = saveEdgeRuleChain("Gateway Root A");
        RuleChain rootB = saveEdgeRuleChain("Gateway Root B");
        Edge a1 = doPost("/api/edge", constructEdge("Mixed edge a1", null), Edge.class);
        Edge a2 = doPost("/api/edge", constructEdge("Mixed edge a2", null), Edge.class);
        Edge b1 = doPost("/api/edge", constructEdge("Mixed edge b1", null), Edge.class);
        Edge noRoot = doPost("/api/edge", constructEdge("Mixed edge no root", null), Edge.class);
        Edge tieA = doPost("/api/edge", constructEdge("Tie edge a", null), Edge.class);
        Edge tieB = doPost("/api/edge", constructEdge("Tie edge b", null), Edge.class);

        // pre-upgrade state: one type over two roots, plus an edge with no root at all
        legacyState(a1, "gateway", rootA.getId().getId());
        legacyState(a2, "gateway", rootA.getId().getId());
        legacyState(b1, "gateway", rootB.getId().getId());
        legacyState(noRoot, "gateway", null);
        legacyState(tieA, "tie", rootA.getId().getId());
        legacyState(tieB, "tie", rootB.getId().getId());
        deleteEdgeProfiles();

        migrationService.ensureEdgeProfiles(tenantId);

        // no edge changes its type: the type keeps one profile, on the root most of its edges run
        Assert.assertEquals(3, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
        EdgeProfile gateway = edgeProfileService.findEdgeProfileByName(tenantId, "gateway");
        Assert.assertNotNull(gateway);
        Assert.assertEquals(rootA.getId(), gateway.getRootRuleChainId());
        for (Edge edge : List.of(a1, a2, b1, noRoot)) {
            Edge migrated = edgeService.findEdgeById(tenantId, edge.getId());
            Assert.assertEquals(gateway.getId(), migrated.getEdgeProfileId());
            Assert.assertEquals("gateway", migrated.getType());
        }
        Assert.assertEquals(rootA.getId(), edgeService.findEdgeById(tenantId, a1.getId()).getRootRuleChainId());
        // the minority root stays on its edge as an override, and the edge without a root takes the profile's
        Assert.assertEquals(rootB.getId(), edgeService.findEdgeById(tenantId, b1.getId()).getRootRuleChainId());
        Assert.assertEquals(rootA.getId(), edgeService.findEdgeById(tenantId, noRoot.getId()).getRootRuleChainId());
        Assert.assertEquals(Integer.valueOf(1), stampedRelations(b1, rootA.getId().getId(), gateway));

        // a tie picks one of the roots, and each edge still runs its own
        EdgeProfile tie = edgeProfileService.findEdgeProfileByName(tenantId, "tie");
        Assert.assertNotNull(tie);
        Assert.assertTrue(Set.of(rootA.getId(), rootB.getId()).contains(tie.getRootRuleChainId()));
        Assert.assertEquals(rootA.getId(), edgeService.findEdgeById(tenantId, tieA.getId()).getRootRuleChainId());
        Assert.assertEquals(rootB.getId(), edgeService.findEdgeById(tenantId, tieB.getId()).getRootRuleChainId());
    }

    @Test
    public void testNonDefaultTypeWithUnusableRootTakesTheTemplateRoot() throws Exception {
        UUID templateRoot = edgeProfileService.findDefaultEdgeProfile(tenantId).getRootRuleChainId().getId();
        RuleChain deletedRoot = saveEdgeRuleChain("Doomed Root");
        Edge dangling = doPost("/api/edge", constructEdge("Doomed edge dangling", null), Edge.class);
        Edge nullRoot = doPost("/api/edge", constructEdge("Doomed edge null", null), Edge.class);

        legacyState(dangling, "doomed", deletedRoot.getId().getId());
        legacyState(nullRoot, "doomed", null);
        deleteEdgeProfiles();
        doDelete("/api/ruleChain/" + deletedRoot.getId().getId()).andExpect(status().isOk());

        migrationService.ensureEdgeProfiles(tenantId);

        Assert.assertEquals(2, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
        EdgeProfile doomed = edgeProfileService.findEdgeProfileByName(tenantId, "doomed");
        Assert.assertNotNull(doomed);
        Assert.assertEquals(templateRoot, doomed.getRootRuleChainId().getId());
        for (Edge edge : List.of(dangling, nullRoot)) {
            Edge migrated = edgeService.findEdgeById(tenantId, edge.getId());
            Assert.assertEquals(doomed.getId(), migrated.getEdgeProfileId());
            Assert.assertEquals(templateRoot, migrated.getRootRuleChainId().getId());
            Assert.assertEquals("doomed", migrated.getType());
        }
    }

    @Test
    public void testResumeReusesTheTypeProfileWhateverItsRoot() throws Exception {
        RuleChain rootA = saveEdgeRuleChain("Resume Root A");
        RuleChain rootB = saveEdgeRuleChain("Resume Root B");
        Edge done = doPost("/api/edge", constructEdge("Resume edge done", null), Edge.class);
        Edge sameRoot = doPost("/api/edge", constructEdge("Resume edge same root", null), Edge.class);
        Edge otherRoot1 = doPost("/api/edge", constructEdge("Resume edge other root 1", null), Edge.class);
        Edge otherRoot2 = doPost("/api/edge", constructEdge("Resume edge other root 2", null), Edge.class);
        legacyState(done, "gateway", rootA.getId().getId());
        legacyState(sameRoot, "gateway", rootA.getId().getId());
        legacyState(otherRoot1, "gateway", rootB.getId().getId());
        legacyState(otherRoot2, "gateway", rootB.getId().getId());
        deleteEdgeProfiles();

        // a first attempt committed the "gateway" profile with one of its edges, then crashed before the other three
        EdgeProfile partial = new EdgeProfile();
        partial.setTenantId(tenantId);
        partial.setName("gateway");
        partial.setRootRuleChainId(rootA.getId());
        partial.getProfileData().setRuleChainIds(List.of(rootA.getId()));
        UUID partialId = edgeProfileService.saveEdgeProfile(partial, false).getId().getId();
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = ? WHERE id = ?", partialId, done.getId().getId());

        migrationService.ensureEdgeProfiles(tenantId);

        // root B now leads among the edges left, yet the type's profile is reused rather than a second one created
        Assert.assertEquals(2, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
        Assert.assertEquals(rootA.getId(), edgeProfileService.findEdgeProfileById(tenantId, new EdgeProfileId(partialId)).getRootRuleChainId());
        for (Edge edge : List.of(done, sameRoot, otherRoot1, otherRoot2)) {
            Edge migrated = edgeService.findEdgeById(tenantId, edge.getId());
            Assert.assertEquals(partialId, migrated.getEdgeProfileId().getId());
            Assert.assertEquals("gateway", migrated.getType());
        }
        Assert.assertEquals(rootB.getId(), edgeService.findEdgeById(tenantId, otherRoot1.getId()).getRootRuleChainId());
        Assert.assertEquals(rootB.getId(), edgeService.findEdgeById(tenantId, otherRoot2.getId()).getRootRuleChainId());
    }

    @Test
    public void testTenantWithoutEdgesGetsDefault() {
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL WHERE tenant_id = ?", tenantId.getId());
        deleteEdgeProfiles();
        migrationService.ensureEdgeProfiles(tenantId);
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertNotNull(def);
        Assert.assertEquals("default", def.getName());
        Assert.assertNotNull(def.getRootRuleChainId());
    }

    @Test
    public void testNullRootEdgeGoesToDefaultProfileAndStaysIdempotent() throws Exception {
        RuleChain otherRoot = saveEdgeRuleChain("Null Root Other");

        Edge eDefault = doPost("/api/edge", constructEdge("Null root edge default", null), Edge.class);
        Edge eOther = doPost("/api/edge", constructEdge("Null root edge other", null), Edge.class);
        Edge eNullRoot = doPost("/api/edge", constructEdge("Null root edge null", null), Edge.class);

        // simulate the pre-upgrade state, including a legacy edge with no root rule chain at all
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        jdbcTemplate.update("UPDATE edge SET root_rule_chain_id = ? WHERE id = ?", otherRoot.getId().getId(), eOther.getId().getId());
        jdbcTemplate.update("UPDATE edge SET root_rule_chain_id = NULL WHERE id = ?", eNullRoot.getId().getId());
        deleteEdgeProfiles();

        migrationService.ensureEdgeProfiles(tenantId);

        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);

        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eDefault.getId()).getEdgeProfileId());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eOther.getId()).getEdgeProfileId());
        Assert.assertEquals(otherRoot.getId(), edgeService.findEdgeById(tenantId, eOther.getId()).getRootRuleChainId());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eNullRoot.getId()).getEdgeProfileId());
        Assert.assertEquals("default", edgeService.findEdgeById(tenantId, eNullRoot.getId()).getType());
        Assert.assertEquals(migratedDefault.getRootRuleChainId(), edgeService.findEdgeById(tenantId, eNullRoot.getId()).getRootRuleChainId());

        migrationService.ensureEdgeProfiles(tenantId);
        Assert.assertEquals(1, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eNullRoot.getId()).getEdgeProfileId());
    }

    @Test
    public void testDanglingRootAndStaleAutoAssignFoldIntoDefaultProfile() throws Exception {
        RuleChain deletedRoot = saveEdgeRuleChain("Dangling Root");
        RuleChain deletedAutoAssign = saveEdgeRuleChain("Dangling Auto Assign");

        Edge eDefault = doPost("/api/edge", constructEdge("Dangling edge default", null), Edge.class);
        Edge eDangling = doPost("/api/edge", constructEdge("Dangling edge root", null), Edge.class);

        // simulate the pre-upgrade state: one edge's root and one auto-assign relation point at chains that get deleted below
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        jdbcTemplate.update("UPDATE edge SET root_rule_chain_id = ? WHERE id = ?", deletedRoot.getId().getId(), eDangling.getId().getId());
        deleteEdgeProfiles();

        // neither pointer is backed by a relation the deletion guard checks, so both chains can be deleted while still referenced
        doDelete("/api/ruleChain/" + deletedRoot.getId().getId()).andExpect(status().isOk());
        doDelete("/api/ruleChain/" + deletedAutoAssign.getId().getId()).andExpect(status().isOk());

        // after the delete, on purpose: deleting a rule chain also removes its inbound relations, so inserting this row
        // first would leave nothing for the migration to filter and the assertion below could not fail
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'TENANT', ?, 'RULE_CHAIN', 'EDGE_AUTO_ASSIGN_RULE_CHAIN', 'Contains', NULL, 1)",
                tenantId.getId(), deletedAutoAssign.getId().getId());

        migrationService.ensureEdgeProfiles(tenantId);

        PageData<EdgeProfile> profiles = edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100));
        Assert.assertEquals(1, profiles.getTotalElements());
        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertNotNull(migratedDefault);
        Assert.assertFalse(migratedDefault.getProfileData().getRuleChainIds().contains(deletedAutoAssign.getId()));
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eDefault.getId()).getEdgeProfileId());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eDangling.getId()).getEdgeProfileId());
        Assert.assertEquals(migratedDefault.getRootRuleChainId(), edgeService.findEdgeById(tenantId, eDangling.getId()).getRootRuleChainId());

        migrationService.ensureEdgeProfiles(tenantId);
        Assert.assertEquals(1, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eDangling.getId()).getEdgeProfileId());
    }

    @Test
    public void testDanglingRootWithoutTemplateRootStillGetsAProfile() throws Exception {
        RuleChain deletedRoot = saveEdgeRuleChain("Orphan Root");
        Edge eDangling = doPost("/api/edge", constructEdge("Orphan root edge", null), Edge.class);

        // pre-upgrade tenant with no edge template root at all and one edge whose root chain gets deleted below
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        jdbcTemplate.update("UPDATE edge SET root_rule_chain_id = ? WHERE id = ?", deletedRoot.getId().getId(), eDangling.getId().getId());
        jdbcTemplate.update("UPDATE rule_chain SET root = false WHERE tenant_id = ? AND type = 'EDGE'", tenantId.getId());
        deleteEdgeProfiles();
        doDelete("/api/ruleChain/" + deletedRoot.getId().getId()).andExpect(status().isOk());

        migrationService.ensureEdgeProfiles(tenantId);

        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertNotNull(migratedDefault);
        Assert.assertNotNull(migratedDefault.getRootRuleChainId());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eDangling.getId()).getEdgeProfileId());
        Assert.assertEquals(migratedDefault.getRootRuleChainId(), edgeService.findEdgeById(tenantId, eDangling.getId()).getRootRuleChainId());

        migrationService.ensureEdgeProfiles(tenantId);
        Assert.assertEquals(1, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
    }

    @Test
    public void testResumesAfterPartialMigrationLeavesEdgesUnassigned() throws Exception {
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        UUID templateRoot = def.getRootRuleChainId().getId();
        RuleChain otherRoot = saveEdgeRuleChain("Partial Other Root");
        RuleChain autoAssign = saveEdgeRuleChain("Partial Auto assigned");

        Edge eDefault = doPost("/api/edge", constructEdge("Partial edge default", null), Edge.class);
        Edge eOther = doPost("/api/edge", constructEdge("Partial edge other", null), Edge.class);

        // simulate the pre-upgrade state
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        jdbcTemplate.update("UPDATE edge SET root_rule_chain_id = ? WHERE id = ?", otherRoot.getId().getId(), eOther.getId().getId());
        deleteEdgeProfiles();
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'TENANT', ?, 'RULE_CHAIN', 'EDGE_AUTO_ASSIGN_RULE_CHAIN', 'Contains', NULL, 1)",
                tenantId.getId(), autoAssign.getId().getId());

        // simulate a first migration attempt that crashed right after committing the default profile,
        // before ever assigning an edge
        EdgeProfile partialDefault = new EdgeProfile();
        partialDefault.setTenantId(tenantId);
        partialDefault.setName("default");
        partialDefault.setDefault(true);
        partialDefault.setRootRuleChainId(new RuleChainId(templateRoot));
        partialDefault.getProfileData().setRuleChainIds(List.of(new RuleChainId(templateRoot)));
        UUID partialDefaultId = edgeProfileService.saveEdgeProfile(partialDefault, false).getId().getId();

        // the old guard ("a profile exists") would treat this tenant as already migrated and skip it forever;
        // the new guard must see the unassigned edges and resume the work
        migrationService.ensureEdgeProfiles(tenantId);

        PageData<EdgeProfile> profiles = edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100));
        Assert.assertEquals(1, profiles.getTotalElements());
        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertEquals("the pre-existing default profile must be reused, not duplicated", partialDefaultId, migratedDefault.getId().getId());
        // the auto-assign relations are deleted below, so the reused profile must have picked the chain up like a new one
        Assert.assertTrue("auto-assign chain folded into the reused default profile", migratedDefault.getProfileData().getRuleChainIds().contains(autoAssign.getId()));

        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eDefault.getId()).getEdgeProfileId());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eOther.getId()).getEdgeProfileId());
        Assert.assertEquals(otherRoot.getId(), edgeService.findEdgeById(tenantId, eOther.getId()).getRootRuleChainId());

        Integer autoAssignRows = jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND relation_type_group = 'EDGE_AUTO_ASSIGN_RULE_CHAIN'",
                Integer.class, tenantId.getId());
        Assert.assertEquals(Integer.valueOf(0), autoAssignRows);

        // a further run must be a true no-op: no new/duplicate profiles, assignments unchanged
        migrationService.ensureEdgeProfiles(tenantId);
        Assert.assertEquals(1, edgeProfileService.findEdgeProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eDefault.getId()).getEdgeProfileId());
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, eOther.getId()).getEdgeProfileId());
    }

    @Test
    public void testCrashBetweenAssignmentAndStampingRollsBackTheChunk() throws Exception {
        EdgeProfile def = edgeProfileService.findDefaultEdgeProfile(tenantId);
        UUID templateRoot = def.getRootRuleChainId().getId();

        Edge edge = doPost("/api/edge", constructEdge("Crash test edge", null), Edge.class);

        // simulate the pre-upgrade state
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        deleteEdgeProfiles();
        jdbcTemplate.update("UPDATE relation SET additional_info = NULL WHERE from_type = 'EDGE' AND relation_type_group = 'EDGE' AND to_type = 'RULE_CHAIN'");

        // fault-inject a failure into the relation-stamping statement, i.e. the point right after assignEdgesToProfile
        // has already run inside the same chunk; fires once, then the spy behaves like the real bean. The statement
        // is a queryForList because it stamps with UPDATE ... RETURNING, so the stamped rows can be evicted.
        AtomicBoolean armed = new AtomicBoolean(true);
        Mockito.doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("UPDATE relation SET additional_info") && armed.getAndSet(false)) {
                throw new RuntimeException("simulated crash between edge assignment and relation stamping");
            }
            return invocation.callRealMethod();
        }).when(jdbcTemplate).queryForList(Mockito.anyString(), Mockito.any(Class.class), Mockito.any(Object[].class));

        try {
            migrationService.ensureEdgeProfiles(tenantId);
            Assert.fail("expected the simulated crash to propagate");
        } catch (RuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("simulated crash"));
        }

        // the chunk's @Transactional boundary must have rolled back the edge assignment along with the
        // never-reached stamp: the edge is fully unassigned again, never left "assigned but unstamped"
        Assert.assertNull(edgeService.findEdgeById(tenantId, edge.getId()).getEdgeProfileId());

        // the guard must see the still-unassigned edge and resume, rather than treating the tenant as done
        migrationService.ensureEdgeProfiles(tenantId);

        EdgeProfile migratedDefault = edgeProfileService.findDefaultEdgeProfile(tenantId);
        Assert.assertNotNull(migratedDefault);
        Assert.assertEquals(migratedDefault.getId(), edgeService.findEdgeById(tenantId, edge.getId()).getEdgeProfileId());
        Integer stamped = jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND to_id = ? AND additional_info LIKE ?",
                Integer.class, edge.getId().getId(), templateRoot, "%" + migratedDefault.getId().getId() + "%");
        Assert.assertEquals(Integer.valueOf(1), stamped);
    }

    @Test
    public void testGroupLargerThanOneChunkIsMigratedCompletely() throws Exception {
        Edge template = doPost("/api/edge", constructEdge("Chunk template edge", null), Edge.class);
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'bulk' WHERE tenant_id = ?", tenantId.getId());
        // 400 edges is more than one chunk, so an off-by-one in the chunk loop leaves some of them behind
        jdbcTemplate.update("INSERT INTO edge (id, created_time, customer_id, root_rule_chain_id, type, name, routing_key, secret, " +
                        "edge_license_key, cloud_endpoint, tenant_id, version) " +
                        "SELECT gen_random_uuid(), e.created_time, e.customer_id, e.root_rule_chain_id, e.type, 'Bulk edge ' || g, " +
                        "gen_random_uuid()::text, e.secret, e.edge_license_key, e.cloud_endpoint, e.tenant_id, 1 " +
                        "FROM edge e, generate_series(1, 400) g WHERE e.id = ?",
                template.getId().getId());
        deleteEdgeProfiles();

        migrationService.ensureEdgeProfiles(tenantId);

        Integer unassigned = jdbcTemplate.queryForObject("SELECT count(*) FROM edge WHERE tenant_id = ? AND edge_profile_id IS NULL",
                Integer.class, tenantId.getId());
        Assert.assertEquals(Integer.valueOf(0), unassigned);
        Integer onBulkProfile = jdbcTemplate.queryForObject("SELECT count(*) FROM edge e JOIN edge_profile p ON p.id = e.edge_profile_id " +
                        "WHERE e.tenant_id = ? AND p.name = 'bulk'", Integer.class, tenantId.getId());
        Assert.assertEquals(Integer.valueOf(401), onBulkProfile);
    }

    private RuleChain saveEdgeRuleChain(String name) throws Exception {
        RuleChain rc = new RuleChain();
        rc.setName(name);
        rc.setType(RuleChainType.EDGE);
        return doPost("/api/ruleChain", rc, RuleChain.class);
    }

    // the pre-upgrade row: no profile, a free-text type and whatever root the 4.3 tenant had set, null included
    private void legacyState(Edge edge, String type, UUID root) {
        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = ?, root_rule_chain_id = CAST(? AS uuid) WHERE id = ?",
                type, root, edge.getId().getId());
    }

    private Integer stampedRelations(Edge edge, UUID ruleChainId, EdgeProfile profile) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND to_id = ? AND relation_type_group = 'EDGE' AND additional_info LIKE ?",
                Integer.class, edge.getId().getId(), ruleChainId, "%" + profile.getId().getId() + "%");
    }

    private Integer edgeRuleChainRelations(Edge edge, RuleChain ruleChain) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND to_id = ? AND relation_type_group = 'EDGE'",
                Integer.class, edge.getId().getId(), ruleChain.getId().getId());
    }

    // through the service rather than JDBC, so its cache does not keep serving the deleted profiles to the migration
    private void deleteEdgeProfiles() {
        edgeProfileService.deleteEdgeProfilesByTenantId(tenantId);
    }
}
