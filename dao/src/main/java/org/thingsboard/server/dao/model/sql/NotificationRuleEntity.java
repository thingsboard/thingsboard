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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.rule.NotificationEscalationConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Data @EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.NOTIFICATION_RULE_TABLE_NAME)
public class NotificationRuleEntity extends BaseSqlEntity<NotificationRule> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Column(name = ModelConstants.NAME_PROPERTY, nullable = false)
    private String name;

    @Column(name = ModelConstants.NOTIFICATION_RULE_TEMPLATE_ID_PROPERTY, nullable = false)
    private UUID templateId;

    @Column(name = ModelConstants.NOTIFICATION_RULE_DELIVERY_METHODS_PROPERTY, nullable = false)
    private String deliveryMethods;

    @Column(name = ModelConstants.NOTIFICATION_RULE_INITIAL_NOTIFICATION_TARGET_ID_PROPERTY)
    private UUID initialNotificationTargetId;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_RULE_ESCALATION_CONFIG_PROPERTY)
    private JsonNode escalationConfig;

    public NotificationRuleEntity() {}

    public NotificationRuleEntity(NotificationRule notificationRule) {
        setId(notificationRule.getUuidId());
        setCreatedTime(notificationRule.getCreatedTime());
        setTenantId(getUuid(notificationRule.getTenantId()));
        setName(notificationRule.getName());
        setTemplateId(getUuid(notificationRule.getTemplateId()));
        setDeliveryMethods(StringUtils.join(notificationRule.getDeliveryMethods(), ','));
        setInitialNotificationTargetId(getUuid(notificationRule.getInitialNotificationTargetId()));
        setEscalationConfig(toJson(notificationRule.getEscalationConfig()));
    }

    @Override
    public NotificationRule toData() {
        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setId(new NotificationRuleId(id));
        notificationRule.setCreatedTime(createdTime);
        notificationRule.setTenantId(createId(tenantId, TenantId::fromUUID));
        notificationRule.setName(name);
        notificationRule.setTemplateId(createId(templateId, NotificationTemplateId::new));
        if (deliveryMethods != null) {
            notificationRule.setDeliveryMethods(Arrays.stream(StringUtils.split(deliveryMethods, ','))
                    .filter(StringUtils::isNotBlank).map(NotificationDeliveryMethod::valueOf).collect(Collectors.toList()));
        }
        notificationRule.setInitialNotificationTargetId(createId(initialNotificationTargetId, NotificationTargetId::new));
        notificationRule.setEscalationConfig(fromJson(escalationConfig, NotificationEscalationConfig.class));
        return notificationRule;
    }

}
