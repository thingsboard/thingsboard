// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.channels;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.template.SlackDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

@Component
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel<SlackConversation, SlackDeliveryMethodNotificationTemplate> {

    private final SlackService slackService;
    private final NotificationSettingsService notificationSettingsService;

    @Override
    public void sendNotification(SlackConversation conversation, SlackDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        SlackNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(NotificationDeliveryMethod.SLACK);
        slackService.sendMessage(ctx.getTenantId(), config.getBotToken(), conversation.getId(), processedTemplate.getBody());
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        NotificationSettings notificationSettings = notificationSettingsService.findNotificationSettings(tenantId);
        if (!notificationSettings.getDeliveryMethodsConfigs().containsKey(NotificationDeliveryMethod.SLACK)) {
            throw new RuntimeException("Slack API token is not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.SLACK;
    }

}
