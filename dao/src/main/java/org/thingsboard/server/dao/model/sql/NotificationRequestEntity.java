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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationSeverity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

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

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_NOTIFICATION_REASON_PROPERTY, nullable = false)
    private String notificationReason;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TEXT_TEMPLATE_PROPERTY, nullable = false)
    private String textTemplate;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_NOTIFICATION_INFO_PROPERTY)
    private JsonNode notificationInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_NOTIFICATION_SEVERITY_PROPERTY)
    private NotificationSeverity notificationSeverity;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    public NotificationRequestEntity() {}

    public NotificationRequestEntity(NotificationRequest notificationRequest) {
        setId(notificationRequest.getUuidId());
        setCreatedTime(notificationRequest.getCreatedTime());
        setTenantId(getUuid(notificationRequest.getTenantId()));
        setTargetId(getUuid(notificationRequest.getTargetId()));
        setNotificationReason(notificationRequest.getNotificationReason());
        setTextTemplate(notificationRequest.getTextTemplate());
        setNotificationInfo(toJson(notificationRequest.getNotificationInfo()));
        setNotificationSeverity(notificationRequest.getNotificationSeverity());
        setAdditionalConfig(toJson(notificationRequest.getAdditionalConfig()));
    }

    @Override
    public NotificationRequest toData() {
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setId(new NotificationRequestId(id));
        notificationRequest.setCreatedTime(createdTime);
        notificationRequest.setTenantId(createId(tenantId, TenantId::new));
        notificationRequest.setTargetId(createId(targetId, NotificationTargetId::new));
        notificationRequest.setNotificationReason(notificationReason);
        notificationRequest.setTextTemplate(textTemplate);
        notificationRequest.setNotificationInfo(fromJson(notificationInfo));
        notificationRequest.setNotificationSeverity(notificationSeverity);
        notificationRequest.setAdditionalConfig(fromJson(additionalConfig));
        return notificationRequest;
    }

}
