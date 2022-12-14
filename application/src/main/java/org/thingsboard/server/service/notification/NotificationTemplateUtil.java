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
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.NotificationText;
import org.thingsboard.server.common.data.notification.template.NotificationTextTemplate;
import org.thingsboard.server.dao.notification.NotificationTemplateService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationTemplateUtil {

    private final NotificationTemplateService templateService;

    public Map<NotificationDeliveryMethod, NotificationTextTemplate> getTemplates(TenantId tenantId, NotificationTemplateId templateId, List<NotificationDeliveryMethod> deliveryMethods) {
        NotificationTemplate notificationTemplate = templateService.findNotificationTemplateById(tenantId, templateId);
        NotificationTemplateConfig config = notificationTemplate.getConfiguration();
        return deliveryMethods.stream()
                .collect(Collectors.toMap(k -> k, deliveryMethod -> {
                    return Optional.ofNullable(config.getTextTemplates())
                            .map(templates -> templates.get(deliveryMethod))
                            .orElse(config.getDefaultTextTemplate());
                }));
    }

    public NotificationText processTemplate(NotificationTextTemplate template, Map<String, String> templateContext) {
        return new NotificationText(TbNodeUtils.processTemplate(template.getBody(), templateContext), template.getSubject());
    }

}
