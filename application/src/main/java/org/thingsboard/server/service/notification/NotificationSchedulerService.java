// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification;

import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;

public interface NotificationSchedulerService {

    void scheduleNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId, long requestTs);

}
