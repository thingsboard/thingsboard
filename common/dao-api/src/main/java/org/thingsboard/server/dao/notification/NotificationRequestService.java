// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface NotificationRequestService {

    NotificationRequest saveNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest);

    NotificationRequest findNotificationRequestById(TenantId tenantId, NotificationRequestId id);

    NotificationRequestInfo findNotificationRequestInfoById(TenantId tenantId, NotificationRequestId id);

    PageData<NotificationRequest> findNotificationRequestsByTenantIdAndOriginatorType(TenantId tenantId, EntityType originatorType, PageLink pageLink);

    PageData<NotificationRequestInfo> findNotificationRequestsInfosByTenantIdAndOriginatorType(TenantId tenantId, EntityType originatorType, PageLink pageLink);

    List<NotificationRequestId> findNotificationRequestsIdsByStatusAndRuleId(TenantId tenantId, NotificationRequestStatus requestStatus, NotificationRuleId ruleId);

    List<NotificationRequest> findNotificationRequestsByRuleIdAndOriginatorEntityIdAndStatus(TenantId tenantId, NotificationRuleId ruleId, EntityId originatorEntityId, NotificationRequestStatus status);

    void deleteNotificationRequest(TenantId tenantId, NotificationRequest request);

    PageData<NotificationRequest> findScheduledNotificationRequests(PageLink pageLink);

    void updateNotificationRequest(TenantId tenantId, NotificationRequestId requestId, NotificationRequestStatus requestStatus, NotificationRequestStats stats);

    void deleteNotificationRequestsByTenantId(TenantId tenantId);

}
