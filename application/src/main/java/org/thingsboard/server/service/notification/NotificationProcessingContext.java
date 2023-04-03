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

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@SuppressWarnings("unchecked")
public class NotificationProcessingContext {

    @Getter
    private final TenantId tenantId;
    private final NotificationSettings settings;
    @Getter
    private final NotificationRequest request;

    @Getter
    private final NotificationTemplate notificationTemplate;
    private final Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates;
    @Getter
    private Set<NotificationDeliveryMethod> deliveryMethods;
    @Getter
    private final NotificationRequestStats stats;

    private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z]+)(:[a-zA-Z]+)?}");

    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationRequest request, NotificationTemplate template, NotificationSettings settings) {
        this.tenantId = tenantId;
        this.request = request;
        this.settings = settings;
        this.notificationTemplate = template;
        this.templates = new EnumMap<>(NotificationDeliveryMethod.class);
        this.stats = new NotificationRequestStats();
        init();
    }

    private void init() {
        NotificationTemplateConfig templateConfig = notificationTemplate.getConfiguration();
        templateConfig.getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (template.isEnabled()) {
                template = processTemplate(template, null); // processing template with immutable params
                templates.put(deliveryMethod, template);
            }
        });
        deliveryMethods = templates.keySet();
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
            template = processTemplate(template, additionalTemplateContext);
        }
        return template;
    }

    private <T extends DeliveryMethodNotificationTemplate> T processTemplate(T template, Map<String, String> additionalTemplateContext) {
        Map<String, String> templateContext = new HashMap<>();
        if (request.getInfo() != null) {
            templateContext.putAll(request.getInfo().getTemplateData());
        }
        if (additionalTemplateContext != null) {
            templateContext.putAll(additionalTemplateContext);
        }
        if (templateContext.isEmpty()) return template;

        template = (T) template.copy();
        template.setBody(processTemplate(template.getBody(), templateContext));
        if (template instanceof HasSubject) {
            String subject = ((HasSubject) template).getSubject();
            ((HasSubject) template).setSubject(processTemplate(subject, templateContext));
        }
        if (template instanceof WebDeliveryMethodNotificationTemplate) {
            WebDeliveryMethodNotificationTemplate webNotificationTemplate = (WebDeliveryMethodNotificationTemplate) template;
            String buttonText = webNotificationTemplate.getButtonText();
            if (isNotEmpty(buttonText)) {
                webNotificationTemplate.setButtonText(processTemplate(buttonText, templateContext));
            }
            String buttonLink = webNotificationTemplate.getButtonLink();
            if (isNotEmpty(buttonLink)) {
                webNotificationTemplate.setButtonLink(processTemplate(buttonLink, templateContext));
            }
        }
        return template;
    }

    private static String processTemplate(String template, Map<String, String> context) {
        return TEMPLATE_PARAM_PATTERN.matcher(template).replaceAll(matchResult -> {
            String key = matchResult.group(1);
            if (!context.containsKey(key)) {
                return "\\" + matchResult.group(); // adding escape char due to special meaning of '$' to matcher
            }
            String value = Strings.nullToEmpty(context.get(key));
            String function = matchResult.group(2);
            if (function != null) {
                switch (function) {
                    case ":upperCase":
                        return value.toUpperCase();
                    case ":lowerCase":
                        return value.toLowerCase();
                    case ":capitalize":
                        return StringUtils.capitalize(value.toLowerCase());
                }
            }
            return value;
        });
    }

    private Map<String, String> createTemplateContextForRecipient(NotificationRecipient recipient) {
        return Map.of(
                "recipientEmail", Strings.nullToEmpty(recipient.getEmail()),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
    }

}
