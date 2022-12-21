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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationOriginatorType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.NOTIFICATION_REQUEST_TABLE_NAME)
public class NotificationRequestEntity extends BaseSqlEntity<NotificationRequest> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TARGET_ID_PROPERTY, nullable = false)
    private UUID targetId;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TEMPLATE_ID_PROPERTY, nullable = false)
    private UUID templateId;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_INFO_PROPERTY)
    private JsonNode info;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_DELIVERY_METHODS_PROPERTY, nullable = false)
    private String deliveryMethods;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ORIGINATOR_TYPE_PROPERTY, nullable = false)
    private NotificationOriginatorType originatorType;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_ID_PROPERTY)
    private UUID originatorEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_TYPE_PROPERTY)
    private EntityType originatorEntityType;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_RULE_ID_PROPERTY)
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_STATUS_PROPERTY)
    private NotificationRequestStatus status;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_STATS_PROPERTY)
    private JsonNode stats;

    public NotificationRequestEntity() {}

    public NotificationRequestEntity(NotificationRequest notificationRequest) {
        setId(notificationRequest.getUuidId());
        setCreatedTime(notificationRequest.getCreatedTime());
        setTenantId(getUuid(notificationRequest.getTenantId()));
        setTargetId(getUuid(notificationRequest.getTargetId()));
        setTemplateId(getUuid(notificationRequest.getTemplateId()));
        setInfo(toJson(notificationRequest.getInfo()));
        setDeliveryMethods(StringUtils.join(notificationRequest.getDeliveryMethods(), ','));
        setAdditionalConfig(toJson(notificationRequest.getAdditionalConfig()));
        setOriginatorType(notificationRequest.getOriginatorType());
        if (notificationRequest.getOriginatorEntityId() != null) {
            setOriginatorEntityId(notificationRequest.getOriginatorEntityId().getId());
            setOriginatorEntityType(notificationRequest.getOriginatorEntityId().getEntityType());
        }
        setRuleId(getUuid(notificationRequest.getRuleId()));
        setStatus(notificationRequest.getStatus());
        setStats(toJson(notificationRequest.getStats()));
    }

    @Override
    public NotificationRequest toData() {
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setId(new NotificationRequestId(id));
        notificationRequest.setCreatedTime(createdTime);
        notificationRequest.setTenantId(createId(tenantId, TenantId::new));
        notificationRequest.setTargetId(createId(targetId, NotificationTargetId::new));
        notificationRequest.setTemplateId(createId(templateId, NotificationTemplateId::new));
        notificationRequest.setInfo(fromJson(info, NotificationInfo.class));
        if (deliveryMethods != null) {
            notificationRequest.setDeliveryMethods(Arrays.stream(StringUtils.split(deliveryMethods, ','))
                    .filter(StringUtils::isNotBlank).map(NotificationDeliveryMethod::valueOf).collect(Collectors.toList()));
        }
        notificationRequest.setAdditionalConfig(fromJson(additionalConfig, NotificationRequestConfig.class));
        notificationRequest.setOriginatorType(originatorType);
        if (originatorEntityId != null) {
            notificationRequest.setOriginatorEntityId(EntityIdFactory.getByTypeAndUuid(originatorEntityType, originatorEntityId));
        }
        notificationRequest.setRuleId(createId(ruleId, NotificationRuleId::new));
        notificationRequest.setStatus(status);
        notificationRequest.setStats(fromJson(stats, NotificationRequestStats.class));
        return notificationRequest;
    }

}
