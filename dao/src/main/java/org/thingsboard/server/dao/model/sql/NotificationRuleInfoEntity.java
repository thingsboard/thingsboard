// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
