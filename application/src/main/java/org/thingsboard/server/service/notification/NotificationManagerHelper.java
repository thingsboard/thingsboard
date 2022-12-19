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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationManagerHelper {

    public static final String SETTINGS_KEY = "notifications";
    private final NotificationTemplateService templateService;
    private final AdminSettingsService adminSettingsService;

    public Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> getTemplates(TenantId tenantId, NotificationTemplateId templateId, List<NotificationDeliveryMethod> deliveryMethods) {
        NotificationTemplate notificationTemplate = templateService.findNotificationTemplateById(tenantId, templateId);
        NotificationTemplateConfig config = notificationTemplate.getConfiguration();
        return deliveryMethods.stream()
                .collect(Collectors.toMap(k -> k, deliveryMethod -> {
                    return Optional.ofNullable(config.getTemplates())
                            .map(templates -> templates.get(deliveryMethod))
                            .orElse(config.getDefaultTemplate());
                }));
    }

    public String processTemplate(String template, Map<String, String> templateContext) {
        return TbNodeUtils.processTemplate(template, templateContext);
    }

    public NotificationSettings getNotificationSettings(TenantId tenantId) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), NotificationSettings.class))
                .orElseGet(() -> {
                    NotificationSettings settings = new NotificationSettings();
                    NotificationDeliveryMethodConfig defaultConfig = new NotificationDeliveryMethodConfig();
                    defaultConfig.setEnabled(true);
                    settings.setDeliveryMethodsConfigs(Map.of(
                            NotificationDeliveryMethod.WEBSOCKET, defaultConfig,
                            NotificationDeliveryMethod.EMAIL, defaultConfig,
                            NotificationDeliveryMethod.SMS, defaultConfig
                    ));
                    return settings;
                });
    }

    public void saveNotificationSettings(TenantId tenantId, NotificationSettings notificationSettings) {
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setKey(SETTINGS_KEY);
                    newAdminSettings.setTenantId(tenantId);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(notificationSettings));
        adminSettingsService.saveAdminSettings(tenantId, adminSettings);
    }

}
