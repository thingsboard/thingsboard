// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.notification.channels;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackFile;
import org.thingsboard.server.common.data.notification.template.SlackDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel<SlackConversation, SlackDeliveryMethodNotificationTemplate> {

    private final SlackService slackService;
    private final NotificationSettingsService notificationSettingsService;
    private final ReportService reportService;

    @Override
    public void sendNotification(SlackConversation conversation, SlackDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        SlackNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(NotificationDeliveryMethod.SLACK);
        List<SlackFile> files = null;
        NotificationRequestConfig requestConfig = ctx.getRequest().getAdditionalConfig();
        if (requestConfig != null && CollectionsUtil.isNotEmpty(requestConfig.getReports())) {
            files = requestConfig.getReports().stream()
                    .map(reportId -> {
                        ReportData reportData = reportService.getReportDataById(ctx.getTenantId(), reportId);
                        return SlackFile.builder()
                                .name(reportData.getName())
                                .data(reportData.getData())
                                .build();
                    })
                    .toList();
        }
        slackService.sendMessage(ctx.getTenantId(), config.getBotToken(), conversation.getId(), processedTemplate.getBody(), files);
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
