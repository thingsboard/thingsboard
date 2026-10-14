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
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeEntityProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
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

    private EdgeEntityProcessor processor;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());

    @BeforeEach
    public void setUp() {
        processor = spy(new EdgeEntityProcessor());
        ReflectionTestUtils.setField(processor, "edgeCtx", edgeCtx);
        ReflectionTestUtils.setField(processor, "dbCallbackExecutorService", dbCallbackExecutorService);
        // Run all listenable-future callbacks inline so ordering is deterministic.
        doAnswer(inv -> {
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

}
