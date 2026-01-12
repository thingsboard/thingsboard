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
package org.thingsboard.server.service.notification.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.settings.MobileAppNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.MOBILE_APP;

@Component
@RequiredArgsConstructor
@Slf4j
public class MobileAppNotificationChannel implements NotificationChannel<User, MobileAppDeliveryMethodNotificationTemplate> {

    private final FirebaseService firebaseService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final NotificationSettingsService notificationSettingsService;

    @Override
    public void sendNotification(User recipient, MobileAppDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        NotificationRequest request = ctx.getRequest();
        NotificationInfo info = request.getInfo();
        if (info != null && info.getDashboardId() != null) {
            ObjectNode additionalConfig = JacksonUtil.asObject(processedTemplate.getAdditionalConfig());
            ObjectNode onClick = JacksonUtil.asObject(additionalConfig.get("onClick"));
            if (onClick.get("enabled") == null || !Boolean.parseBoolean(onClick.get("enabled").asText())) {
                onClick.put("enabled", true);
                onClick.put("linkType", "DASHBOARD");
                onClick.put("setEntityIdInState", true);
                onClick.put("dashboardId", info.getDashboardId().toString());
                additionalConfig.set("onClick", onClick);
            }
            processedTemplate.setAdditionalConfig(additionalConfig);
        }
        Notification notification = Notification.builder()
                .requestId(request.getId())
                .recipientId(recipient.getId())
                .type(ctx.getNotificationType())
                .deliveryMethod(MOBILE_APP)
                .subject(processedTemplate.getSubject())
                .text(processedTemplate.getBody())
                .additionalConfig(processedTemplate.getAdditionalConfig())
                .info(info)
                .status(NotificationStatus.SENT)
                .build();
        notificationService.saveNotification(recipient.getTenantId(), notification);

        var mobileSessions = userService.findMobileSessions(recipient.getTenantId(), recipient.getId());
        if (mobileSessions.isEmpty()) {
            throw new IllegalArgumentException("User doesn't use the mobile app");
        }

        MobileAppNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(MOBILE_APP);
        String credentials = config.getFirebaseServiceAccountCredentials();
        Set<String> validTokens = new HashSet<>(mobileSessions.keySet());

        String subject = processedTemplate.getSubject();
        String body = processedTemplate.getBody();
        Map<String, String> data = getNotificationData(processedTemplate, ctx);
        int unreadCount = notificationService.countUnreadNotificationsByRecipientId(ctx.getTenantId(), MOBILE_APP, recipient.getId());
        for (String token : mobileSessions.keySet()) {
            try {
                firebaseService.sendMessage(ctx.getTenantId(), credentials, token, subject, body, data, unreadCount);
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode errorCode = e.getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
                    validTokens.remove(token);
                    userService.removeMobileSession(recipient.getTenantId(), token);
                    log.debug("[{}][{}] Removed invalid FCM token due to {} {} ({})", recipient.getTenantId(), recipient.getId(), errorCode, e.getMessage(), token);
                    continue;
                }
                throw new RuntimeException("Failed to send message via FCM: " + e.getMessage(), e);
            }
        }
        if (validTokens.isEmpty()) {
            throw new IllegalArgumentException("User doesn't use the mobile app");
        }
    }

    private Map<String, String> getNotificationData(MobileAppDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) {
        Map<String, String> data = Optional.ofNullable(processedTemplate.getAdditionalConfig())
                .filter(JsonNode::isObject).map(JacksonUtil::toFlatMap).orElseGet(HashMap::new);
        NotificationInfo info = ctx.getRequest().getInfo();
        if (info == null) {
            return data;
        }
        Optional.ofNullable(info.getStateEntityId()).ifPresent(stateEntityId -> {
            data.put("stateEntityId", stateEntityId.getId().toString());
            data.put("stateEntityType", stateEntityId.getEntityType().name());
        });
        data.put("notificationType", ctx.getNotificationType().name());
        switch (ctx.getNotificationType()) {
            case ALARM:
            case ALARM_ASSIGNMENT:
            case ALARM_COMMENT:
                info.getTemplateData().forEach((key, value) -> {
                    data.put("info." + key, value);
                });
                break;
        }
        data.replaceAll((key, value) -> Strings.nullToEmpty(value));
        return data;
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        NotificationSettings systemSettings = notificationSettingsService.findNotificationSettings(TenantId.SYS_TENANT_ID);
        if (!systemSettings.getDeliveryMethodsConfigs().containsKey(MOBILE_APP)) {
            throw new RuntimeException("Push-notifications to mobile are not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return MOBILE_APP;
    }

}
