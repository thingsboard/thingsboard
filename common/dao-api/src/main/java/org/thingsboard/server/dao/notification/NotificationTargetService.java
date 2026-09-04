// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface NotificationTargetService {

    NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget);

    NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id);

    PageData<NotificationTarget> findNotificationTargetsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<NotificationTarget> findNotificationTargetsByTenantIdAndSupportedNotificationType(TenantId tenantId, NotificationType notificationType, PageLink pageLink);

    List<NotificationTarget> findNotificationTargetsByTenantIdAndIds(TenantId tenantId, List<NotificationTargetId> ids);

    List<NotificationTarget> findNotificationTargetsByTenantIdAndUsersFilterType(TenantId tenantId, UsersFilterType filterType);

    PageData<User> findRecipientsForNotificationTarget(TenantId tenantId, CustomerId customerId, NotificationTargetId targetId, PageLink pageLink);

    PageData<User> findRecipientsForNotificationTargetConfig(TenantId tenantId, PlatformUsersNotificationTargetConfig targetConfig, PageLink pageLink);

    PageData<User> findRecipientsForRuleNotificationTargetConfig(TenantId tenantId, PlatformUsersNotificationTargetConfig targetConfig, RuleOriginatedNotificationInfo info, PageLink pageLink);

    void deleteNotificationTargetById(TenantId tenantId, NotificationTargetId id);

    void deleteNotificationTargetsByTenantId(TenantId tenantId);

    long countNotificationTargetsByTenantId(TenantId tenantId);

}
