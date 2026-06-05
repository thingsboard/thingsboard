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
package org.thingsboard.monitoring.notification.channels.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.monitoring.data.notification.Notification;
import org.thingsboard.monitoring.notification.channels.NotificationChannel;
import org.thingsboard.monitoring.notification.incident.IncidentManager;

import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "monitoring.notifications.slack.enabled", havingValue = "true")
@Slf4j
public class SlackNotificationChannel implements NotificationChannel {

    @Value("${monitoring.notifications.slack.webhook_url}")
    private String webhookUrl;

    @Value("${monitoring.notifications.slack.bot_token:}")
    private String botToken;

    @Value("${monitoring.notifications.slack.channel_id:}")
    private String channelId;

    @Value("${monitoring.notifications.incident.enabled:}")
    private boolean incidentEnabled;

    @Value("${monitoring.notifications.incident.resolution_timeout_s:}")
    private long resolutionTimeoutSeconds;

    @Value("${monitoring.notifications.incident.tag_channel:}")
    private boolean tagChannel;

    @Value("${monitoring.notifications.message_prefix:}")
    private String messagePrefix;

    private RestTemplate restTemplate;
    private SlackApiClient slackApiClient;
    private IncidentManager incidentManager;

    @PostConstruct
    private void init() {
        boolean hasBotConfig = botToken != null && !botToken.isEmpty() && channelId != null && !channelId.isEmpty();
        if (hasBotConfig) {
            slackApiClient = new SlackApiClient(botToken);
            log.info("Slack API mode enabled (channel: {})", channelId);
            if (incidentEnabled) {
                incidentManager = new IncidentManager(new SlackIncidentTransport(slackApiClient, channelId),
                        resolutionTimeoutSeconds, messagePrefix, tagChannel);
                log.info("Incident grouping enabled via Slack (resolution timeout: {}s)", resolutionTimeoutSeconds);
            }
        } else {
            if (incidentEnabled) {
                log.warn("Incident grouping is enabled but Slack bot_token/channel_id are not set; " +
                        "falling back to plain webhook mode without incident support");
            }
            restTemplate = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(2))
                    .build();
            log.info("Slack webhook mode enabled");
        }
    }

    @Override
    public void sendNotification(String message, Notification notification) {
        if (incidentManager != null && notification.isIncident()) {
            // Pass the raw notification text: IncidentManager already puts the prefix into the
            // incident header, so pre-prefixing the thread reply would double it up.
            incidentManager.sendAlert(notification.getText(), notification.getAffectedServices());
        } else if (slackApiClient != null) {
            slackApiClient.postMessage(channelId, message);
        } else {
            restTemplate.postForObject(webhookUrl, Map.of("text", message), String.class);
        }
    }

    @PreDestroy
    private void destroy() {
        if (incidentManager != null) {
            incidentManager.shutdown();
        }
        if (slackApiClient != null) {
            slackApiClient.close();
        }
    }

}
