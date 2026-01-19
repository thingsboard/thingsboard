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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationRuleInfoEntity extends NotificationRuleEntity {

    private String templateName;
    private JsonNode templateConfig;

    public NotificationRuleInfoEntity(NotificationRuleEntity ruleEntity, String templateName, Object templateConfig) {
        super(ruleEntity);
        this.templateName = templateName;
        this.templateConfig = (JsonNode) templateConfig;
    }

    @Override
    public NotificationRuleInfo toData() {
        NotificationRule rule = super.toData();
        List<NotificationDeliveryMethod> deliveryMethods = fromJson(templateConfig, NotificationTemplateConfig.class)
                .getDeliveryMethodsTemplates().entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .map(Map.Entry::getKey).collect(Collectors.toList());
        return new NotificationRuleInfo(rule, templateName, deliveryMethods);
    }

}
