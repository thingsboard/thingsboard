/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface NotificationTargetService {

    NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget);

    NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id);

    PageData<NotificationTarget> findNotificationTargetsByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink);

    List<UserId> findRecipientsForNotificationTarget(TenantId tenantId, NotificationTargetId notificationTargetId);

}
