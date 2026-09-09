// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.channels;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

public interface NotificationChannel<R extends NotificationRecipient, T extends DeliveryMethodNotificationTemplate> {

    void sendNotification(R recipient, T processedTemplate, NotificationProcessingContext ctx) throws Exception;

    void check(TenantId tenantId) throws Exception;

    NotificationDeliveryMethod getDeliveryMethod();

}
