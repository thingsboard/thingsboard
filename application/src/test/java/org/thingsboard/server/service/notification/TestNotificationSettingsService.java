// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.notification.DefaultNotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserSettingsService;

@Service
@Primary
public class TestNotificationSettingsService extends DefaultNotificationSettingsService {

    public TestNotificationSettingsService(AdminSettingsService adminSettingsService,
                                           NotificationTargetService notificationTargetService,
                                           NotificationTemplateService notificationTemplateService,
                                           UserSettingsService userSettingsService) {
        super(adminSettingsService, notificationTargetService, notificationTemplateService, null, userSettingsService);
    }

    @Override
    public void createDefaultNotificationConfigs(TenantId tenantId) {
        // do nothing
    }

}
