// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.MobileAppNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.util.TemplateUtils;
import org.thingsboard.server.dao.secret.SecretConfigurationService;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.SOFT;

@SuppressWarnings("unchecked")
public class NotificationProcessingContext {

    @Getter
    private final TenantId tenantId;
    private final NotificationSettings settings;
    private final NotificationSettings systemSettings;
    @Getter
    private final NotificationRequest request;
    @Getter
    private final Set<NotificationDeliveryMethod> deliveryMethods;
    @Getter
    private final NotificationTemplate notificationTemplate;
    @Getter
    private final NotificationType notificationType;

    private final Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates;
    @Getter
    private final NotificationRequestStats stats;
    private final SecretConfigurationService secretConfigurationService;

    private final Function<String, JsonNode> translationProvider;
    private final Map<String, JsonNode> fullTranslations = new ConcurrentReferenceHashMap<>(4, SOFT);
    private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationRequest request, Set<NotificationDeliveryMethod> deliveryMethods,
                                         NotificationTemplate template, NotificationSettings settings, NotificationSettings systemSettings,
                                         Function<String, JsonNode> translationProvider, SecretConfigurationService secretConfigurationService) {
        this.tenantId = tenantId;
        this.request = request;
        this.deliveryMethods = deliveryMethods;
        this.settings = settings;
        this.systemSettings = systemSettings;
        this.translationProvider = translationProvider;
        this.notificationTemplate = template;
        this.notificationType = template.getNotificationType();
        this.templates = new EnumMap<>(NotificationDeliveryMethod.class);
        this.stats = request.getStats() != null ? request.getStats() : new NotificationRequestStats();
        this.secretConfigurationService = secretConfigurationService;
        init();
    }

    private void init() {
        NotificationTemplateConfig templateConfig = notificationTemplate.getConfiguration();
        templateConfig.getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (template.isEnabled()) {
                template = processTemplate(template, null, null); // processing template with immutable params
                templates.put(deliveryMethod, template);
            }
        });
    }

    public <C extends NotificationDeliveryMethodConfig> C getDeliveryMethodConfig(NotificationDeliveryMethod deliveryMethod) {
        NotificationSettings settings = this.settings;
        boolean isSystem = false;
        if (deliveryMethod == NotificationDeliveryMethod.MOBILE_APP && !tenantId.isSysTenantId()) {
            var config = (MobileAppNotificationDeliveryMethodConfig) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
            if (config == null || config.isUseSystemSettings()) {
                settings = this.systemSettings;
                isSystem = true;
            }
        }
        var config = (C) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
        return secretConfigurationService.replaceSecretUsages(isSystem ? TenantId.SYS_TENANT_ID : tenantId, config, (Class<C>) config.getClass());
    }

    public <T extends DeliveryMethodNotificationTemplate> T getProcessedTemplate(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        T template = (T) templates.get(deliveryMethod);
        if (recipient != null) {
            String locale = recipient instanceof User user ? user.getLocale() : Locale.US.toString();
            Map<String, String> additionalTemplateContext = createTemplateContextForRecipient(recipient);
            List<String> params = new ArrayList<>(additionalTemplateContext.keySet());
            params.add("translate"); // checking if template value contains any recipient-related variables or translated keys
            if (template.getTemplatableValues().stream().anyMatch(value -> value.contains(params))) {
                template = processTemplate(template, locale, additionalTemplateContext);
            }
        }
        return template;
    }

    private <T extends DeliveryMethodNotificationTemplate> T processTemplate(T template, String locale, Map<String, String> additionalTemplateContext) {
        Map<String, String> templateContext = new HashMap<>();
        if (request.getInfo() != null) {
            templateContext.putAll(request.getInfo().getTemplateData());
        }
        if (additionalTemplateContext != null) {
            templateContext.putAll(additionalTemplateContext);
        }
        if (templateContext.isEmpty()) return template;

        template = (T) template.copy();
        template.getTemplatableValues().forEach(templatableValue -> {
            String value = templatableValue.get();
            if (StringUtils.isNotEmpty(value)) {
                Map<String, UnaryOperator<String>> functions = null;
                if (locale != null && translationProvider != null) {
                    functions = Map.of("translate", key -> translate(key, locale));
                }
                value = TemplateUtils.processTemplate(value, templateContext, functions);
                templatableValue.set(value);
            }
        });
        return template;
    }

    private String translate(String key, String locale) {
        return translations.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(locale, k -> {
                    JsonNode fullTranslation = fullTranslations.computeIfAbsent(locale, translationProvider);
                    return Optional.ofNullable(JacksonUtil.getByKeyPath(fullTranslation, key))
                            .map(JsonNode::asText).orElse("");
                });
    }

    private Map<String, String> createTemplateContextForRecipient(NotificationRecipient recipient) {
        return Map.of(
                "recipientTitle", recipient.getTitle(),
                "recipientEmail", Strings.nullToEmpty(recipient.getEmail()),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
    }

}
