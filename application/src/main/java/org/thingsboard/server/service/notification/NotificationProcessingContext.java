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
package org.thingsboard.server.service.notification;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@SuppressWarnings("unchecked")
public class NotificationProcessingContext {

    @Getter
    private final TenantId tenantId;
    private final NotificationSettings settings;
    @Getter
    private final NotificationRequest request;
    @Getter
    private final Set<NotificationDeliveryMethod> deliveryMethods;
    @Getter
    private final NotificationTemplate notificationTemplate;

    private final Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates;
    @Getter
    private final NotificationRequestStats stats;

    private final TemplateService templateService;

    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationRequest request, Set<NotificationDeliveryMethod> deliveryMethods,
                                         NotificationTemplate template, NotificationSettings settings, TemplateService templateService) {
        this.tenantId = tenantId;
        this.request = request;
        this.deliveryMethods = deliveryMethods;
        this.settings = settings;
        this.notificationTemplate = template;
        this.templates = new EnumMap<>(NotificationDeliveryMethod.class);
        this.stats = new NotificationRequestStats();
        this.templateService = templateService;
        init();
    }

    private final Map<String, Function<NotificationRecipient, String>> recipientTemplateParams = Map.of(
            "recipientTitle", NotificationRecipient::getTitle,
            "recipientEmail", recipient -> Strings.nullToEmpty(recipient.getEmail()),
            "recipientFirstName", recipient -> Strings.nullToEmpty(recipient.getFirstName()),
            "recipientLastName", recipient -> Strings.nullToEmpty(recipient.getLastName())
    );

    private void init() {
        NotificationTemplateConfig templateConfig = notificationTemplate.getConfiguration();
        templateConfig.getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (template.isEnabled()) {
                template = processTemplate(template, null, recipientTemplateParams.keySet());
                templates.put(deliveryMethod, template);
            }
        });
    }

    public <C extends NotificationDeliveryMethodConfig> C getDeliveryMethodConfig(NotificationDeliveryMethod deliveryMethod) {
        return (C) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
    }

    public <T extends DeliveryMethodNotificationTemplate> T getProcessedTemplate(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        T template = (T) templates.get(deliveryMethod);
        Map<String, String> additionalTemplateContext = null;
        if (recipient != null) {
            additionalTemplateContext = createTemplateContextForRecipient(recipient);
        }
        if (MapUtils.isNotEmpty(additionalTemplateContext) && template.containsAny(additionalTemplateContext.keySet().toArray(String[]::new))) {
            template = processTemplate(template, additionalTemplateContext, Collections.emptySet());
        }
        return template;
    }

    private <T extends DeliveryMethodNotificationTemplate> T processTemplate(T template, Map<String, String> additionalTemplateContext, Set<String> ignoredParams) {
        Map<String, String> templateContext = new HashMap<>();
        if (request.getInfo() != null) {
            templateContext.putAll(request.getInfo().getTemplateData());
        }
        if (additionalTemplateContext != null) {
            templateContext.putAll(additionalTemplateContext);
        }
        if (templateContext.isEmpty() && ignoredParams.isEmpty()) return template;

        template = (T) template.copy();
        template.setBody(templateService.processTemplate(tenantId, template.getBody(), templateContext, ignoredParams));
        if (template instanceof HasSubject) {
            String subject = ((HasSubject) template).getSubject();
            ((HasSubject) template).setSubject(templateService.processTemplate(tenantId, subject, templateContext, ignoredParams));
        }
        if (template instanceof WebDeliveryMethodNotificationTemplate) {
            WebDeliveryMethodNotificationTemplate webNotificationTemplate = (WebDeliveryMethodNotificationTemplate) template;
            String buttonText = webNotificationTemplate.getButtonText();
            if (isNotEmpty(buttonText)) {
                webNotificationTemplate.setButtonText(templateService.processTemplate(tenantId, buttonText, templateContext, ignoredParams));
            }
            String buttonLink = webNotificationTemplate.getButtonLink();
            if (isNotEmpty(buttonLink)) {
                webNotificationTemplate.setButtonLink(templateService.processTemplate(tenantId, buttonLink, templateContext, ignoredParams));
            }
        }
        return template;
    }

    private Map<String, String> createTemplateContextForRecipient(NotificationRecipient recipient) {
        return recipientTemplateParams.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().apply(recipient)));
    }

}
