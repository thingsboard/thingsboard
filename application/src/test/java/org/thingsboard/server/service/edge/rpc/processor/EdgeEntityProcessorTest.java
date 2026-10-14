// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.profile.EdgeProfileApplier;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeEntityProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EdgeEntityProcessorTest {

    @Mock
    private EdgeContextComponent edgeCtx;
    @Mock
    private DbCallbackExecutorService dbCallbackExecutorService;
    @Mock
    private RoleService roleService;
    @Mock
    private EntityGroupService entityGroupService;
    @Mock
    private SchedulerEventService schedulerEventService;
    @Mock
    private RelationService relationService;
    @Mock
    private OwnersCacheService ownersCacheService;

    private EdgeEntityProcessor processor;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());

    @BeforeEach
    public void setUp() {
        processor = spy(new EdgeEntityProcessor());
        ReflectionTestUtils.setField(processor, "edgeCtx", edgeCtx);
        ReflectionTestUtils.setField(processor, "dbCallbackExecutorService", dbCallbackExecutorService);
        ReflectionTestUtils.setField(processor, "ownersCacheService", ownersCacheService);
        // Run all listenable-future callbacks inline so ordering is deterministic.
        lenient().doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(dbCallbackExecutorService).execute(any(Runnable.class));
    }

    // Deterministic guard for customer-before-roles ordering: unlike the end-to-end CustomerEdgeTest, it asserts
    // the enqueue order directly, so it fails reliably on the pre-fix race (at the cost of coupling to saveEdgeEvent).
    @Test
    public void syncCustomerSendsCustomerBeforeRolesToAvoidEdgeRejection() {
        Customer customer = new Customer();
        customer.setId(new CustomerId(UUID.randomUUID()));
        customer.setTenantId(tenantId);

        Role role1 = new Role();
        role1.setId(new RoleId(UUID.randomUUID()));
        Role role2 = new Role();
        role2.setId(new RoleId(UUID.randomUUID()));

        when(edgeCtx.getRoleService()).thenReturn(roleService);
        when(roleService.findRolesByTenantIdAndCustomerId(eq(tenantId), eq(customer.getId()), any()))
                .thenReturn(new PageData<>(List.of(role1, role2), 1, 2, false));
        when(edgeCtx.getEntityGroupService()).thenReturn(entityGroupService);
        when(entityGroupService.findOrCreateCustomerAdminsGroup(any(), any(), any())).thenReturn(new EntityGroup());
        when(entityGroupService.findOrCreateCustomerUsersGroup(any(), any(), any())).thenReturn(new EntityGroup());

        // The CUSTOMER-edge event is held open; the ROLE events must not be enqueued until it completes.
        SettableFuture<Void> customerEventSaved = SettableFuture.create();
        doReturn(Futures.immediateFuture(null))
                .when(processor).saveEdgeEvent(any(), any(), any(), any(), any(), any(), any());
        doReturn(customerEventSaved)
                .when(processor).saveEdgeEvent(any(), any(), eq(EdgeEventType.CUSTOMER), any(), any(), any(), any());

        ReflectionTestUtils.invokeMethod(processor, "syncCustomer", tenantId, edgeId, customer);

        // Customer event not yet persisted -> roles must NOT have been enqueued. (Old code fails here.)
        verify(processor, never()).saveEdgeEvent(any(), any(), eq(EdgeEventType.ROLE), any(), any(), any(), any());

        // Customer event completes -> roles are now enqueued.
        customerEventSaved.set(null);
        verify(processor, times(2)).saveEdgeEvent(any(), any(), eq(EdgeEventType.ROLE), any(), any(), any(), any());
    }

    @Test
    public void edgeMovedUpToTenantKeepsCustomerDeviceGroupButDropsCustomerUsersAndScheduler() {
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        Edge edge = new Edge(edgeId);
        edge.setTenantId(tenantId);
        edge.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        edge.setEdgeProfileId(new EdgeProfileId(UUID.randomUUID()));
        EntityGroup devices = new EntityGroup(new EntityGroupId(UUID.randomUUID()));
        devices.setType(EntityType.DEVICE);
        devices.setOwnerId(customerA);
        EntityGroup users = new EntityGroup(new EntityGroupId(UUID.randomUUID()));
        users.setType(EntityType.USER);
        users.setOwnerId(customerA);
        SchedulerEventInfo event = new SchedulerEventInfo(new SchedulerEventId(UUID.randomUUID()));
        event.setCustomerId(customerA);

        when(edgeCtx.getEntityGroupService()).thenReturn(entityGroupService);
        when(entityGroupService.findEdgeEntityGroupsByOwnerIdAndType(eq(tenantId), eq(edgeId), eq(customerA), any(), any()))
                .thenAnswer(inv -> switch ((EntityType) inv.getArgument(3)) {
                    case DEVICE -> new PageData<>(List.of(devices), 1, 1, false);
                    case USER -> new PageData<>(List.of(users), 1, 1, false);
                    default -> new PageData<>(List.of(), 0, 0, false);
                });
        when(edgeCtx.getSchedulerEventService()).thenReturn(schedulerEventService);
        when(schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(eq(tenantId), eq(edgeId), eq(customerA), any()))
                .thenReturn(new PageData<>(List.of(event), 1, 1, false));
        // every row was stamped by the edge's own profile, so only eligibility decides what stays
        when(edgeCtx.getRelationService()).thenReturn(relationService);
        when(relationService.getRelation(eq(tenantId), eq(edgeId), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenAnswer(inv -> new EntityRelation(edgeId, inv.getArgument(2), inv.getArgument(3), RelationTypeGroup.EDGE,
                        EdgeProfileApplier.stamp(edge.getEdgeProfileId())));
        when(edgeCtx.getEdgeProfileApplier()).thenReturn(new EdgeProfileApplier(null, null, null, null, null, null, null, null, null, null));
        when(ownersCacheService.getChildOwners(tenantId, customerA)).thenReturn(Set.of(customerA));

        Object rows = ReflectionTestUtils.invokeMethod(processor, "collectRowsOfRemovedCustomers", tenantId, edge, List.of(customerA));

        // the tenant edge never holds Customer A, so a re-sent scheduler or user would be rejected there
        List<?> kept = ReflectionTestUtils.invokeMethod(rows, "kept");
        Map<EntityType, List<EntityGroup>> groupsToRemove = ReflectionTestUtils.invokeMethod(rows, "groupsToRemove");
        List<SchedulerEventInfo> schedulerEventsToRemove = ReflectionTestUtils.invokeMethod(rows, "schedulerEventsToRemove");
        assertThat(kept).hasSize(1);
        assertThat((Object) ReflectionTestUtils.invokeMethod(kept.get(0), "entityId")).isEqualTo(devices.getId());
        assertThat(groupsToRemove.get(EntityType.USER)).containsExactly(users);
        assertThat(groupsToRemove.get(EntityType.DEVICE)).isEmpty();
        assertThat(schedulerEventsToRemove).containsExactly(event);
    }

    @Test
    public void edgeMovedUpToTenantAlsoResolvesRowsOfSubCustomersTheEdgeDeletesWithTheCustomer() {
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        CustomerId subA1 = new CustomerId(UUID.randomUUID());
        Edge edge = new Edge(edgeId);
        edge.setTenantId(tenantId);
        edge.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        edge.setEdgeProfileId(new EdgeProfileId(UUID.randomUUID()));
        EntityGroup profileDevices = new EntityGroup(new EntityGroupId(UUID.randomUUID()));
        profileDevices.setType(EntityType.DEVICE);
        profileDevices.setOwnerId(subA1);
        EntityGroup manualAssets = new EntityGroup(new EntityGroupId(UUID.randomUUID()));
        manualAssets.setType(EntityType.ASSET);
        manualAssets.setOwnerId(subA1);

        when(edgeCtx.getEntityGroupService()).thenReturn(entityGroupService);
        when(entityGroupService.findEdgeEntityGroupsByOwnerIdAndType(eq(tenantId), eq(edgeId), any(), any(), any()))
                .thenAnswer(inv -> {
                    if (!subA1.equals(inv.getArgument(2))) {
                        return new PageData<>(List.of(), 0, 0, false);
                    }
                    return switch ((EntityType) inv.getArgument(3)) {
                        case DEVICE -> new PageData<>(List.of(profileDevices), 1, 1, false);
                        case ASSET -> new PageData<>(List.of(manualAssets), 1, 1, false);
                        default -> new PageData<>(List.of(), 0, 0, false);
                    };
                });
        when(edgeCtx.getSchedulerEventService()).thenReturn(schedulerEventService);
        when(schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(eq(tenantId), eq(edgeId), any(), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));
        when(edgeCtx.getRelationService()).thenReturn(relationService);
        when(relationService.getRelation(eq(tenantId), eq(edgeId), any(), any(), eq(RelationTypeGroup.EDGE)))
                .thenAnswer(inv -> profileDevices.getId().equals(inv.getArgument(2))
                        ? new EntityRelation(edgeId, inv.getArgument(2), inv.getArgument(3), RelationTypeGroup.EDGE, EdgeProfileApplier.stamp(edge.getEdgeProfileId()))
                        : new EntityRelation(edgeId, inv.getArgument(2), inv.getArgument(3), RelationTypeGroup.EDGE));
        lenient().when(edgeCtx.getEdgeProfileApplier()).thenReturn(new EdgeProfileApplier(null, null, null, null, null, null, null, null, null, null));
        when(ownersCacheService.getChildOwners(tenantId, customerA)).thenReturn(new LinkedHashSet<>(List.of(customerA, subA1)));

        Object rows = ReflectionTestUtils.invokeMethod(processor, "collectRowsOfRemovedCustomers", tenantId, edge, List.of(customerA));

        // the edge deletes Sub A1 together with Customer A: its profile row still fits the tenant edge and is re-sent,
        // its hand-made row is unassigned like one of Customer A itself, instead of staying assigned to nothing on the edge
        List<?> kept = ReflectionTestUtils.invokeMethod(rows, "kept");
        Map<EntityType, List<EntityGroup>> groupsToRemove = ReflectionTestUtils.invokeMethod(rows, "groupsToRemove");
        assertThat(kept).hasSize(1);
        assertThat((Object) ReflectionTestUtils.invokeMethod(kept.get(0), "entityId")).isEqualTo(profileDevices.getId());
        assertThat(groupsToRemove.get(EntityType.ASSET)).containsExactly(manualAssets);
        assertThat(groupsToRemove.get(EntityType.DEVICE)).isEmpty();
    }

    @Test
    public void removedCustomerRowsAreReadOnlyAfterTheCustomerDeletesArePersisted() {
        CustomerId customerA = new CustomerId(UUID.randomUUID());
        Customer customer = new Customer();
        customer.setId(customerA);
        Edge edge = new Edge(edgeId);
        edge.setTenantId(tenantId);
        edge.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        edge.setEdgeProfileId(new EdgeProfileId(UUID.randomUUID()));

        lenient().when(edgeCtx.getEntityGroupService()).thenReturn(entityGroupService);
        lenient().when(entityGroupService.findEdgeEntityGroupsByOwnerIdAndType(any(), any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));
        lenient().when(edgeCtx.getSchedulerEventService()).thenReturn(schedulerEventService);
        lenient().when(schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));
        lenient().when(ownersCacheService.getChildOwners(tenantId, customerA)).thenReturn(Set.of(customerA));

        SettableFuture<Void> customerDeleteSaved = SettableFuture.create();
        doReturn(customerDeleteSaved)
                .when(processor).saveEdgeEvent(any(), any(), eq(EdgeEventType.CUSTOMER), eq(EdgeEventActionType.DELETED), any(), any());

        ReflectionTestUtils.invokeMethod(processor, "removeCustomers", tenantId, edge, List.of(customer));

        // a row the profile applier assigns before the delete is persisted would otherwise be missed, and its assignment
        // could reach the edge ahead of the delete that wipes it
        verify(ownersCacheService, never()).getChildOwners(any(), any());
        verify(entityGroupService, never()).findEdgeEntityGroupsByOwnerIdAndType(any(), any(), any(), any(), any());

        customerDeleteSaved.set(null);
        verify(ownersCacheService).getChildOwners(tenantId, customerA);
        verify(entityGroupService, times(EdgeProfileData.SYNCABLE_GROUP_TYPES.size()))
                .findEdgeEntityGroupsByOwnerIdAndType(eq(tenantId), eq(edgeId), eq(customerA), any(), any());
    }

}
