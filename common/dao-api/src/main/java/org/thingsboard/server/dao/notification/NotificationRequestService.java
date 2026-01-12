/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
