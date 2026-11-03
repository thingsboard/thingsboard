// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.channels;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel<User, EmailDeliveryMethodNotificationTemplate> {

    private final MailService mailService;

    @Override
    public void sendNotification(User recipient, EmailDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        mailService.send(ctx.getTenantId(), null, TbEmail.builder()
                .to(recipient.getEmail())
                .subject(processedTemplate.getSubject())
                .body(processedTemplate.getBody())
                .html(true)
                .build());
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        if (!mailService.isConfigured(tenantId)) {
            throw new RuntimeException("Mail server is not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.EMAIL;
    }

}
