// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;

public interface NotificationSettingsService {

    void saveNotificationSettings(TenantId tenantId, NotificationSettings settings);

    NotificationSettings findNotificationSettings(TenantId tenantId);

    void deleteNotificationSettings(TenantId tenantId);

    UserNotificationSettings saveUserNotificationSettings(TenantId tenantId, UserId userId, UserNotificationSettings settings);

    UserNotificationSettings getUserNotificationSettings(TenantId tenantId, UserId userId, boolean format);

    void createDefaultNotificationConfigs(TenantId tenantId);

    void updateDefaultNotificationConfigs(TenantId tenantId);

    boolean isNotificationConfigured(TenantId tenantId, NotificationType notificationType);

    void createSystemNotificationTemplate(TenantId tenantId, NotificationTemplate template);

}
