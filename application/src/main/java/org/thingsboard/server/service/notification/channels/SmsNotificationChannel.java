// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.channels;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.template.SmsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

@Component
@RequiredArgsConstructor
public class SmsNotificationChannel implements NotificationChannel<User, SmsDeliveryMethodNotificationTemplate> {

    private final SmsService smsService;

    @Override
    public void sendNotification(User recipient, SmsDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        String phone = recipient.getPhone();
        if (StringUtils.isBlank(phone)) {
            throw new RuntimeException("User does not have phone number");
        }

        smsService.sendSms(ctx.getTenantId(), null, new String[]{phone}, processedTemplate.getBody());
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        if (!smsService.isConfigured(tenantId)) {
            throw new RuntimeException("SMS provider is not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.SMS;
    }

}
