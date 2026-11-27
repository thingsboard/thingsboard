// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.notification.NotificationSettingsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V4_4_2_0MigrationTest {

    @Mock
    private NotificationSettingsService notificationSettingsService;
    @Mock
    private DefaultNotifications defaultNotifications;

    @InjectMocks
    private V4_4_2_0Migration migration;

    @Test
    void hasItsVersion() {
        assertEquals("4.4.2.0", migration.getVersion());
    }

    @Test
    void createsAiCreditsLimitIncreaseRequestTemplateWhenMissing() {
        when(notificationSettingsService.isNotificationConfigured(TenantId.SYS_TENANT_ID, NotificationType.AI_CREDITS_LIMIT_INCREASE_REQUEST))
                .thenReturn(false);

        migration.apply();

        verify(defaultNotifications).create(TenantId.SYS_TENANT_ID, DefaultNotifications.aiCreditsLimitIncreaseRequest);
    }

    @Test
    void keepsAiCreditsLimitIncreaseRequestTemplateWhenPresent() {
        when(notificationSettingsService.isNotificationConfigured(TenantId.SYS_TENANT_ID, NotificationType.AI_CREDITS_LIMIT_INCREASE_REQUEST))
                .thenReturn(true);

        migration.apply();

        verify(defaultNotifications, never()).create(any(), any());
    }
}
