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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.NOTIFICATION_RULE_TABLE_NAME)
public class NotificationRuleEntity extends BaseSqlEntity<NotificationRule> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Column(name = ModelConstants.NAME_PROPERTY, nullable = false)
    private String name;

    @Column(name = ModelConstants.NOTIFICATION_RULE_ENABLED_PROPERTY, nullable = false)
    private boolean enabled;

    @Column(name = ModelConstants.NOTIFICATION_RULE_TEMPLATE_ID_PROPERTY, nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_RULE_TRIGGER_TYPE_PROPERTY, nullable = false)
    private NotificationRuleTriggerType triggerType;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.NOTIFICATION_RULE_TRIGGER_CONFIG_PROPERTY, nullable = false)
    private JsonNode triggerConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.NOTIFICATION_RULE_RECIPIENTS_CONFIG_PROPERTY, nullable = false)
    private JsonNode recipientsConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.NOTIFICATION_RULE_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public NotificationRuleEntity() {}

    public NotificationRuleEntity(NotificationRule notificationRule) {
        setId(notificationRule.getUuidId());
        setCreatedTime(notificationRule.getCreatedTime());
        setTenantId(getTenantUuid(notificationRule.getTenantId()));
        setName(notificationRule.getName());
        setEnabled(notificationRule.isEnabled());
        setTemplateId(getUuid(notificationRule.getTemplateId()));
        setTriggerType(notificationRule.getTriggerType());
        setTriggerConfig(toJson(notificationRule.getTriggerConfig()));
        setRecipientsConfig(toJson(notificationRule.getRecipientsConfig()));
        setAdditionalConfig(toJson(notificationRule.getAdditionalConfig()));
        setExternalId(getUuid(notificationRule.getExternalId()));
    }

    public NotificationRuleEntity(NotificationRuleEntity other) {
        this.id = other.id;
        this.createdTime = other.createdTime;
        this.tenantId = other.tenantId;
        this.name = other.name;
        this.enabled = other.enabled;
        this.templateId = other.templateId;
        this.triggerType = other.triggerType;
        this.triggerConfig = other.triggerConfig;
        this.recipientsConfig = other.recipientsConfig;
        this.additionalConfig = other.additionalConfig;
        this.externalId = other.externalId;
    }

    @Override
    public NotificationRule toData() {
        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setId(new NotificationRuleId(id));
        notificationRule.setCreatedTime(createdTime);
        notificationRule.setTenantId(getTenantId(tenantId));
        notificationRule.setName(name);
        notificationRule.setEnabled(enabled);
        notificationRule.setTemplateId(getEntityId(templateId, NotificationTemplateId::new));
        notificationRule.setTriggerType(triggerType);
        notificationRule.setTriggerConfig(fromJson(triggerConfig, NotificationRuleTriggerConfig.class));
        notificationRule.setRecipientsConfig(fromJson(recipientsConfig, NotificationRuleRecipientsConfig.class));
        notificationRule.setAdditionalConfig(fromJson(additionalConfig, NotificationRuleConfig.class));
        notificationRule.setExternalId(getEntityId(externalId, NotificationRuleId::new));
        return notificationRule;
    }

}
