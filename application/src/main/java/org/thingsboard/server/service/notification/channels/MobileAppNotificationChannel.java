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

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserMobileInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.MobileAppNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MobileAppNotificationChannel implements NotificationChannel<User, MobileAppDeliveryMethodNotificationTemplate> {

    private final FirebaseService firebaseService;
    private final UserService userService;
    private final NotificationSettingsService notificationSettingsService;

    @Override
    public void sendNotification(User recipient, MobileAppDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        UserMobileInfo mobileInfo = userService.findMobileInfo(recipient.getTenantId(), recipient.getId());
        String fcmToken = Optional.ofNullable(mobileInfo)
                .map(UserMobileInfo::getFcmToken)
                .orElseThrow(() -> new IllegalArgumentException("User doesn't use the mobile app"));

        MobileAppNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(NotificationDeliveryMethod.MOBILE_APP);
        try {
            firebaseService.sendMessage(ctx.getTenantId(), config.getFirebaseServiceAccountCredentials(),
                    fcmToken, processedTemplate.getSubject(), processedTemplate.getBody());
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                // the token is no longer valid
                mobileInfo.setFcmToken(null);
                userService.saveMobileInfo(recipient.getTenantId(), recipient.getId(), mobileInfo);
                throw new IllegalArgumentException("User doesn't use the mobile app");
            }
            throw new RuntimeException("Failed to send message via FCM: " + e.getMessage(), e);
        }
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);
        if (!settings.getDeliveryMethodsConfigs().containsKey(NotificationDeliveryMethod.MOBILE_APP)) {
            throw new RuntimeException("Push-notifications to mobile are not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.MOBILE_APP;
    }

}
