// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies that lifecycle actions are forwarded to the cluster instead of being handled only on the
 * local node: stop/start broadcast an UPDATED lifecycle event after the state is committed, and
 * restart broadcasts the explicit restart marker (a plain re-apply would be a spec-hash no-op).
 */
class DefaultTbExtensionServiceTest {

    private final ExtensionService extensionService = mock(ExtensionService.class);
    private final TbClusterService tbClusterService = mock(TbClusterService.class);

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final ExtensionId extensionId = new ExtensionId(UUID.randomUUID());

    private DefaultTbExtensionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new DefaultTbExtensionService(extensionService);
        ReflectionTestUtils.setField(service, "tbClusterService", tbClusterService);
        ReflectionTestUtils.setField(service, "logEntityActionService", mock(TbLogEntityActionService.class));
        user = new User(new UserId(UUID.randomUUID()));
        user.setTenantId(tenantId);
    }

    private Extension extension(ExtensionState state) {
        Extension ext = new Extension(extensionId);
        ext.setTenantId(tenantId);
        ext.setSlug("whoami-1d61");
        ext.setState(state);
        return ext;
    }

    @Test
    void stopBroadcastsUpdatedEventAfterStateIsCommitted() {
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(extension(ExtensionState.STOPPED));

        service.stop(extension(ExtensionState.ACTIVE), user);

        InOrder inOrder = inOrder(extensionService, tbClusterService);
        inOrder.verify(extensionService).updateState(tenantId, extensionId, ExtensionState.STOPPED, null);
        inOrder.verify(tbClusterService).broadcastEntityStateChangeEvent(tenantId, extensionId, ComponentLifecycleEvent.UPDATED);
    }

    // Resuming re-consumes quota, so start must go through the guarded check-and-flip rather than a bare
    // capacity check followed by an unguarded updateState — otherwise two concurrent starts both pass.
    @Test
    void startBroadcastsUpdatedEventAfterStateIsCommitted() {
        Extension stopped = extension(ExtensionState.STOPPED);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(extension(ExtensionState.PROVISIONING));

        service.start(stopped, user);

        InOrder inOrder = inOrder(extensionService, tbClusterService);
        inOrder.verify(extensionService).startWithinQuota(tenantId, stopped);
        inOrder.verify(tbClusterService).broadcastEntityStateChangeEvent(tenantId, extensionId, ComponentLifecycleEvent.UPDATED);
        verify(extensionService, never()).updateState(any(), any(), eq(ExtensionState.PROVISIONING), any());
    }

    // A STOPPED extension is scaled to zero, so restartQuietly re-reads the row and returns without
    // acting. Accepting the call would hand the caller a 202 and a successful audit entry for work that
    // never happens, with no hint that /start was the operation they wanted.
    @Test
    void restartIsRejectedForAStoppedExtension() {
        assertThatThrownBy(() -> service.restart(extension(ExtensionState.STOPPED), user))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("start it instead");

        verifyNoInteractions(tbClusterService);
    }

    @Test
    void restartBroadcastsLifecycleMsgWithRestartMarker() {
        service.restart(extension(ExtensionState.ACTIVE), user);

        ArgumentCaptor<ComponentLifecycleMsg> captor = ArgumentCaptor.forClass(ComponentLifecycleMsg.class);
        verify(tbClusterService).broadcast(captor.capture());
        ComponentLifecycleMsg msg = captor.getValue();
        assertThat(msg.getTenantId()).isEqualTo(tenantId);
        assertThat(msg.getEntityId()).isEqualTo(extensionId);
        assertThat(msg.getEvent()).isEqualTo(ComponentLifecycleEvent.UPDATED);
        assertThat(msg.getInfo()).isNotNull();
        assertThat(msg.getInfo().path("action").asText()).isEqualTo("restart");
    }

}
