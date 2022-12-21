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
package org.thingsboard.server.service.notification;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.dao.notification.NotificationTemplateService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class NotificationProcessingContext {

    @Getter
    private final TenantId tenantId;
    private final NotificationSettings settings;
    @Getter
    private final NotificationRequest request;
    private final Map<String, String> additionalTemplateContext;

    @Getter
    private NotificationTemplate notificationTemplate;
    private Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates;
    @Getter
    private NotificationRequestStats stats;

    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationSettings settings, NotificationRequest request, Map<String, String> additionalTemplateContext) {
        this.tenantId = tenantId;
        this.settings = settings;
        this.request = request;
        this.additionalTemplateContext = additionalTemplateContext;
    }

    public void init(NotificationTemplateService templateService) {
        notificationTemplate = templateService.findNotificationTemplateById(tenantId, request.getTemplateId());
        NotificationTemplateConfig config = notificationTemplate.getConfiguration();
        templates = request.getDeliveryMethods().stream()
                .collect(Collectors.toMap(k -> k, deliveryMethod -> {
                    return Optional.ofNullable(config.getTemplates())
                            .map(templates -> templates.get(deliveryMethod))
                            .orElse(config.getDefaultTemplate());
                }));
        stats = new NotificationRequestStats();
    }

    public <T extends DeliveryMethodNotificationTemplate> T getTemplate(NotificationDeliveryMethod deliveryMethod) {
        return (T) templates.get(deliveryMethod);
    }

    public <C extends NotificationDeliveryMethodConfig> C getDeliveryMethodConfig(NotificationDeliveryMethod deliveryMethod) {
        return (C) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
    }

    public Map<String, String> createTemplateContext(User recipient) {
        Map<String, String> templateContext = new HashMap<>();
        templateContext.put("email", recipient.getEmail());
        templateContext.put("firstName", Strings.nullToEmpty(recipient.getFirstName()));
        templateContext.put("lastName", Strings.nullToEmpty(recipient.getLastName()));
        if (additionalTemplateContext != null) {
            templateContext.putAll(additionalTemplateContext);
        }
        return templateContext;
    }

}
