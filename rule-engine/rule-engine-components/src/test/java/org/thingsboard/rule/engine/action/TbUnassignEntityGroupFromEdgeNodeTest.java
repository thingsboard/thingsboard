// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Only what is specific to unassigning. Everything shared with the assign node is covered once in the base
 * class and runs here too.
 */
class TbUnassignEntityGroupFromEdgeNodeTest
        extends TbAbstractEdgeEntityGroupActionNodeTest<TbUnassignEntityGroupFromEdgeNodeConfiguration> {

    @Override
    protected TbAbstractEdgeEntityGroupActionNode<TbUnassignEntityGroupFromEdgeNodeConfiguration> newNode() {
        return new TbUnassignEntityGroupFromEdgeNode();
    }

    @Override
    protected TbUnassignEntityGroupFromEdgeNodeConfiguration newConfiguration() {
        return new TbUnassignEntityGroupFromEdgeNodeConfiguration().defaultConfiguration();
    }

    @Override
    protected void stubWillApply(EntityGroupId groupId, EntityType groupType) {
        when(entityGroupServiceMock.checkEntityGroupAssignedToEdgeAsync(TENANT_ID, EDGE_ID, groupId, groupType))
                .thenReturn(immediateFuture(true));
    }

    @Override
    protected void stubWillSkip(EntityGroupId groupId, EntityType groupType) {
        when(entityGroupServiceMock.checkEntityGroupAssignedToEdgeAsync(TENANT_ID, EDGE_ID, groupId, groupType))
                .thenReturn(immediateFuture(false));
    }

    @Override
    protected void verifyApplied(EntityGroupId groupId, EntityType groupType) {
        verify(entityGroupServiceMock).unassignEntityGroupFromEdge(TENANT_ID, groupId, EDGE_ID, groupType);
    }

    @Override
    protected void verifyAppliedInOrder(InOrder inOrder, EntityGroupId groupId, EntityType groupType) {
        inOrder.verify(entityGroupServiceMock).unassignEntityGroupFromEdge(TENANT_ID, groupId, EDGE_ID, groupType);
    }

    @Override
    protected void verifyNeverApplied() {
        verify(entityGroupServiceMock, never()).unassignEntityGroupFromEdge(any(), any(), any(), any());
    }

    @Test
    void givenIndeterminateAssignmentCheck_whenOnMsg_thenNeverUnassigns() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        when(entityGroupServiceMock.checkEntityGroupAssignedToEdgeAsync(TENANT_ID, EDGE_ID, DEVICE_GROUP_ID, EntityType.DEVICE))
                .thenReturn(immediateFuture(null));

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // Unassign takes the opposite safe direction from assign: never a destructive removal on an
        // indeterminate check.
        verifyNeverApplied();
        verify(ctxMock).tellSuccess(any());
    }

    @Test
    void givenMissingGroup_whenOnMsg_thenNeverCreatesIt() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DEVICE, "Site Devices"))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Site Devices"))
                .thenReturn(Optional.empty());

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // This node never creates entity groups, whatever the configuration says.
        verify(entityGroupServiceMock, never()).saveEntityGroup(any(), any(), any());
        verifyNeverApplied();
    }
}
