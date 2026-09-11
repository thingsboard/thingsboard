// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.NOTIFICATION_TEMPLATE_TABLE_NAME)
public class NotificationTemplateEntity extends BaseSqlEntity<NotificationTemplate> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Column(name = ModelConstants.NAME_PROPERTY, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_TEMPLATE_NOTIFICATION_TYPE_PROPERTY, nullable = false)
    private NotificationType notificationType;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.NOTIFICATION_TEMPLATE_CONFIGURATION_PROPERTY, nullable = false)
    private JsonNode configuration;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public NotificationTemplateEntity() {}

    public NotificationTemplateEntity(NotificationTemplate notificationTemplate) {
        setId(notificationTemplate.getUuidId());
        setCreatedTime(notificationTemplate.getCreatedTime());
        setTenantId(getTenantUuid(notificationTemplate.getTenantId()));
        setName(notificationTemplate.getName());
        setNotificationType(notificationTemplate.getNotificationType());
        setConfiguration(toJson(notificationTemplate.getConfiguration()));
        setExternalId(getUuid(notificationTemplate.getExternalId()));
    }

    @Override
    public NotificationTemplate toData() {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setId(new NotificationTemplateId(id));
        notificationTemplate.setCreatedTime(createdTime);
        notificationTemplate.setTenantId(getTenantId(tenantId));
        notificationTemplate.setName(name);
        notificationTemplate.setNotificationType(notificationType);
        notificationTemplate.setConfiguration(fromJson(configuration, NotificationTemplateConfig.class));
        notificationTemplate.setExternalId(getEntityId(externalId, NotificationTemplateId::new));
        return notificationTemplate;
    }

}
