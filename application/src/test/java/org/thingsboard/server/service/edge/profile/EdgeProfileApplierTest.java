// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
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
import org.thingsboard.server.common.data.job.EdgeProfileJobRow;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.BaseEntityGroupService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.service.edge.profile.EdgeProfileRows.EdgeAssignmentRowRef;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeProfileApplierTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final String DEVICE_GROUP_RELATION = BaseEntityGroupService.EDGE_ENTITY_GROUP_RELATION_PREFIX + EntityType.DEVICE.name();

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
    private final Map<EdgeId, Edge> edges = new HashMap<>();
    private final Map<EdgeProfileId, EdgeProfile> profiles = new HashMap<>();

    @BeforeEach
    void setUp() {
        applier = new EdgeProfileApplier(edgeProfileService, edgeService, relationService, ruleChainService,
                entityGroupService, integrationService, schedulerEventService, new EdgeProfileEligibility(ownersCacheService),
                edgeEventService, ruleChainEdgeProcessorProvider);
        lenient().when(edgeEventService.saveAsync(any())).thenReturn(Futures.immediateFuture(null));
        profile = new EdgeProfile(new EdgeProfileId(UUID.randomUUID()));
        profile.setTenantId(TENANT_ID);
        profile.setName("P");
        rc1 = new RuleChainId(UUID.randomUUID());
        rc2 = new RuleChainId(UUID.randomUUID());
        profile.getProfileData().getRuleChainIds().add(rc1);
        profile.getProfileData().getRuleChainIds().add(rc2);
        profile.setRootRuleChainId(rc1);
        profiles.put(profile.getId(), profile);
        lenient().when(edgeService.findEdgeById(eq(TENANT_ID), any())).thenAnswer(inv -> edges.get(inv.<EdgeId>getArgument(1)));
        lenient().when(edgeProfileService.findEdgeProfileById(eq(TENANT_ID), any())).thenAnswer(inv -> profiles.get(inv.<EdgeProfileId>getArgument(1)));
        lenient().when(ruleChainService.findRuleChainById(any(), any())).thenReturn(new RuleChain());
        lenient().when(integrationService.findIntegrationById(any(), any())).thenReturn(new Integration());
        lenient().when(ruleChainEdgeProcessorProvider.getObject()).thenReturn(ruleChainEdgeProcessor);
        lenient().when(ruleChainEdgeProcessor.pushRootRuleChain(any(), any(), any())).thenReturn(Futures.immediateFuture(null));
    }

    private Edge edge(EdgeProfileId profileId, RuleChainId root) {
        Edge e = new Edge(new EdgeId(UUID.randomUUID()));
        e.setTenantId(TENANT_ID);
        e.setEdgeProfileId(profileId);
        e.setRootRuleChainId(root);
        e.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        edges.put(e.getId(), e);
        return e;
    }

    private EdgeProfile otherProfile() {
        EdgeProfile other = new EdgeProfile(new EdgeProfileId(UUID.randomUUID()));
        other.setTenantId(TENANT_ID);
        profiles.put(other.getId(), other);
        return other;
    }

    private void onProfile(EdgeAssignmentRowRef row) {
        switch (row.entityType()) {
            case RULE_CHAIN -> profile.getProfileData().getRuleChainIds().add((RuleChainId) row.id());
            case ENTITY_GROUP -> profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(row.groupType(), (EntityGroupId) row.id()));
            case SCHEDULER_EVENT -> profile.getProfileData().getSchedulerEventIds().add((SchedulerEventId) row.id());
            default -> throw new IllegalArgumentException(row.entityType().name());
        }
    }

    private boolean addRow(Edge edge, EdgeAssignmentRowRef row) throws Exception {
        return applier.addRows(TENANT_ID, edge.getId(), profile.getId(), List.of(row), false, null, null);
    }

    private boolean moveRoot(Edge edge, RuleChainId root, RuleChainId previousRoot) throws Exception {
        return applier.addRows(TENANT_ID, edge.getId(), profile.getId(), List.of(), true, root, previousRoot);
    }

    private boolean removeRow(Edge edge, EdgeAssignmentRowRef row) {
        return applier.removeRow(TENANT_ID, edge.getId(), profile.getId(), row);
    }

    private EntityRelation ruleChainRelation(Edge edge, RuleChainId ruleChainId, EdgeProfileId stampedBy) {
        return stampedBy == null
                ? new EntityRelation(edge.getId(), ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE)
                : new EntityRelation(edge.getId(), ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileRows.stamp(stampedBy));
    }

    private void relation(Edge edge, RuleChainId ruleChainId, EntityRelation relation) {
        when(relationService.getRelation(TENANT_ID, edge.getId(), ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE)).thenReturn(relation);
    }

    private EntityGroup group(EntityGroupId id, EntityType type, EntityId ownerId) {
        EntityGroup group = new EntityGroup(id);
        group.setType(type);
        group.setOwnerId(ownerId);
        when(entityGroupService.findEntityGroupById(TENANT_ID, id)).thenReturn(group);
        return group;
    }

    @Test
    void everyStepTakesTheEdgeLockBeforeReadingTheEdge() throws Exception {
        Edge edge = edge(profile.getId(), rc1);

        applier.reapply(TENANT_ID, edge.getId(), profile.getId());

        InOrder lockFirst = inOrder(edgeProfileService, edgeService, relationService);
        lockFirst.verify(edgeProfileService).lockEdgeForApply(eq(TENANT_ID), eq(edge.getId()), anyLong());
        lockFirst.verify(edgeService).findEdgeById(TENANT_ID, edge.getId());
        lockFirst.verify(relationService).findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE);
    }

    @Test
    void addRowsSkipsAnEdgeThatLeftTheProfile() throws Exception {
        Edge edge = edge(new EdgeProfileId(UUID.randomUUID()), rc1);

        assertThat(addRow(edge, EdgeAssignmentRowRef.ruleChain(rc1))).isFalse();

        verifyNoInteractions(relationService, ruleChainService, ruleChainEdgeProcessor);
    }

    @Test
    void addRowsAppliesOnlyRowsStillOnTheProfileThenMovesTheRoot() throws Exception {
        profile.getProfileData().getRuleChainIds().remove(rc2);
        RuleChainId previousRoot = new RuleChainId(UUID.randomUUID());
        Edge edge = edge(profile.getId(), previousRoot);
        when(edgeService.saveEdge(edge)).thenReturn(edge);

        applier.addRows(TENANT_ID, edge.getId(), profile.getId(),
                List.of(EdgeAssignmentRowRef.ruleChain(rc1), EdgeAssignmentRowRef.ruleChain(rc2)), true, rc1, previousRoot);

        InOrder rowBeforeRoot = inOrder(ruleChainService, ruleChainEdgeProcessor);
        rowBeforeRoot.verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc1), eq(edge.getId()), any(ObjectNode.class));
        rowBeforeRoot.verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, edge.getId(), rc1);
        verify(ruleChainService, never()).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), any(), any());
    }

    @Test
    void applyRowAssignsAMissingRowAndLeavesAStampedOneAlone() throws Exception {
        Edge missing = edge(profile.getId(), rc1);
        Edge carrying = edge(profile.getId(), rc1);
        relation(missing, rc2, null);
        relation(carrying, rc2, ruleChainRelation(carrying, rc2, profile.getId()));

        assertThat(addRow(missing, EdgeAssignmentRowRef.ruleChain(rc2))).isTrue();
        assertThat(addRow(carrying, EdgeAssignmentRowRef.ruleChain(rc2))).isFalse();

        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(missing.getId()), any(ObjectNode.class));
        verify(ruleChainService, never()).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(carrying.getId()), any());
    }

    @Test
    void applyRowLeavesAHandMadeRelationAlone() throws Exception {
        Edge edge = edge(profile.getId(), rc1);
        relation(edge, rc2, ruleChainRelation(edge, rc2, null));

        assertThat(addRow(edge, EdgeAssignmentRowRef.ruleChain(rc2))).isFalse();

        verify(ruleChainService, never()).assignRuleChainToEdge(any(), any(), any(), any(ObjectNode.class));
    }

    @Test
    void applyRowReclaimsRowStampedByAnotherProfile() throws Exception {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, groupId));
        group(groupId, EntityType.DEVICE, TENANT_ID);
        Edge moved = edge(profile.getId(), rc1);
        when(relationService.getRelation(TENANT_ID, moved.getId(), groupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(moved.getId(), groupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE, EdgeProfileRows.stamp(new EdgeProfileId(UUID.randomUUID()))));

        assertThat(addRow(moved, EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE))).isTrue();

        verify(entityGroupService).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(moved.getId()), eq(EntityType.DEVICE), any(ObjectNode.class));
    }

    @Test
    void groupRowSkipsEdgeOfAnotherCustomer() throws Exception {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        CustomerId customerB = new CustomerId(UUID.randomUUID());
        group(groupId, EntityType.DEVICE, customerA);
        Edge edgeA = edge(profile.getId(), rc1);
        edgeA.setCustomerId(customerA);
        Edge edgeB = edge(profile.getId(), rc1);
        edgeB.setCustomerId(customerB);
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, customerA)).thenReturn(Set.of(customerA, TENANT_ID));
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE);
        onProfile(row);

        assertThat(addRow(edgeA, row)).isTrue();
        assertThat(addRow(edgeB, row)).isFalse();

        verify(entityGroupService).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(edgeA.getId()), eq(EntityType.DEVICE), any(ObjectNode.class));
        verify(entityGroupService, never()).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(edgeB.getId()), eq(EntityType.DEVICE), any());
    }

    @Test
    void groupRowReachesEdgeOwnedByCustomerAboveTheGroupOwner() throws Exception {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        CustomerId parent = new CustomerId(UUID.randomUUID());
        CustomerId child = new CustomerId(UUID.randomUUID());
        group(groupId, EntityType.DEVICE, child);
        Edge above = edge(profile.getId(), rc1);
        above.setCustomerId(parent);
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, child)).thenReturn(Set.of(child, parent, TENANT_ID));
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE);
        onProfile(row);

        assertThat(addRow(above, row)).isTrue();

        verify(entityGroupService).assignEntityGroupToEdge(eq(TENANT_ID), eq(groupId), eq(above.getId()), eq(EntityType.DEVICE), any(ObjectNode.class));
    }

    @Test
    void applyRowDropsStampedRowFromEdgeWhoseOwnerCanNoLongerSeeItAndKeepsAHandMadeOne() throws Exception {
        EntityGroupId groupId = new EntityGroupId(UUID.randomUUID());
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        CustomerId customerB = new CustomerId(UUID.randomUUID());
        group(groupId, EntityType.DEVICE, customerA);
        Edge stamped = edge(profile.getId(), rc1);
        stamped.setCustomerId(customerB);
        Edge manual = edge(profile.getId(), rc1);
        manual.setCustomerId(customerB);
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, customerA)).thenReturn(Set.of(customerA, TENANT_ID));
        when(relationService.getRelation(TENANT_ID, stamped.getId(), groupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(stamped.getId(), groupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE, EdgeProfileRows.stamp(profile.getId())));
        when(relationService.getRelation(TENANT_ID, manual.getId(), groupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE))
                .thenReturn(new EntityRelation(manual.getId(), groupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE));
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.entityGroup(groupId, EntityType.DEVICE);
        onProfile(row);

        assertThat(addRow(stamped, row)).isTrue();
        assertThat(addRow(manual, row)).isFalse();

        verify(entityGroupService).unassignEntityGroupFromEdge(TENANT_ID, groupId, stamped.getId(), EntityType.DEVICE);
        verify(entityGroupService, never()).unassignEntityGroupFromEdge(eq(TENANT_ID), eq(groupId), eq(manual.getId()), any());
        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), any(), any(), any(), any());
    }

    @Test
    void schedulerEventRowFollowsTheEventCustomer() throws Exception {
        SchedulerEventId customerEventId = new SchedulerEventId(UUID.randomUUID());
        SchedulerEventId tenantEventId = new SchedulerEventId(UUID.randomUUID());
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        SchedulerEventInfo customerEvent = new SchedulerEventInfo(customerEventId);
        customerEvent.setCustomerId(customerA);
        SchedulerEventInfo tenantEvent = new SchedulerEventInfo(tenantEventId);
        tenantEvent.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, customerEventId)).thenReturn(customerEvent);
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, tenantEventId)).thenReturn(tenantEvent);
        Edge edgeA = edge(profile.getId(), rc1);
        edgeA.setCustomerId(customerA);
        Edge edgeB = edge(profile.getId(), rc1);
        edgeB.setCustomerId(new CustomerId(UUID.randomUUID()));
        onProfile(EdgeAssignmentRowRef.schedulerEvent(customerEventId));
        onProfile(EdgeAssignmentRowRef.schedulerEvent(tenantEventId));

        assertThat(addRow(edgeA, EdgeAssignmentRowRef.schedulerEvent(customerEventId))).isTrue();
        assertThat(addRow(edgeB, EdgeAssignmentRowRef.schedulerEvent(customerEventId))).isFalse();
        assertThat(addRow(edgeA, EdgeAssignmentRowRef.schedulerEvent(tenantEventId))).isTrue();
        assertThat(addRow(edgeB, EdgeAssignmentRowRef.schedulerEvent(tenantEventId))).isTrue();
    }

    @Test
    void customerBoundRowsSkipTenantEdgeWhileTheCustomerDeviceGroupStillReachesIt() throws Exception {
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        SchedulerEventId eventId = new SchedulerEventId(UUID.randomUUID());
        SchedulerEventInfo event = new SchedulerEventInfo(eventId);
        event.setCustomerId(customerA);
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, eventId)).thenReturn(event);
        EntityGroupId usersId = new EntityGroupId(UUID.randomUUID());
        group(usersId, EntityType.USER, customerA);
        EntityGroupId devicesId = new EntityGroupId(UUID.randomUUID());
        group(devicesId, EntityType.DEVICE, customerA);
        Edge tenantEdge = edge(profile.getId(), rc1);
        onProfile(EdgeAssignmentRowRef.schedulerEvent(eventId));
        onProfile(EdgeAssignmentRowRef.entityGroup(usersId, EntityType.USER));
        onProfile(EdgeAssignmentRowRef.entityGroup(devicesId, EntityType.DEVICE));

        assertThat(addRow(tenantEdge, EdgeAssignmentRowRef.schedulerEvent(eventId))).isFalse();
        assertThat(addRow(tenantEdge, EdgeAssignmentRowRef.entityGroup(usersId, EntityType.USER))).isFalse();
        assertThat(addRow(tenantEdge, EdgeAssignmentRowRef.entityGroup(devicesId, EntityType.DEVICE))).isTrue();

        verify(schedulerEventService, never()).assignSchedulerEventToEdge(any(), any(), any(), any());
        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), eq(usersId), any(), any(), any());
    }

    @Test
    void customerBoundRowSkipsEdgeOwnedByCustomerAboveTheRowOwner() throws Exception {
        CustomerId parent = new CustomerId(UUID.randomUUID());
        CustomerId child = new CustomerId(UUID.randomUUID());
        EntityGroupId usersId = new EntityGroupId(UUID.randomUUID());
        group(usersId, EntityType.USER, child);
        Edge above = edge(profile.getId(), rc1);
        above.setCustomerId(parent);
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.entityGroup(usersId, EntityType.USER);
        onProfile(row);

        assertThat(addRow(above, row)).isFalse();

        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), any(), any(), any(), any());
        verify(ownersCacheService, never()).fetchOwnersHierarchy(any(), any());
    }

    @Test
    void removeRowSkipsARowThatWasAddedBackToTheProfile() {
        Edge edge = edge(profile.getId(), rc1);

        assertThat(removeRow(edge, EdgeAssignmentRowRef.ruleChain(rc2))).isFalse();

        verifyNoInteractions(relationService);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(any(), any(), any(), eq(false));
    }

    @Test
    void removeRowReachesAnEdgeOnAnotherProfile() {
        profile.getProfileData().getRuleChainIds().remove(rc2);
        EdgeProfile current = otherProfile();
        Edge edge = edge(current.getId(), rc1);
        relation(edge, rc2, ruleChainRelation(edge, rc2, profile.getId()));

        assertThat(removeRow(edge, EdgeAssignmentRowRef.ruleChain(rc2))).isTrue();

        verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, rc2, edge.getId(), false);
    }

    @Test
    void unassignRowTouchesOnlyRelationsStampedByTheProfile() {
        profile.getProfileData().getRuleChainIds().remove(rc2);
        Edge stamped = edge(profile.getId(), rc1);
        Edge manual = edge(profile.getId(), rc1);
        Edge foreign = edge(profile.getId(), rc1);
        relation(stamped, rc2, ruleChainRelation(stamped, rc2, profile.getId()));
        relation(manual, rc2, ruleChainRelation(manual, rc2, null));
        relation(foreign, rc2, ruleChainRelation(foreign, rc2, new EdgeProfileId(UUID.randomUUID())));
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.ruleChain(rc2);

        assertThat(removeRow(stamped, row)).isTrue();
        assertThat(removeRow(manual, row)).isFalse();
        assertThat(removeRow(foreign, row)).isFalse();

        verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, rc2, stamped.getId(), false);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(rc2), eq(manual.getId()), eq(false));
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(rc2), eq(foreign.getId()), eq(false));
    }

    @Test
    void unassignRowReachesEdgesThatSwitchedAwayUnlessTheirNewProfileCarriesTheRow() {
        profile.getProfileData().getRuleChainIds().remove(rc2);
        EdgeProfile carrying = otherProfile();
        carrying.getProfileData().getRuleChainIds().add(rc2);
        EdgeProfile other = otherProfile();
        Edge switchedToOther = edge(other.getId(), rc1);
        Edge switchedToCarrying = edge(carrying.getId(), rc1);
        relation(switchedToOther, rc2, ruleChainRelation(switchedToOther, rc2, profile.getId()));
        relation(switchedToCarrying, rc2, ruleChainRelation(switchedToCarrying, rc2, profile.getId()));
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.ruleChain(rc2);

        assertThat(removeRow(switchedToOther, row)).isTrue();
        assertThat(removeRow(switchedToCarrying, row)).isFalse();

        verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, rc2, switchedToOther.getId(), false);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(rc2), eq(switchedToCarrying.getId()), eq(false));
    }

    @Test
    void unassignRowKeepsTheEdgeRootAsTheEdgesOwn() {
        profile.getProfileData().getRuleChainIds().remove(rc2);
        Edge overridden = edge(profile.getId(), rc2);
        relation(overridden, rc2, ruleChainRelation(overridden, rc2, profile.getId()));

        assertThat(removeRow(overridden, EdgeAssignmentRowRef.ruleChain(rc2))).isTrue();

        verify(ruleChainService, never()).unassignRuleChainFromEdge(any(), any(), any(), eq(false));
        assertUnstamped(overridden, rc2);
    }

    @Test
    void profileSwitchKeepsTheEdgeRootFromThePreviousProfileAsTheEdgesOwn() throws Exception {
        EdgeProfile previous = otherProfile();
        EdgeProfileId previousProfileId = previous.getId();
        RuleChainId previousRow = new RuleChainId(UUID.randomUUID());
        previous.getProfileData().getRuleChainIds().add(previousRow);
        Edge edge = edge(profile.getId(), previousRow);
        when(relationService.getRelation(eq(TENANT_ID), eq(edge.getId()), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenAnswer(inv -> ruleChainRelation(edge, inv.getArgument(2), previousProfileId));

        applier.applyToEdge(TENANT_ID, edge.getId(), false, false, previousProfileId);

        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(previousRow), any(), eq(false));
        assertUnstamped(edge, previousRow);
    }

    private void assertUnstamped(Edge edge, RuleChainId ruleChainId) {
        ArgumentCaptor<EntityRelation> saved = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationService).saveRelation(eq(TENANT_ID), saved.capture());
        assertThat(saved.getValue().getFrom()).isEqualTo(edge.getId());
        assertThat(saved.getValue().getTo()).isEqualTo(ruleChainId);
        assertThat(EdgeProfileRows.isStamped(saved.getValue())).isFalse();
    }

    @Test
    void moveRootMovesOnlyFollowersOfThePreviousRoot() throws Exception {
        RuleChainId previousRoot = new RuleChainId(UUID.randomUUID());
        Edge follower = edge(profile.getId(), previousRoot);
        Edge overridden = edge(profile.getId(), rc2);
        when(edgeService.saveEdge(follower)).thenReturn(follower);

        assertThat(moveRoot(follower, rc1, previousRoot)).isTrue();
        assertThat(moveRoot(overridden, rc1, previousRoot)).isFalse();
        assertThat(moveRoot(follower, previousRoot, previousRoot)).isFalse();

        verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, follower.getId(), rc1);
        verify(ruleChainEdgeProcessor, never()).pushRootRuleChain(eq(TENANT_ID), eq(overridden.getId()), any());
    }

    @Test
    void applyToEdgeWithoutProfileIsANoop() throws Exception {
        Edge edge = edge(null, rc1);

        assertThat(applier.applyToEdge(TENANT_ID, edge.getId(), false, false, null)).isSameAs(edge);

        verify(edgeProfileService, never()).findEdgeProfileById(any(), any());
        verifyNoInteractions(relationService, ruleChainService, ruleChainEdgeProcessor);
    }

    @Test
    void applyToDeletedEdgeReturnsNull() throws Exception {
        assertThat(applier.applyToEdge(TENANT_ID, new EdgeId(UUID.randomUUID()), true, false, null)).isNull();

        verifyNoInteractions(relationService, ruleChainService, ruleChainEdgeProcessor);
    }

    @Test
    void applyToEdgeKeepsTheEdgeRootUnlessCreatedOrForced() throws Exception {
        Edge edge = edge(profile.getId(), rc2);
        when(relationService.getRelation(eq(TENANT_ID), eq(edge.getId()), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenReturn(ruleChainRelation(edge, rc1, profile.getId()));

        applier.applyToEdge(TENANT_ID, edge.getId(), false, false, null);

        verify(ruleChainEdgeProcessor, never()).pushRootRuleChain(any(), any(), any());
    }

    @Test
    void applyToEdgeSkipsMissingEntityGroupButAssignsOtherRows() throws Exception {
        EntityGroupId missingGroupId = new EntityGroupId(UUID.randomUUID());
        profile.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(EntityType.DEVICE, missingGroupId));
        Edge edge = edge(profile.getId(), null);
        when(edgeService.saveEdge(edge)).thenReturn(edge);

        assertThat(applier.applyToEdge(TENANT_ID, edge.getId(), true, false, null)).isEqualTo(edge);

        verify(entityGroupService, never()).assignEntityGroupToEdge(any(), any(), any(), any(), any());
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc1), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, edge.getId(), rc1);
    }

    @Test
    void applyToEdgeSkipsRuleChainDeletedWhileStillListedOnTheProfile() throws Exception {
        Edge edge = edge(profile.getId(), null);
        when(ruleChainService.findRuleChainById(TENANT_ID, rc2)).thenReturn(null);
        when(edgeService.saveEdge(edge)).thenReturn(edge);

        applier.applyToEdge(TENANT_ID, edge.getId(), true, false, null);

        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc1), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainService, never()).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(edge.getId()), any());
    }

    @Test
    void applyToEdgeDropsStampedCustomerSchedulerFromEdgeMovedToTheTenant() throws Exception {
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        SchedulerEventId eventId = new SchedulerEventId(UUID.randomUUID());
        SchedulerEventInfo event = new SchedulerEventInfo(eventId);
        event.setCustomerId(customerA);
        when(schedulerEventService.findSchedulerEventInfoById(TENANT_ID, eventId)).thenReturn(event);
        profile.getProfileData().getSchedulerEventIds().add(eventId);
        Edge movedToTenant = edge(profile.getId(), rc1);
        when(relationService.getRelation(eq(TENANT_ID), eq(movedToTenant.getId()), any(), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.EDGE)))
                .thenAnswer(inv -> new EntityRelation(movedToTenant.getId(), inv.getArgument(2), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EdgeProfileRows.stamp(profile.getId())));

        applier.applyAfterOwnerChange(TENANT_ID, movedToTenant.getId());

        verify(schedulerEventService).unassignSchedulerEventFromEdge(TENANT_ID, eventId, movedToTenant.getId());
        verify(schedulerEventService, never()).assignSchedulerEventToEdge(any(), any(), any(), any());
    }

    @Test
    void applyToEdgeDropsRowsStampedByThePreviousProfile() throws Exception {
        RuleChainId sharedRc = rc1;
        RuleChainId oldOnlyRc = new RuleChainId(UUID.randomUUID());
        RuleChainId handMadeRc = new RuleChainId(UUID.randomUUID());
        EdgeProfile previous = otherProfile();
        previous.setName("Old");
        previous.getProfileData().getRuleChainIds().add(sharedRc);
        previous.getProfileData().getRuleChainIds().add(oldOnlyRc);
        previous.getProfileData().getRuleChainIds().add(handMadeRc);
        previous.setRootRuleChainId(oldOnlyRc);
        Edge edge = edge(profile.getId(), rc1);
        relation(edge, sharedRc, ruleChainRelation(edge, sharedRc, previous.getId()));
        relation(edge, rc2, null);
        relation(edge, oldOnlyRc, ruleChainRelation(edge, oldOnlyRc, previous.getId()));
        relation(edge, handMadeRc, ruleChainRelation(edge, handMadeRc, null));

        applier.applyToEdge(TENANT_ID, edge.getId(), false, true, previous.getId());

        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(rc2), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainService).assignRuleChainToEdge(eq(TENANT_ID), eq(sharedRc), eq(edge.getId()), any(ObjectNode.class));
        verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, oldOnlyRc, edge.getId(), false);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(sharedRc), eq(edge.getId()), eq(false));
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(handMadeRc), eq(edge.getId()), eq(false));
        InOrder rootBeforeDrop = inOrder(ruleChainEdgeProcessor, ruleChainService);
        rootBeforeDrop.verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, edge.getId(), rc1);
        rootBeforeDrop.verify(ruleChainService).unassignRuleChainFromEdge(TENANT_ID, oldOnlyRc, edge.getId(), false);
    }

    @Test
    void reapplySkipsAnEdgeThatSwitchedAway() throws Exception {
        Edge edge = edge(new EdgeProfileId(UUID.randomUUID()), rc1);

        assertThat(applier.reapply(TENANT_ID, edge.getId(), profile.getId())).isFalse();

        verifyNoInteractions(relationService, ruleChainService, ruleChainEdgeProcessor);
    }

    @Test
    void reapplyGivesTheProfileRootOnlyToAnEdgeWithoutOne() throws Exception {
        Edge overridden = edge(profile.getId(), rc2);
        Edge rootless = edge(profile.getId(), null);
        when(relationService.findByFrom(eq(TENANT_ID), any(), eq(RelationTypeGroup.EDGE))).thenAnswer(inv -> {
            Edge edge = edges.get(inv.<EdgeId>getArgument(1));
            return List.of(ruleChainRelation(edge, rc1, profile.getId()), ruleChainRelation(edge, rc2, profile.getId()));
        });
        when(edgeService.saveEdge(rootless)).thenReturn(rootless);

        assertThat(applier.reapply(TENANT_ID, overridden.getId(), profile.getId())).isTrue();
        assertThat(applier.reapply(TENANT_ID, rootless.getId(), profile.getId())).isTrue();

        verify(ruleChainEdgeProcessor).pushRootRuleChain(TENANT_ID, rootless.getId(), rc1);
        verify(ruleChainEdgeProcessor, never()).pushRootRuleChain(eq(TENANT_ID), eq(overridden.getId()), any());
    }

    @Test
    void reapplyResendsRowsTheCloudAlreadyAssignsButNotHandMadeOnes() throws Exception {
        Edge edge = edge(profile.getId(), rc1);
        when(relationService.findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE)).thenReturn(List.of(
                ruleChainRelation(edge, rc1, profile.getId()),
                ruleChainRelation(edge, rc2, null)));

        applier.reapply(TENANT_ID, edge.getId(), profile.getId());

        ArgumentCaptor<EdgeEvent> resent = ArgumentCaptor.forClass(EdgeEvent.class);
        verify(edgeEventService).saveAsync(resent.capture());
        assertThat(resent.getValue().getType()).isEqualTo(EdgeEventType.RULE_CHAIN);
        assertThat(resent.getValue().getAction()).isEqualTo(EdgeEventActionType.ASSIGNED_TO_EDGE);
        assertThat(resent.getValue().getEntityId()).isEqualTo(rc1.getId());
        verify(ruleChainService, never()).assignRuleChainToEdge(any(), any(), any(), any());
    }

    @Test
    void reapplyReadsTheEdgeRelationsOnceAndResendsEveryStampedRow() throws Exception {
        Edge edge = edge(profile.getId(), rc1);
        when(relationService.findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE)).thenReturn(List.of(
                ruleChainRelation(edge, rc1, profile.getId()),
                ruleChainRelation(edge, rc2, profile.getId())));

        applier.reapply(TENANT_ID, edge.getId(), profile.getId());

        verify(relationService).findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE);
        verify(relationService, never()).getRelation(any(), any(), any(), any(), any());
        verify(edgeEventService, times(2)).saveAsync(any());
    }

    @Test
    void reapplyDropsRowsStampedByProfilesItIsNoLongerOnAndKeepsHandMadeRowsAndTheRoot() throws Exception {
        EntityGroupId strandedGroupId = new EntityGroupId(UUID.randomUUID());
        EntityGroupId handMadeGroupId = new EntityGroupId(UUID.randomUUID());
        RuleChainId overriddenRoot = new RuleChainId(UUID.randomUUID());
        Edge edge = edge(profile.getId(), overriddenRoot);
        when(relationService.findByFrom(TENANT_ID, edge.getId(), RelationTypeGroup.EDGE)).thenReturn(List.of(
                ruleChainRelation(edge, rc1, profile.getId()),
                ruleChainRelation(edge, rc2, profile.getId()),
                new EntityRelation(edge.getId(), strandedGroupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE,
                        EdgeProfileRows.stamp(new EdgeProfileId(UUID.randomUUID()))),
                new EntityRelation(edge.getId(), handMadeGroupId, DEVICE_GROUP_RELATION, RelationTypeGroup.EDGE),
                ruleChainRelation(edge, overriddenRoot, new EdgeProfileId(UUID.randomUUID()))));

        applier.reapply(TENANT_ID, edge.getId(), profile.getId());

        verify(entityGroupService).unassignEntityGroupFromEdge(TENANT_ID, strandedGroupId, edge.getId(), EntityType.DEVICE);
        verify(entityGroupService, never()).unassignEntityGroupFromEdge(TENANT_ID, handMadeGroupId, edge.getId(), EntityType.DEVICE);
        verify(ruleChainService, never()).unassignRuleChainFromEdge(eq(TENANT_ID), eq(overriddenRoot), any(), eq(false));
        assertUnstamped(edge, overriddenRoot);
    }

    @Test
    void jobRowRoundTrip() {
        EdgeAssignmentRowRef row = EdgeAssignmentRowRef.entityGroup(new EntityGroupId(UUID.randomUUID()), EntityType.DEVICE);
        EdgeProfileJobRow jobRow = JacksonUtil.fromString(JacksonUtil.toString(EdgeProfileRows.toJobRow(row)), EdgeProfileJobRow.class);

        assertThat(EdgeProfileRows.fromJobRow(jobRow)).isEqualTo(row);
    }

    @Test
    void unstampedKeepsOtherRelationInfo() {
        ObjectNode info = EdgeProfileRows.stamp(profile.getId());
        info.put("other", "value");
        EntityRelation relation = new EntityRelation(new EdgeId(UUID.randomUUID()), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, info);

        assertThat(EdgeProfileRows.unstamped(relation)).isEqualTo(JacksonUtil.newObjectNode().put("other", "value"));
        assertThat(relation.getAdditionalInfo().has(EdgeProfileRows.STAMP_KEY)).isTrue();
        relation.setAdditionalInfo(EdgeProfileRows.stamp(profile.getId()));
        assertThat(EdgeProfileRows.unstamped(relation)).isNull();
    }

    @Test
    void stampRoundTrip() {
        ObjectNode stamp = EdgeProfileRows.stamp(profile.getId());
        EntityRelation relation = new EntityRelation(new EdgeId(UUID.randomUUID()), rc1, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, stamp);
        assertThat(EdgeProfileRows.isStampedBy(relation, profile.getId())).isTrue();
        assertThat(EdgeProfileRows.isStampedBy(relation, new EdgeProfileId(UUID.randomUUID()))).isFalse();
        assertThat(EdgeProfileRows.isStampedBy(new EntityRelation(), profile.getId())).isFalse();
        assertThat(JacksonUtil.toString(stamp)).contains(profile.getId().getId().toString());
    }

}
