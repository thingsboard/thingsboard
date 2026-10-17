// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityGroupResolverTest {

    static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("8f88de53-4e79-4bb1-bc70-946d9a869458"));
    static final CustomerId CUSTOMER_ID = new CustomerId(UUID.fromString("3a2f2b91-4e58-4b6c-9f4d-6b8f4e2c1a77"));
    static final EntityGroupId GROUP_ID = new EntityGroupId(UUID.fromString("bd0d1f2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f"));
    static final String GROUP_NAME = "Site Devices";

    @Mock
    TbContext ctxMock;
    @Mock
    TbPeContext peCtxMock;
    @Mock
    EntityGroupService entityGroupServiceMock;

    @BeforeEach
    void setUp() {
        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        lenient().when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);
    }

    @Test
    void givenGroupAtStartingOwner_whenResolve_thenDoesNotWalkUp() {
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DEVICE, GROUP_NAME))
                .thenReturn(Optional.of(group(GROUP_ID)));

        var resolver = new EntityGroupResolver(ctxMock);

        assertThat(resolver.resolve(CUSTOMER_ID, EntityType.DEVICE, GROUP_NAME)).isEqualTo(GROUP_ID);
        verify(peCtxMock, times(0)).getOwner(any(), any());
    }

    @Test
    void givenGroupOnlyAtTenantLevel_whenResolve_thenWalksUpFromCustomer() {
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DEVICE, GROUP_NAME))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, EntityType.DEVICE, GROUP_NAME))
                .thenReturn(Optional.of(group(GROUP_ID)));

        var resolver = new EntityGroupResolver(ctxMock);

        assertThat(resolver.resolve(CUSTOMER_ID, EntityType.DEVICE, GROUP_NAME)).isEqualTo(GROUP_ID);
    }

    @Test
    void givenCyclicOwnerChain_whenResolve_thenThrowsInsteadOfSpinning() {
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DEVICE, GROUP_NAME))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(CUSTOMER_ID);

        var resolver = new EntityGroupResolver(ctxMock);

        assertThatThrownBy(() -> resolver.resolve(CUSTOMER_ID, EntityType.DEVICE, GROUP_NAME))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Owner hierarchy for " + CUSTOMER_ID + " exceeded the maximum depth of 64!");
    }

    @Test
    void givenMissingGroup_whenResolve_thenThrows() {
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(eq(TENANT_ID), any(EntityId.class), eq(EntityType.DEVICE), eq(GROUP_NAME)))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);

        var resolver = new EntityGroupResolver(ctxMock);

        assertThatThrownBy(() -> resolver.resolve(CUSTOMER_ID, EntityType.DEVICE, GROUP_NAME))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No entity group found with type 'DEVICE' and name 'Site Devices' for owner " + CUSTOMER_ID + "!");
    }

    @Test
    void givenMissingGroup_whenResolveOrCreate_thenCreatesUnderStartingOwner() {
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(eq(TENANT_ID), any(EntityId.class), eq(EntityType.ASSET), eq(GROUP_NAME)))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.saveEntityGroup(eq(TENANT_ID), eq(CUSTOMER_ID), any(EntityGroup.class)))
                .thenReturn(group(GROUP_ID));

        var resolver = new EntityGroupResolver(ctxMock);

        assertThat(resolver.resolveOrCreate(CUSTOMER_ID, EntityType.ASSET, GROUP_NAME)).isEqualTo(GROUP_ID);

        ArgumentCaptor<EntityGroup> captor = ArgumentCaptor.forClass(EntityGroup.class);
        verify(entityGroupServiceMock).saveEntityGroup(eq(TENANT_ID), eq(CUSTOMER_ID), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(GROUP_NAME);
        assertThat(captor.getValue().getType()).isEqualTo(EntityType.ASSET);
    }

    private static EntityGroup group(EntityGroupId id) {
        var group = new EntityGroup();
        group.setId(id);
        return group;
    }
}
