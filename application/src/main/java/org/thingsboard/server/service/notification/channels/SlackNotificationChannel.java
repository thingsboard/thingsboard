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
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.slack.SlackService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.notification.AlreadySentException;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.template.SlackDeliveryMethodNotificationTemplate;
import org.thingsboard.server.service.executors.ExternalCallExecutorService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

@Component
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel {

    private final SlackService slackService;
    private final ExternalCallExecutorService executor;

    @Override
    public ListenableFuture<Void> sendNotification(User recipient, String text, NotificationProcessingContext ctx) {
        if (ctx.getStats().contains(NotificationDeliveryMethod.SLACK)) {
            return Futures.immediateFailedFuture(new AlreadySentException());
        }

        SlackDeliveryMethodNotificationTemplate template = ctx.getTemplate(NotificationDeliveryMethod.SLACK);
        SlackNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(NotificationDeliveryMethod.SLACK);

        return executor.submit(() -> {
            slackService.sendMessage(ctx.getTenantId(), config.getBotToken(), template.getConversationId(), text);
            return null;
        });
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.SLACK;
    }

}
