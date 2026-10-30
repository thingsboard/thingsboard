// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.wl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;

import java.util.UUID;

public class WhiteLabelingEdgeProcessorTest {

    private final WhiteLabelingEdgeProcessor processor = new WhiteLabelingEdgeProcessor();
    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @Test
    public void testSystemMailTemplatesAreNotSyncedToEdge() {
        // System mail templates are rendered on the cloud, so they must never be pushed to the edge.
        Assertions.assertTrue(isSystemMailTemplates(WhiteLabelingType.MAIL_TEMPLATES, TenantId.SYS_TENANT_ID));
    }

    @Test
    public void testTenantMailTemplatesAreStillSyncedToEdge() {
        // Only system-scoped mail templates are skipped; tenant-scoped ones keep syncing.
        Assertions.assertFalse(isSystemMailTemplates(WhiteLabelingType.MAIL_TEMPLATES, tenantId));
    }

    @Test
    public void testSystemNonMailTemplatesAreStillSyncedToEdge() {
        // Other system white-labeling (e.g. GENERAL) is not affected by the mail-templates skip.
        Assertions.assertFalse(isSystemMailTemplates(WhiteLabelingType.GENERAL, TenantId.SYS_TENANT_ID));
    }

    private boolean isSystemMailTemplates(WhiteLabelingType type, TenantId tenantId) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(processor, "isSystemMailTemplates", type, tenantId));
    }

}
