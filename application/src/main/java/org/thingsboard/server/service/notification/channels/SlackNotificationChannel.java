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
package org.thingsboard.server.service.notification.channels;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.service.slack.SlackService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.template.SlackDeliveryMethodNotificationTemplate;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel {

    private final SlackService slackService;
    private ExecutorService executor;

    @PostConstruct
    private void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public ListenableFuture<Void> sendNotification(User recipient, String text, NotificationProcessingContext ctx) {
        SlackDeliveryMethodNotificationTemplate template = ctx.getTemplate(NotificationDeliveryMethod.SLACK);
        SlackNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(NotificationDeliveryMethod.SLACK);

        String conversationId = template.getConversationId();
        if (StringUtils.isNotEmpty(conversationId)) {
            if (ctx.getStats().contains(NotificationDeliveryMethod.SLACK)) {
                // FIXME stats.sent will be reported anyway
                return Futures.immediateFuture(null); // if conversationId is set, we only need to send message once
            }
        } else {
            String username = StringUtils.join(new String[]{recipient.getFirstName(), recipient.getLastName()}, ' ');
            if (StringUtils.isNotEmpty(username)) {
                conversationId = username;
            } else {
                return Futures.immediateFailedFuture(new IllegalArgumentException("Couldn't determine Slack username for the user"));
            }
        }
        return send(ctx.getTenantId(), config.getBotToken(), conversationId, text);
    }

    private ListenableFuture<Void> send(TenantId tenantId, String botToken, String conversationId, String text) {
        return Futures.submit(() -> {
            slackService.sendMessage(tenantId, botToken, conversationId, text);
            return null;
        }, executor);
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.SLACK;
    }

}
