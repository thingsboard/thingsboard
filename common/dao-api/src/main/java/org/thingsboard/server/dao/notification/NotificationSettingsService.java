// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;

import java.util.Map;

public interface NotificationSettingsService {

    void saveNotificationSettings(TenantId tenantId, NotificationSettings settings);

    NotificationSettings findNotificationSettings(TenantId tenantId);

    void deleteNotificationSettings(TenantId tenantId);

    UserNotificationSettings saveUserNotificationSettings(TenantId tenantId, UserId userId, UserNotificationSettings settings);

    UserNotificationSettings getUserNotificationSettings(TenantId tenantId, UserId userId, boolean format);

    void createDefaultNotificationConfigs(TenantId tenantId);

    void updateDefaultNotificationConfigs(TenantId tenantId);

    void moveMailTemplatesToNotificationCenter(TenantId tenantId, JsonNode mailTemplates, Map<String, NotificationType> mailTemplatesNames);

}
