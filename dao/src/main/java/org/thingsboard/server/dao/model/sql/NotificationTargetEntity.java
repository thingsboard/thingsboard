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
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.NOTIFICATION_TARGET_TABLE_NAME)
public class NotificationTargetEntity extends BaseSqlEntity<NotificationTarget> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Column(name = ModelConstants.NAME_PROPERTY, nullable = false)
    private String name;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_TARGET_CONFIGURATION_PROPERTY, nullable = false)
    private JsonNode configuration;

    public NotificationTargetEntity() {}

    public NotificationTargetEntity(NotificationTarget notificationTarget) {
        setId(notificationTarget.getUuidId());
        setCreatedTime(notificationTarget.getCreatedTime());
        setTenantId(getUuid(notificationTarget.getTenantId()));
        setName(notificationTarget.getName());
        setConfiguration(JacksonUtil.valueToTree(notificationTarget.getConfiguration()));
    }

    @Override
    public NotificationTarget toData() {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setId(new NotificationTargetId(id));
        notificationTarget.setCreatedTime(createdTime);
        notificationTarget.setTenantId(createId(tenantId, TenantId::fromUUID));
        notificationTarget.setName(name);
        if (configuration != null) {
            notificationTarget.setConfiguration(JacksonUtil.treeToValue(configuration, NotificationTargetConfig.class));
        }
        return notificationTarget;
    }

}
