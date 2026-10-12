// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.group.BaseEntityGroupService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeProfileApplierTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    EdgeProfileService edgeProfileService;
    @Mock
    EdgeService edgeService;
    @Mock
    RelationService relationService;
    @Mock
    RuleChainService ruleChainService;
    @Mock
    EntityGroupService entityGroupService;
    @Mock
    IntegrationService integrationService;
    @Mock
    SchedulerEventService schedulerEventService;
    @Mock
    OwnersCacheService ownersCacheService;
    @Mock
    EdgeEventService edgeEventService;
    @Mock
    ObjectProvider<RuleChainEdgeProcessor> ruleChainEdgeProcessorProvider;
    @Mock
    RuleChainEdgeProcessor ruleChainEdgeProcessor;

    private EdgeProfileApplier applier;
    private EdgeProfile profile;
    private RuleChainId rc1;
    private RuleChainId rc2;

    @BeforeEach
    void setUp() {
        applier = new EdgeProfileApplier(edgeProfileService, edgeService, relationService, ruleChainService,
                entityGroupService, integrationService, schedulerEventService, ownersCacheService, edgeEventService, ruleChainEdgeProcessorProvider);
        // the executor is built in @PostConstruct, which a plain constructor call does not run
        applier.init();
        lenient().when(edgeEventService.saveAsync(any())).thenReturn(Futures.immediateFuture(null));
        profile = new EdgeProfile(new EdgeProfileId(UUID.randomUUID()));
        profile.setTenantId(TENANT_ID);
        profile.setName("P");
        rc1 = new RuleChainId(UUID.randomUUID());
        rc2 = new RuleChainId(UUID.randomUUID());
        profile.getProfileData().getRuleChainIds().add(rc1);
        profile.getProfileData().getRuleChainIds().add(rc2);
        profile.setRootRuleChainId(rc1);
        // rows are skipped when their entity no longer exists, so every row lookup has to resolve unless a test says otherwise
        lenient().when(ruleChainService.findRuleChainById(any(), any())).thenReturn(new RuleChain());
        lenient().when(integrationService.findIntegrationById(any(), any())).thenReturn(new Integration());
        lenient().when(ruleChainEdgeProcessorProvider.getObject()).thenReturn(ruleChainEdgeProcessor);
        lenient().when(ruleChainEdgeProcessor.pushRootRuleChain(any(), any(), any())).thenReturn(Futures.immediateFuture(null));
    }

    @AfterEach
    void tearDown() {
        applier.shutdown();
    }

    private Edge edge(EdgeId id, EdgeProfileId profileId, RuleChainId root) {
        Edge e = new Edge(id);
        e.setTenantId(TENANT_ID);
        e.setEdgeProfileId(profileId);
        e.setRootRuleChainId(root);
        e.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        return e;
    }

    private void profileEdges(Edge... edges) {
        when(edgeService.findEdgesByTenantIdAndEdgeProfileId(eq(TENANT_ID), eq(profile.getId()), any()))
                .thenReturn(new PageData<>(List.of(edges), 1, edges.length, false));
        // Apply re-reads each edge before touching it
        for (Edge e : edges) {
            lenient().when(edgeService.findEdgeById(TENANT_ID, e.getId())).thenReturn(e);
        }
    }

    @Test
    void applyRowAssignsOnlyMissingEdgesAndTakesOneSnapshot() {
        Edge e1 = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        Edge e2 = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        Edge e3 = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(e1, e2, e3);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.RULE_CHAIN), any()))
                .thenReturn(new PageData<>(List.of(e2.getId()), 1, 1, false));
        // every eligible edge is checked before it is written to, so the edges without the row answer null
        when(relationService.getRelation(eq(TENANT_ID), any(), eq(rc2), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.EDGE)))
                .thenReturn(null);
        when(relationService.getRelation(TENANT_ID, e2.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(e2.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));

        int assigned = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.ruleChain(rc2));

        assertThat(assigned).isEqualTo(2);
        verify(edgeService, times(1)).findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.RULE_CHAIN), any());
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(e1.getId()), any(ObjectNode.class));
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(e3.getId()), any(ObjectNode.class));
        verify(ruleChainService, never()).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(e2.getId()), any());
        // one lookup per eligible edge: the row snapshot alone cannot be trusted to decide whether to write, since
        // a hand-made relation can appear after it is taken. Each read guards the write that follows it, so the
        // extra reads are the ones paired with an assign anyway -- see applyRowLeavesHandMadeRelationCreatedAfterTheSnapshotAlone.
        verify(relationService, times(3)).getRelation(any(), any(), any(), anyString(), any());
    }

    @Test
    void applyRowLeavesHandMadeRelationCreatedAfterTheSnapshotAlone() {
        Edge e1 = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(e1);
        // the snapshot of who carries the row is taken once, before the fan-out walks the edges...
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.RULE_CHAIN), any()))
                .thenReturn(new PageData<>(List.of(), 1, 0, false));
        // ...and by the time it reaches this edge the chain has been assigned by hand, so the relation exists unstamped
        when(relationService.getRelation(TENANT_ID, e1.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(e1.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));

        int assigned = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.ruleChain(rc2));

        // assigning over it would stamp a row the user made, and the profile would later remove it as its own
        assertThat(assigned).isZero();
        verify(ruleChainService, never()).assignRuleChainToEdge(any(), any(), any(), any(ObjectNode.class));
    }

    @Test
    void applyRowSecondRunIsNoop() {
        Edge e1 = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(e1);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.RULE_CHAIN), any()))
                .thenReturn(new PageData<>(List.of(e1.getId()), 1, 1, false));
        when(relationService.getRelation(TENANT_ID, e1.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(e1.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));

        assertThat(applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.ruleChain(rc2))).isZero();
        verify(ruleChainService, never()).assignRuleChainToEdge(any(), any(), any(), any());
    }

    @Test
    void unassignRowTouchesOnlyStampedRelations() {
        Edge stamped = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        Edge manual = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        Edge foreign = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.RULE_CHAIN), any()))
                .thenReturn(new PageData<>(List.of(stamped.getId(), manual.getId(), foreign.getId()), 1, 3, false));
        when(relationService.getRelation(TENANT_ID, stamped.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(stamped.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        when(relationService.getRelation(TENANT_ID, manual.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(manual.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        when(relationService.getRelation(TENANT_ID, foreign.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(foreign.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(new EdgeProfileId(UUID.randomUUID()))));

        int removed = applier.unassignRowFromProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.ruleChain(rc2));

        assertThat(removed).isEqualTo(1);
        verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, rc2, stamped.getId(), false);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(rc2), eq(manual.getId()), eq(false));
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(rc2), eq(foreign.getId()), eq(false));
    }

    @Test
    void unassignRowReachesEdgesThatSwitchedAwayUnlessTheirNewProfileCarriesTheRow() {
        EdgeProfile carrying = new EdgeProfile(new EdgeProfileId(UUID.randomUUID()));
        carrying.getProfileData().getRuleChainIds().add(rc2);
        EdgeProfile other = new EdgeProfile(new EdgeProfileId(UUID.randomUUID()));
        Edge switchedToOther = edge(new EdgeId(UUID.randomUUID()), other.getId(), rc1);
        Edge switchedToCarrying = edge(new EdgeId(UUID.randomUUID()), carrying.getId(), rc1);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.RULE_CHAIN), any()))
                .thenReturn(new PageData<>(List.of(switchedToOther.getId(), switchedToCarrying.getId()), 1, 2, false));
        for (Edge e : List.of(switchedToOther, switchedToCarrying)) {
            when(edgeService.findEdgeById(TENANT_ID, e.getId())).thenReturn(e);
            when(relationService.getRelation(TENANT_ID, e.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                    .thenReturn(new EntityRelation(e.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        }
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, other.getId())).thenReturn(other);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, carrying.getId())).thenReturn(carrying);

        assertThat(applier.unassignRowFromProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.ruleChain(rc2))).isEqualTo(1);
        verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, rc2, switchedToOther.getId(), false);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(rc2), eq(switchedToCarrying.getId()), eq(false));
    }

    @Test
    void setRootMovesFollowersOfThePreviousRootAndKeepsOverrides() throws Exception {
        RuleChainId previousRoot = new RuleChainId(UUID.randomUUID());
        Edge follower = edge(new EdgeId(UUID.randomUUID()), profile.getId(), previousRoot);
        Edge overridden = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc2);
        Edge already = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(follower, overridden, already);
        when(edgeService.saveEdge(follower)).thenReturn(follower);

        assertThat(applier.setRoot(TENANT_ID, profile, previousRoot)).isEqualTo(1);
        verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, follower.getId(), rc1);
        verify(ruleChainEdgeProcessor, never()).pushRootRuleChain(eq(TENANT_ID), eq(overridden.getId()), any());
        verify(ruleChainEdgeProcessor, never()).pushRootRuleChain(eq(TENANT_ID), eq(already.getId()), any());
    }

    @Test
    void applyToEdgeKeepsTheEdgeRootUnlessCreatedOrForced() throws Exception {
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc2);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profile.getId())).thenReturn(profile);
        when(relationService.getRelation(eq(TENANT_ID), eq(edge.getId()), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenReturn(new EntityRelation(edge.getId(), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));

        applier.applyToEdge(TENANT_ID, edge, false);

        verify(ruleChainEdgeProcessor, never()).pushRootRuleChain(any(), any(), any());
    }

    @Test
    void reapplyGivesTheProfileRootOnlyToEdgesWithoutOne() throws Exception {
        Edge overridden = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc2);
        Edge rootless = edge(new EdgeId(UUID.randomUUID()), profile.getId(), null);
        profileEdges(overridden, rootless);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(new PageData<>(List.of(overridden.getId(), rootless.getId()), 1, 2, false));
        when(relationService.getRelation(eq(TENANT_ID), any(), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenReturn(new EntityRelation(overridden.getId(), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        when(relationService.findByFrom(eq(TENANT_ID), any(), eq(RelationTypeGroup.EDGE))).thenReturn(List.of());
        when(edgeService.saveEdge(rootless)).thenReturn(rootless);

        applier.reapply(TENANT_ID, profile);

        verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, rootless.getId(), rc1);
        verify(ruleChainEdgeProcessor, never()).pushRootRuleChain(eq(TENANT_ID), eq(overridden.getId()), any());
    }

    @Test
    void groupRowSkipsEdgesOfOtherCustomers() {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        CustomerId customerB = new CustomerId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, groupId));
        EntityGroup group = new EntityGroup(groupId);
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customerA);
        when(entityGroupService.findEntityGroupById(TENANT_ID, groupId)).thenReturn(group);
        Edge edgeA = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        edgeA.setCustomerId(customerA);
        Edge edgeB = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        edgeB.setCustomerId(customerB);
        profileEdges(edgeA, edgeB);
        when(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(eq(TENANT_ID), anyList(), eq(EntityType.DEVICE), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));
        when(ownersCacheService.isChildOwner(TENANT_ID, customerB, customerA)).thenReturn(false);

        int assigned = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE));

        assertThat(assigned).isEqualTo(1);
        verify(entityGroupService).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(edgeA.getId()), eq(EntityType.DEVICE), any(ObjectNode.class));
        verify(entityGroupService, never()).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(edgeB.getId()), eq(EntityType.DEVICE), any());
    }

    @Test
    void groupRowSkipsEdgeThatAlreadyHasTheGroup() {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, groupId));
        // tenant-owned, so the edge really is eligible and the skip below is the "already has it" branch rather than
        // an eligibility miss
        EntityGroup group = new EntityGroup(groupId);
        group.setType(EntityType.DEVICE);
        group.setOwnerId(TENANT_ID);
        when(entityGroupService.findEntityGroupById(TENANT_ID, groupId)).thenReturn(group);
        Edge hasGroup = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(hasGroup);
        when(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(eq(TENANT_ID), anyList(), eq(EntityType.DEVICE), any()))
                .thenReturn(new PageData<>(List.of(hasGroup.getId()), 1, 1, false));
        String relationType = BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + EntityType.DEVICE.name();
        when(relationService.getRelation(TENANT_ID, hasGroup.getId(), groupId, relationType, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(hasGroup.getId(), groupId, relationType, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));

        int assigned = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE));

        assertThat(assigned).isZero();
        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), any(), any(), any(), any());
    }

    @Test
    void groupRowReachesEdgeOwnedByCustomerAboveTheGroupOwner() {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        CustomerId parent = new CustomerId(UUID.randomUUID());
        CustomerId child = new CustomerId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, groupId));
        EntityGroup group = new EntityGroup(groupId);
        group.setType(EntityType.DEVICE);
        group.setOwnerId(child);
        when(entityGroupService.findEntityGroupById(TENANT_ID, groupId)).thenReturn(group);
        Edge above = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        above.setCustomerId(parent);
        profileEdges(above);
        when(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(eq(TENANT_ID), anyList(), eq(EntityType.DEVICE), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));
        // the branch every other case in this class leaves at its unstubbed false: the group owner sits under the edge owner
        when(ownersCacheService.isChildOwner(TENANT_ID, parent, child)).thenReturn(true);

        int assigned = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE));

        assertThat(assigned).isEqualTo(1);
        verify(entityGroupService).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(above.getId()), eq(EntityType.DEVICE), any(ObjectNode.class));
        verify(ownersCacheService).isChildOwner(TENANT_ID, parent, child);
    }

    @Test
    void reapplyResendsRowsTheCloudAlreadyAssignsButNotHandMadeOnes() {
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(edge);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(new PageData<>(List.of(edge.getId()), 1, 1, false));
        when(relationService.getRelation(TENANT_ID, edge.getId(), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(edge.getId(), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        when(relationService.getRelation(TENANT_ID, edge.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(edge.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        when(relationService.findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE)).thenReturn(List.of());

        applier.reapply(TENANT_ID, profile);

        ArgumentCaptor<EdgeEvent> resent = ArgumentCaptor.forClass(EdgeEvent.class);
        verify(edgeEventService).saveAsync(resent.capture());
        assertThat(resent.getValue().getType()).isEqualTo(EdgeEventType.RULE_CHAIN);
        assertThat(resent.getValue().getAction()).isEqualTo(EdgeEventActionType.ASSIGNED_TO_EDGE);
        assertThat(resent.getValue().getEntityId()).isEqualTo(rc1.getId());
        verify(ruleChainService, never()).assignRuleChainToEdge(any(), any(), any(), any());
    }

    @Test
    void reapplyDropsRowStrandedByAnInterruptedRemoval() {
        // rc1/rc2 are on the profile; this group was removed from it while the fan-out was interrupted, so the edge
        // still carries the relation this profile stamped and nothing else would ever reach it
        EntityGroupId strandedGroupId = new EntityGroupId(UUID.randomUUID());
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(edge);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(new PageData<>(List.of(edge.getId()), 1, 1, false));
        String relationType = BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + EntityType.DEVICE.name();
        when(relationService.getRelation(eq(TENANT_ID), eq(edge.getId()), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenReturn(new EntityRelation(edge.getId(), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        when(relationService.findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE))
                .thenReturn(List.of(new EntityRelation(edge.getId(), strandedGroupId, relationType, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId()))));

        applier.reapply(TENANT_ID, profile);

        verify(entityGroupService).unassignEntityGroupFromEdge(TENANT_ID, strandedGroupId, edge.getId(), EntityType.DEVICE);
    }

    @Test
    void reapplyDropsRowsStampedByOtherProfilesAndLeavesHandMadeRowsAlone() {
        EntityGroupId otherProfileGroupId = new EntityGroupId(UUID.randomUUID());
        EntityGroupId handMadeGroupId = new EntityGroupId(UUID.randomUUID());
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(edge);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(new PageData<>(List.of(edge.getId()), 1, 1, false));
        when(relationService.getRelation(eq(TENANT_ID), eq(edge.getId()), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenReturn(new EntityRelation(edge.getId(), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        String relationType = BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + EntityType.DEVICE.name();
        when(relationService.findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE)).thenReturn(List.of(
                new EntityRelation(edge.getId(), otherProfileGroupId, relationType, RelationTypeGroup.EDGE,
                        EdgeProfileApplier.stamp(new EdgeProfileId(UUID.randomUUID()))),
                new EntityRelation(edge.getId(), handMadeGroupId, relationType, RelationTypeGroup.EDGE)));

        applier.reapply(TENANT_ID, profile);

        verify(entityGroupService).unassignEntityGroupFromEdge(TENANT_ID, otherProfileGroupId, edge.getId(), EntityType.DEVICE);
        verify(entityGroupService, never()).unassignEntityGroupFromEdge(TENANT_ID, handMadeGroupId, edge.getId(), EntityType.DEVICE);
    }

    @Test
    void reapplyLeavesAnEdgeThatSwitchedAwayAfterItsPageWasRead() {
        Edge listed = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(listed);
        when(edgeService.findEdgeById(TENANT_ID, listed.getId())).thenReturn(edge(listed.getId(), new EdgeProfileId(UUID.randomUUID()), rc1));
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(new PageData<>(List.of(), 1, 0, false));

        applier.reapply(TENANT_ID, profile);

        verify(ruleChainService, never()).assignRuleChainToEdge(any(), any(), any(), any());
        verify(relationService, never()).findByFrom(any(), any(), any());
        verify(edgeEventService, never()).saveAsync(any());
    }

    @Test
    void profileSwitchWaitsForAnApplyAlreadyWorkingOnThatEdge() throws Exception {
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(edge);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(new PageData<>(List.of(edge.getId()), 1, 1, false));
        when(relationService.getRelation(eq(TENANT_ID), eq(edge.getId()), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenReturn(new EntityRelation(edge.getId(), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profile.getId())).thenReturn(profile);
        CountDownLatch applyHoldsEdge = new CountDownLatch(1);
        CountDownLatch releaseApply = new CountDownLatch(1);
        when(relationService.findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE)).thenAnswer(invocation -> {
            applyHoldsEdge.countDown();
            releaseApply.await(10, TimeUnit.SECONDS);
            return List.of();
        });
        CompletableFuture<Void> apply = CompletableFuture.runAsync(() -> applier.reapply(TENANT_ID, profile));
        assertThat(applyHoldsEdge.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Edge> profileSwitch = CompletableFuture.supplyAsync(() -> {
            try {
                return applier.applyToEdge(TENANT_ID, edge, false);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });

        assertThatThrownBy(() -> profileSwitch.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        releaseApply.countDown();
        assertThat(profileSwitch.get(10, TimeUnit.SECONDS)).isSameAs(edge);
        apply.get(10, TimeUnit.SECONDS);
    }

    @Test
    void applyRowReclaimsRowStampedByAnotherProfile() {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        EdgeProfileId otherProfileId = new EdgeProfileId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, groupId));
        EntityGroup group = new EntityGroup(groupId);
        group.setType(EntityType.DEVICE);
        group.setOwnerId(TENANT_ID);
        when(entityGroupService.findEntityGroupById(TENANT_ID, groupId)).thenReturn(group);
        Edge moved = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(moved);
        when(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(eq(TENANT_ID), anyList(), eq(EntityType.DEVICE), any()))
                .thenReturn(new PageData<>(List.of(moved.getId()), 1, 1, false));
        String relationType = BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + EntityType.DEVICE.name();
        // the edge carried this row over from the profile it left, so the relation still has the old stamp
        when(relationService.getRelation(TENANT_ID, moved.getId(), groupId, relationType, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(moved.getId(), groupId, relationType, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(otherProfileId)));

        int touched = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE));

        // without the re-stamp the row would answer to neither profile: this one cannot remove it, and the old one no longer owns the edge
        assertThat(touched).isEqualTo(1);
        verify(entityGroupService).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(moved.getId()), eq(EntityType.DEVICE), any(ObjectNode.class));
    }

    @Test
    void applyToEdgeSkipsMissingEntityGroupButAssignsOtherRows() throws Exception {
        EntityGroupId missingGroupId = new EntityGroupId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, missingGroupId));
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), null);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profile.getId())).thenReturn(profile);
        when(edgeService.saveEdge(edge)).thenReturn(edge);

        Edge result = applier.applyToEdge(TENANT_ID, edge, true);

        assertThat(result).isEqualTo(edge);
        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), any(), any(), any(), any());
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc1), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, edge.getId(), rc1);
    }

    @Test
    void applyRowDropsStampedRowFromEdgeWhoseOwnerCanNoLongerSeeIt() {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        CustomerId customerB = new CustomerId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, groupId));
        EntityGroup group = new EntityGroup(groupId);
        group.setType(EntityType.DEVICE);
        group.setOwnerId(customerA);
        when(entityGroupService.findEntityGroupById(TENANT_ID, groupId)).thenReturn(group);
        Edge stamped = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        stamped.setCustomerId(customerB);
        Edge manual = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        manual.setCustomerId(customerB);
        profileEdges(stamped, manual);
        when(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(eq(TENANT_ID), anyList(), eq(EntityType.DEVICE), any()))
                .thenReturn(new PageData<>(List.of(stamped.getId(), manual.getId()), 1, 2, false));
        when(ownersCacheService.isChildOwner(TENANT_ID, customerB, customerA)).thenReturn(false);
        String relationType = BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + EntityType.DEVICE.name();
        when(relationService.getRelation(TENANT_ID, stamped.getId(), groupId, relationType, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(stamped.getId(), groupId, relationType, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));
        when(relationService.getRelation(TENANT_ID, manual.getId(), groupId, relationType, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(manual.getId(), groupId, relationType, RelationTypeGroup.EDGE));

        int touched = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE));

        assertThat(touched).isEqualTo(1);
        verify(entityGroupService).unassignEntityGroupFromEdge(TENANT_ID, groupId, stamped.getId(), EntityType.DEVICE);
        verify(entityGroupService, never()).unassignEntityGroupFromEdge(eq(TENANT_ID), eq(groupId), eq(manual.getId()), any());
        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), any(), any(), any(), any());
        // the group owner is resolved once per row, not once per edge
        verify(entityGroupService, times(1)).findEntityGroupById(TENANT_ID, groupId);
    }

    @Test
    void schedulerEventRowFollowsTheEventCustomer() {
        SchedulerEventId customerEventId = new SchedulerEventId(UUID.randomUUID());
        SchedulerEventId tenantEventId = new SchedulerEventId(UUID.randomUUID());
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        CustomerId customerB = new CustomerId(UUID.randomUUID());
        SchedulerEventInfo customerEvent = new SchedulerEventInfo(customerEventId);
        customerEvent.setCustomerId(customerA);
        SchedulerEventInfo tenantEvent = new SchedulerEventInfo(tenantEventId);
        tenantEvent.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, customerEventId)).thenReturn(customerEvent);
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, tenantEventId)).thenReturn(tenantEvent);
        Edge edgeA = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        edgeA.setCustomerId(customerA);
        Edge edgeB = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        edgeB.setCustomerId(customerB);
        profileEdges(edgeA, edgeB);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.SCHEDULER_EVENT), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        int customerTouched = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.schedulerEvent(customerEventId));
        int tenantTouched = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.schedulerEvent(tenantEventId));

        assertThat(customerTouched).isEqualTo(1);
        verify(schedulerEventService).assignSchedulerEventToEdge(eq(TENANT_ID), eq(customerEventId), eq(edgeA.getId()), any(ObjectNode.class));
        verify(schedulerEventService, never()).assignSchedulerEventToEdge(eq(TENANT_ID), eq(customerEventId), eq(edgeB.getId()), any());
        // a null-uuid customer means the event belongs to the tenant and reaches every edge
        assertThat(tenantTouched).isEqualTo(2);
        verify(schedulerEventService).assignSchedulerEventToEdge(eq(TENANT_ID), eq(tenantEventId), eq(edgeA.getId()), any(ObjectNode.class));
        verify(schedulerEventService).assignSchedulerEventToEdge(eq(TENANT_ID), eq(tenantEventId), eq(edgeB.getId()), any(ObjectNode.class));
    }

    @Test
    void customerBoundRowsSkipTenantEdgeWhileTheCustomerDeviceGroupStillReachesIt() {
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        SchedulerEventId eventId = new SchedulerEventId(UUID.randomUUID());
        SchedulerEventInfo event = new SchedulerEventInfo(eventId);
        event.setCustomerId(customerA);
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, eventId)).thenReturn(event);
        EntityGroupId usersId = new EntityGroupId(UUID.randomUUID());
        EntityGroup users = new EntityGroup(usersId);
        users.setType(EntityType.USER);
        users.setOwnerId(customerA);
        when(entityGroupService.findEntityGroupById(TENANT_ID, usersId)).thenReturn(users);
        EntityGroupId devicesId = new EntityGroupId(UUID.randomUUID());
        EntityGroup devices = new EntityGroup(devicesId);
        devices.setType(EntityType.DEVICE);
        devices.setOwnerId(customerA);
        when(entityGroupService.findEntityGroupById(TENANT_ID, devicesId)).thenReturn(devices);
        Edge tenantEdge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        profileEdges(tenantEdge);
        when(edgeService.findEdgeIdsByTenantIdAndEntityIds(eq(TENANT_ID), anyList(), eq(EntityType.SCHEDULER_EVENT), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));
        when(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        int eventTouched = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.schedulerEvent(eventId));
        int usersTouched = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(usersId, EntityType.USER));
        int devicesTouched = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(devicesId, EntityType.DEVICE));

        // a tenant edge never holds Customer A, so it rejects the event and the users; devices land there tenant-owned
        assertThat(eventTouched).isZero();
        assertThat(usersTouched).isZero();
        assertThat(devicesTouched).isEqualTo(1);
        verify(schedulerEventService, never()).assignSchedulerEventToEdge(any(), any(), any(), any());
        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), eq(usersId), any(), any(), any());
        verify(entityGroupService).assignEntityGroupToEdge(eq(TENANT_ID), eq(devicesId), eq(tenantEdge.getId()), eq(EntityType.DEVICE), any(ObjectNode.class));
    }

    @Test
    void customerBoundRowSkipsEdgeOwnedByCustomerAboveTheRowOwner() {
        CustomerId parent = new CustomerId(UUID.randomUUID());
        CustomerId child = new CustomerId(UUID.randomUUID());
        EntityGroupId usersId = new EntityGroupId(UUID.randomUUID());
        EntityGroup users = new EntityGroup(usersId);
        users.setType(EntityType.USER);
        users.setOwnerId(child);
        when(entityGroupService.findEntityGroupById(TENANT_ID, usersId)).thenReturn(users);
        Edge above = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        above.setCustomerId(parent);
        profileEdges(above);
        when(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(eq(TENANT_ID), anyList(), eq(EntityType.USER), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        int assigned = applier.applyRowToProfileEdges(TENANT_ID, profile, EdgeProfileApplier.EdgeAssignmentRowRef.entityGroup(usersId, EntityType.USER));

        // an edge holds its owner's customer and the ones above it, never a sub-customer, so it would reject the sub-customer's users
        assertThat(assigned).isZero();
        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), any(), any(), any(), any());
        verify(ownersCacheService, never()).isChildOwner(any(), any(), any());
    }

    @Test
    void applyToEdgeDropsStampedCustomerSchedulerFromEdgeMovedToTheTenant() throws Exception {
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        SchedulerEventId eventId = new SchedulerEventId(UUID.randomUUID());
        SchedulerEventInfo event = new SchedulerEventInfo(eventId);
        event.setCustomerId(customerA);
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, eventId)).thenReturn(event);
        profile.getProfileData().getSchedulerEventIds().add(eventId);
        Edge movedToTenant = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profile.getId())).thenReturn(profile);
        // every row, rule chains included, was stamped while the edge still belonged to Customer A
        when(relationService.getRelation(eq(TENANT_ID), eq(movedToTenant.getId()), any(), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.EDGE)))
                .thenAnswer(inv -> new EntityRelation(movedToTenant.getId(), inv.getArgument(2), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(profile.getId())));

        applier.applyToEdge(TENANT_ID, movedToTenant, false);

        verify(schedulerEventService).unassignSchedulerEventFromEdge(TENANT_ID, eventId, movedToTenant.getId());
        verify(schedulerEventService, never()).assignSchedulerEventToEdge(any(), any(), any(), any());
    }

    @Test
    void applyToEdgeWithoutProfileIsANoop() throws Exception {
        Edge edge = edge(new EdgeId(UUID.randomUUID()), null, rc1);

        assertThat(applier.applyToEdge(TENANT_ID, edge, false)).isEqualTo(edge);

        verify(edgeProfileService, never()).findEdgeProfileById(any(), any());
        verify(ruleChainService, never()).assignRuleChainToEdge(any(), any(), any(), any());
    }

    @Test
    void applyToEdgeDropsRowsStampedByThePreviousProfile() throws Exception {
        RuleChainId sharedRc = rc1;
        RuleChainId oldOnlyRc = new RuleChainId(UUID.randomUUID());
        RuleChainId handMadeRc = new RuleChainId(UUID.randomUUID());
        EdgeProfile previous = new EdgeProfile(new EdgeProfileId(UUID.randomUUID()));
        previous.setTenantId(TENANT_ID);
        previous.setName("Old");
        previous.getProfileData().getRuleChainIds().add(sharedRc);
        previous.getProfileData().getRuleChainIds().add(oldOnlyRc);
        previous.getProfileData().getRuleChainIds().add(handMadeRc);
        previous.setRootRuleChainId(oldOnlyRc);
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profile.getId())).thenReturn(profile);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, previous.getId())).thenReturn(previous);
        when(relationService.getRelation(TENANT_ID, edge.getId(), sharedRc, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(edge.getId(), sharedRc, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(previous.getId())));
        when(relationService.getRelation(TENANT_ID, edge.getId(), rc2, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE)).thenReturn(null);
        when(relationService.getRelation(TENANT_ID, edge.getId(), oldOnlyRc, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(edge.getId(), oldOnlyRc, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(previous.getId())));
        when(relationService.getRelation(TENANT_ID, edge.getId(), handMadeRc, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(edge.getId(), handMadeRc, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));

        applier.applyToEdge(TENANT_ID, edge, false, true, previous.getId());

        // new profile: the missing row is added, the shared row is re-stamped so the old profile's stamp does not orphan it
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(sharedRc), eq(edge.getId()), any(ObjectNode.class));
        // previous profile: only its stamped, non-shared row goes; the hand-made one stays
        verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, oldOnlyRc, edge.getId(), false);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(sharedRc), eq(edge.getId()), eq(false));
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(handMadeRc), eq(edge.getId()), eq(false));
        // the edge refuses to delete the chain it is rooted on, so the new root must be persisted before the old one is dropped
        InOrder rootBeforeDrop = inOrder(ruleChainEdgeProcessor, ruleChainService);
        rootBeforeDrop.verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, edge.getId(), rc1);
        rootBeforeDrop.verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, oldOnlyRc, edge.getId(), false);
    }

    @Test
    void applyToEdgeSkipsRuleChainDeletedWhileStillListedOnTheProfile() throws Exception {
        Edge edge = edge(new EdgeId(UUID.randomUUID()), profile.getId(), null);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profile.getId())).thenReturn(profile);
        when(ruleChainService.findRuleChainById(TENANT_ID, rc2)).thenReturn(null);
        when(edgeService.saveEdge(edge)).thenReturn(edge);

        applier.applyToEdge(TENANT_ID, edge, true);

        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc1), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainService, never()).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(edge.getId()), any());
    }

    @Test
    void stampRoundTrip() {
        ObjectNode stamp = EdgeProfileApplier.stamp(profile.getId());
        EntityRelation relation = new EntityRelation(new EdgeId(UUID.randomUUID()), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, stamp);
        assertThat(EdgeProfileApplier.isStampedBy(relation, profile.getId())).isTrue();
        assertThat(EdgeProfileApplier.isStampedBy(relation, new EdgeProfileId(UUID.randomUUID()))).isFalse();
        assertThat(EdgeProfileApplier.isStampedBy(new EntityRelation(), profile.getId())).isFalse();
        assertThat(JacksonUtil.toString(stamp)).contains(profile.getId().getId().toString());
    }

    @Test
    void customerOwnerChangeReappliesEdgesOfTheOwnersItLeftAndJoined() {
        CustomerId moved = new CustomerId(UUID.randomUUID());
        CustomerId formerParent = new CustomerId(UUID.randomUUID());
        CustomerId newParent = new CustomerId(UUID.randomUUID());
        when(ownersCacheService.getOwner(TENANT_ID, moved)).thenReturn(newParent);
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, formerParent)).thenReturn(Set.of(formerParent, TENANT_ID));
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, newParent)).thenReturn(Set.of(newParent, TENANT_ID));
        when(edgeService.findEdgesByTenantIdAndCustomerId(eq(TENANT_ID), any(), any())).thenReturn(new PageData<>(List.of(), 0, 0, false));

        applier.onOwnerChanged(ownerChanged(moved, formerParent));

        verify(edgeService, timeout(5000)).findEdgesByTenantIdAndCustomerId(eq(TENANT_ID), eq(formerParent), any());
        verify(edgeService, timeout(5000)).findEdgesByTenantIdAndCustomerId(eq(TENANT_ID), eq(newParent), any());
        verify(edgeService, never()).findEdgesByTenantIdAndCustomerId(eq(TENANT_ID), eq(moved), any());
    }

    @Test
    void edgeOwnerChangeReappliesTheProfileToTheEdge() {
        Edge moved = edge(new EdgeId(UUID.randomUUID()), profile.getId(), rc1);
        when(edgeService.findEdgeById(TENANT_ID, moved.getId())).thenReturn(moved);
        when(edgeProfileService.findEdgeProfileById(TENANT_ID, profile.getId())).thenReturn(profile);

        applier.onOwnerChanged(ownerChanged(moved.getId(), new CustomerId(UUID.randomUUID())));

        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc1), eq(moved.getId()), any());
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(moved.getId()), any());
    }

    private ActionEntityEvent<?> ownerChanged(EntityId movedId, EntityId formerOwnerId) {
        return ActionEntityEvent.builder().tenantId(TENANT_ID).entityId(movedId)
                .body(JacksonUtil.toString(formerOwnerId)).actionType(ActionType.CHANGE_OWNER).build();
    }

}
