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
package org.thingsboard.server.common.data.notification;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationRequest request, NotificationSettings settings,
                                         NotificationTemplate template) {
        this.tenantId = tenantId;
        this.request = request;
        this.settings = settings;
        this.notificationTemplate = template;
        this.templates = new EnumMap<>(NotificationDeliveryMethod.class);
        this.stats = new NotificationRequestStats();
    }

    public void init() {
        NotificationTemplateConfig templateConfig = notificationTemplate.getConfiguration();
        templateConfig.getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (!template.isEnabled()) return;

            template = template.copy();
            if (StringUtils.isEmpty(template.getBody())) {
                template.setBody(templateConfig.getDefaultTextTemplate());
            }
            if (template instanceof HasSubject) {
                if (StringUtils.isEmpty(((HasSubject) template).getSubject())) {
                    ((HasSubject) template).setSubject(templateConfig.getNotificationSubject());
                }
            }
            templates.put(deliveryMethod, template);
        });
        deliveryMethods = templates.keySet();
    }

    public <C extends NotificationDeliveryMethodConfig> C getDeliveryMethodConfig(NotificationDeliveryMethod deliveryMethod) {
        return (C) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
    }

    public  <T extends DeliveryMethodNotificationTemplate> T getProcessedTemplate(NotificationDeliveryMethod deliveryMethod, Map<String, String> templateContext) {
        if (request.getInfo() != null && deliveryMethod != NotificationDeliveryMethod.PUSH) { // for push notifications we are processing template from info on each serialization
            templateContext = new HashMap<>(templateContext);
            templateContext.putAll(request.getInfo().getTemplateData());
        }

        T template = (T) templates.get(deliveryMethod).copy();
        template.setBody(processTemplate(template.getBody(), templateContext));
        if (template instanceof HasSubject) {
            String subject = ((HasSubject) template).getSubject();
            ((HasSubject) template).setSubject(processTemplate(subject, templateContext));
        }
        return template;
    }

    private static String processTemplate(String template, Map<String, String> context) {
        if (template == null || context.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, String> kv : context.entrySet()) {
            result = result.replace("${" + kv.getKey() + '}', kv.getValue());
        }
        return result;
    }

    public static String processTemplate(String template, NotificationInfo notificationInfo) {
        if (notificationInfo == null) return template;
        Map<String, String> templateContext = notificationInfo.getTemplateData();
        return processTemplate(template, templateContext);
    }

    public Map<String, String> createTemplateContext(User recipient) {
        Map<String, String> templateContext = new HashMap<>();
        templateContext.put("email", recipient.getEmail());
        templateContext.put("firstName", Strings.nullToEmpty(recipient.getFirstName()));
        templateContext.put("lastName", Strings.nullToEmpty(recipient.getLastName()));
        return templateContext;
    }

    public CustomerId getCustomerId() {
        CustomerId customerId;
        switch (request.getOriginatorEntityId().getEntityType()) {
            case ALARM:
                customerId = ((AlarmNotificationInfo) request.getInfo()).getCustomerId();
                break;
            default:
                customerId = null;
        }
        return customerId;
    }

}
