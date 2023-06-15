/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.MobileNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.MobileDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MobileNotificationChannel implements NotificationChannel<User, MobileDeliveryMethodNotificationTemplate> {

    private final FirebaseService firebaseService;
    private final NotificationSettingsService notificationSettingsService;

    @Override
    public void sendNotification(User recipient, MobileDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        String fcmToken = Optional.ofNullable(recipient.getAdditionalInfo())
                .map(info -> info.get("fcmToken")).filter(JsonNode::isTextual).map(JsonNode::asText)
                .orElse(null);
        if (StringUtils.isEmpty(fcmToken)) {
            throw new RuntimeException("User doesn't have the mobile app installed");
        }

        MobileNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(NotificationDeliveryMethod.MOBILE);
        firebaseService.sendMessage(ctx.getTenantId(), config.getFirebaseServiceAccountCredentials(),
                fcmToken, processedTemplate.getSubject(), processedTemplate.getBody());
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);
        if (!settings.getDeliveryMethodsConfigs().containsKey(NotificationDeliveryMethod.MOBILE)) {
            throw new RuntimeException("Push-notifications to mobile are not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.MOBILE;
    }

}
