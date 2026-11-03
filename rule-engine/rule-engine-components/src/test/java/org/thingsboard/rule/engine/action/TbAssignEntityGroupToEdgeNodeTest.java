// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Only what is specific to assigning. Everything shared with the unassign node — init validation, edge
 * resolution, group resolution, dedupe and row ordering — is covered once in the base class and runs here too.
 */
class TbAssignEntityGroupToEdgeNodeTest
        extends TbAbstractEdgeEntityGroupActionNodeTest<TbAssignEntityGroupToEdgeNodeConfiguration> {

    @Override
    protected TbAbstractEdgeEntityGroupActionNode<TbAssignEntityGroupToEdgeNodeConfiguration> newNode() {
        return new TbAssignEntityGroupToEdgeNode();
    }

    @Override
    protected TbAssignEntityGroupToEdgeNodeConfiguration newConfiguration() {
        return new TbAssignEntityGroupToEdgeNodeConfiguration().defaultConfiguration();
    }

    @Override
    protected void stubWillApply(EntityGroupId groupId, EntityType groupType) {
        when(entityGroupServiceMock.checkEntityGroupAssignedToEdgeAsync(TENANT_ID, EDGE_ID, groupId, groupType))
                .thenReturn(immediateFuture(false));
    }

    @Override
    protected void stubWillSkip(EntityGroupId groupId, EntityType groupType) {
        when(entityGroupServiceMock.checkEntityGroupAssignedToEdgeAsync(TENANT_ID, EDGE_ID, groupId, groupType))
                .thenReturn(immediateFuture(true));
    }

    @Override
    protected void verifyApplied(EntityGroupId groupId, EntityType groupType) {
        verify(entityGroupServiceMock).assignEntityGroupToEdge(TENANT_ID, groupId, EDGE_ID, groupType);
    }

    @Override
    protected void verifyAppliedInOrder(InOrder inOrder, EntityGroupId groupId, EntityType groupType) {
        inOrder.verify(entityGroupServiceMock).assignEntityGroupToEdge(TENANT_ID, groupId, EDGE_ID, groupType);
    }

    @Override
    protected void verifyNeverApplied() {
        verify(entityGroupServiceMock, never()).assignEntityGroupToEdge(any(), any(), any(), any());
    }

    @Test
    void givenCreateGroupIfNotExists_whenOnMsg_thenCreatesUnderEdgeOwner() throws TbNodeException {
        var configuration = newConfiguration();
        configuration.setCreateGroupIfNotExists(true);
        configuration.setEntityGroups(List.of(pattern("DEVICE", "Site Devices")));
        initWithConfig(configuration);
        stubEdgeById();
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(eq(TENANT_ID), any(), eq(EntityType.DEVICE), eq("Site Devices")))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.saveEntityGroup(eq(TENANT_ID), eq(CUSTOMER_ID), any(EntityGroup.class)))
                .thenReturn(group(DEVICE_GROUP_ID));
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // The new group lands under the edge owner, not the tenant the walk-up terminated at.
        var captor = ArgumentCaptor.forClass(EntityGroup.class);
        verify(entityGroupServiceMock).saveEntityGroup(eq(TENANT_ID), eq(CUSTOMER_ID), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Site Devices");
        assertThat(captor.getValue().getType()).isEqualTo(EntityType.DEVICE);
        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
    }

    @Test
    void givenCreateGroupIfNotExistsAndGroupUpTheChain_whenOnMsg_thenReusesItAndCreatesNothing() throws TbNodeException {
        var configuration = newConfiguration();
        configuration.setCreateGroupIfNotExists(true);
        configuration.setEntityGroups(List.of(pattern("DEVICE", "Site Devices")));
        initWithConfig(configuration);
        stubEdgeById();
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DEVICE, "Site Devices"))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Site Devices"))
                .thenReturn(Optional.of(group(DEVICE_GROUP_ID)));
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // Create-if-not-exists must reuse a group found further up the chain rather than shadowing it at the edge owner.
        verify(entityGroupServiceMock, never()).saveEntityGroup(any(), any(), any());
        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
    }

    @Test
    void givenUnresolvedPlaceholderInGroupName_whenOnMsgWithCreateEnabled_thenNeverCreatesGroup() throws TbNodeException {
        var configuration = newConfiguration();
        configuration.setCreateGroupIfNotExists(true);
        configuration.setEntityGroups(List.of(pattern("DEVICE", "${siteCode} Devices")));
        initWithConfig(configuration);
        stubEdgeById();

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // Without the unresolved-placeholder guard this would create a group literally named
        // "${siteCode} Devices" and report Success.
        assertFailedWith("Group name pattern '${siteCode} Devices' was not resolved!");
        verify(entityGroupServiceMock, never()).saveEntityGroup(any(), any(), any());
        verifyNeverApplied();
    }

    @Test
    void givenIndeterminateAssignmentCheck_whenOnMsg_thenStillAssigns() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        when(entityGroupServiceMock.checkEntityGroupAssignedToEdgeAsync(TENANT_ID, EDGE_ID, DEVICE_GROUP_ID, EntityType.DEVICE))
                .thenReturn(immediateFuture(null));

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // An unclear answer counts as "not assigned": for assign, a redundant re-assign is harmless.
        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
        verify(ctxMock).tellSuccess(any());
    }
}
