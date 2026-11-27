// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Coverage for the conditional state write. The status poller and the reconciler both derive a state
 * from a snapshot taken before a slow orchestrator call, so their write must not overwrite a user
 * transition committed inside that window — it is applied only while the row still holds the state the
 * decision was based on, and reported as not applied otherwise.
 */
class DefaultExtensionServiceStateTest {

    private ExtensionDao extensionDao;
    private DefaultExtensionService service;
    private TenantId tenantId;
    private ExtensionId extensionId;

    @BeforeEach
    void setUp() {
        extensionDao = mock(ExtensionDao.class);
        service = new DefaultExtensionService(extensionDao, null, null, mock(ApiLimitService.class), null);
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        extensionId = new ExtensionId(UUID.randomUUID());
    }

    @Test
    void appliesTheWriteWhileTheRowStillHoldsTheExpectedState() {
        when(extensionDao.updateStateIf(any(), any(), any(), any(), any(), any())).thenReturn(true);

        boolean applied = service.updateStateIf(tenantId, extensionId,
                ExtensionState.PROVISIONING, ExtensionState.ACTIVE, null);

        assertThat(applied).isTrue();
        // no error to store, so no error timestamp either
        verify(extensionDao).updateStateIf(tenantId, extensionId,
                ExtensionState.PROVISIONING, ExtensionState.ACTIVE, null, null);
    }

    @Test
    void reportsNotAppliedWhenTheRowHasMovedOn() {
        // e.g. the user pressed Stop while the poller was inside describe()
        when(extensionDao.updateStateIf(any(), any(), any(), any(), any(), any())).thenReturn(false);

        boolean applied = service.updateStateIf(tenantId, extensionId,
                ExtensionState.PROVISIONING, ExtensionState.ACTIVE, null);

        assertThat(applied).isFalse();
    }

    @Test
    void storesAnErrorTimestampAlongsideTheError() {
        when(extensionDao.updateStateIf(any(), any(), any(), any(), any(), any())).thenReturn(true);
        long before = System.currentTimeMillis();

        service.updateStateIf(tenantId, extensionId, ExtensionState.PROVISIONING, ExtensionState.FAILED,
                "apply failed: boom");

        verify(extensionDao).updateStateIf(eq(tenantId), eq(extensionId), eq(ExtensionState.PROVISIONING),
                eq(ExtensionState.FAILED), eq("apply failed: boom"),
                org.mockito.ArgumentMatchers.longThat(ts -> ts >= before));
    }

    @Test
    void unconditionalUpdateStateIsLeftForExplicitUserTransitions() {
        // start/stop are the user's own decision on the current row, so they are not conditional;
        // guard against the CAS path being wired in there by accident.
        when(extensionDao.findByTenantIdAndId(tenantId, extensionId)).thenReturn(null);

        service.updateState(tenantId, extensionId, ExtensionState.STOPPED, null);

        verify(extensionDao, org.mockito.Mockito.never())
                .updateStateIf(any(), any(), any(), any(), isNull(), isNull());
    }

}
