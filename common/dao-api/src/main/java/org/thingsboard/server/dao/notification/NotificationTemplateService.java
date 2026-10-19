// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationTemplateService {

    NotificationTemplate findNotificationTemplateById(TenantId tenantId, NotificationTemplateId id);

    NotificationTemplate saveNotificationTemplate(TenantId tenantId, NotificationTemplate notificationTemplate);

    PageData<NotificationTemplate> findNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, List<NotificationType> notificationTypes, PageLink pageLink);

    Optional<NotificationTemplate> findTenantOrSystemNotificationTemplate(TenantId tenantId, NotificationType notificationType);

    Optional<NotificationTemplate> findNotificationTemplateByTenantIdAndType(TenantId tenantId, NotificationType notificationType);

    int countNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, Collection<NotificationType> notificationTypes);

    void deleteNotificationTemplateById(TenantId tenantId, NotificationTemplateId id);

    void deleteNotificationTemplatesByTenantId(TenantId tenantId);

}
