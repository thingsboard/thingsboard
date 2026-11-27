// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_4_2_0Migration implements Migration {

    private final NotificationSettingsService notificationSettingsService;
    private final DefaultNotifications defaultNotifications;

    @Override
    public String getVersion() {
        return "4.4.2.0";
    }

    @Override
    public void apply() {
        createAiCreditsLimitIncreaseRequestTemplate();
    }

    private void createAiCreditsLimitIncreaseRequestTemplate() {
        if (notificationSettingsService.isNotificationConfigured(TenantId.SYS_TENANT_ID, NotificationType.AI_CREDITS_LIMIT_INCREASE_REQUEST)) {
            log.debug("The AI credits limit increase request notification template already exists");
            return;
        }
        defaultNotifications.create(TenantId.SYS_TENANT_ID, DefaultNotifications.aiCreditsLimitIncreaseRequest);
        log.info("Created the AI credits limit increase request notification template");
    }
}
