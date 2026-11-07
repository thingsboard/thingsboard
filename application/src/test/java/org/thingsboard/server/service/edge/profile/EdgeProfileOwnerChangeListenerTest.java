// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeProfileOwnerChangeListenerTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    EdgeProfileJobService edgeProfileJobService;
    @Mock
    OwnersCacheService ownersCacheService;
    @InjectMocks
    EdgeProfileOwnerChangeListener listener;

    @Test
    void customerOwnerChangeSubmitsAJobForTheOwnersItLeftAndJoined() {
        CustomerId moved = new CustomerId(UUID.randomUUID());
        CustomerId formerParent = new CustomerId(UUID.randomUUID());
        CustomerId newParent = new CustomerId(UUID.randomUUID());
        when(ownersCacheService.getOwner(TENANT_ID, moved)).thenReturn(newParent);
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, formerParent)).thenReturn(Set.of(formerParent, TENANT_ID));
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, newParent)).thenReturn(Set.of(newParent, TENANT_ID));

        listener.onOwnerChanged(ownerChanged(moved, formerParent));

        verify(edgeProfileJobService).submitOwnerChange(TENANT_ID, moved, Set.of(formerParent, newParent));
    }

    @Test
    void customerOwnerChangeWithinTheSameChainSubmitsNothing() {
        CustomerId moved = new CustomerId(UUID.randomUUID());
        when(ownersCacheService.getOwner(TENANT_ID, moved)).thenReturn(TENANT_ID);
        when(ownersCacheService.fetchOwnersHierarchy(TENANT_ID, TENANT_ID)).thenReturn(Set.of(TENANT_ID));

        listener.onOwnerChanged(ownerChanged(moved, TENANT_ID));

        verify(edgeProfileJobService, never()).submitOwnerChange(any(), any(), any());
    }

    @Test
    void edgeOwnerChangeSubmitsAJobForTheMovedEdge() {
        EdgeId moved = new EdgeId(UUID.randomUUID());

        listener.onOwnerChanged(ownerChanged(moved, new CustomerId(UUID.randomUUID())));

        verify(edgeProfileJobService).submitEdgeOwnerChange(TENANT_ID, moved);
        verify(edgeProfileJobService, never()).submitOwnerChange(any(), any(), any());
    }

    @Test
    void otherActionsAreIgnored() {
        listener.onOwnerChanged(ActionEntityEvent.builder().tenantId(TENANT_ID).entityId(new EdgeId(UUID.randomUUID()))
                .actionType(ActionType.UPDATED).build());

        verifyNoInteractions(edgeProfileJobService, ownersCacheService);
    }

    private ActionEntityEvent<?> ownerChanged(EntityId movedId, EntityId formerOwnerId) {
        return ActionEntityEvent.builder().tenantId(TENANT_ID).entityId(movedId)
                .body(JacksonUtil.toString(formerOwnerId)).actionType(ActionType.CHANGE_OWNER).build();
    }

}
