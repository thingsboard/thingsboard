// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;

public interface NotificationTargetDao extends Dao<NotificationTarget>, TenantEntityDao<NotificationTarget>, ExportableEntityDao<NotificationTargetId, NotificationTarget> {

    PageData<NotificationTarget> findByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink);

    PageData<NotificationTarget> findByTenantIdAndSupportedNotificationTypeAndPageLink(TenantId tenantId, NotificationType notificationType, PageLink pageLink);

    List<NotificationTarget> findByTenantIdAndIds(TenantId tenantId, List<NotificationTargetId> ids);

    List<NotificationTarget> findByTenantIdAndUsersFilterType(TenantId tenantId, UsersFilterType filterType);

    void removeByTenantId(TenantId tenantId);

}
