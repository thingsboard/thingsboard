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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CREATED_TIME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ALARM_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@Entity
@Table(name = ENTITY_ALARM_TABLE_NAME)
@IdClass(EntityAlarmCompositeKey.class)
public final class EntityAlarmEntity implements ToData<EntityAlarm> {

    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = ENTITY_TYPE_COLUMN)
    private String entityType;

    @Id
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    private UUID entityId;

    @Id
    @Column(name = "alarm_id", columnDefinition = "uuid")
    private UUID alarmId;

    @Column(name = CREATED_TIME_PROPERTY)
    private long createdTime;

    @Column(name = "alarm_type")
    private String alarmType;

    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    public EntityAlarmEntity() {
        super();
    }

    public EntityAlarmEntity(EntityAlarm entityAlarm) {
        tenantId = entityAlarm.getTenantId().getId();
        entityId = entityAlarm.getEntityId().getId();
        entityType = entityAlarm.getEntityId().getEntityType().name();
        alarmId = entityAlarm.getAlarmId().getId();
        alarmType = entityAlarm.getAlarmType();
        createdTime = entityAlarm.getCreatedTime();
        if (entityAlarm.getCustomerId() != null) {
            customerId = entityAlarm.getCustomerId().getId();
        }
    }

    @Override
    public EntityAlarm toData() {
        EntityAlarm result = new EntityAlarm();
        result.setTenantId(TenantId.fromUUID(tenantId));
        result.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        result.setAlarmId(new AlarmId(alarmId));
        result.setAlarmType(alarmType);
        result.setCreatedTime(createdTime);
        if (customerId != null) {
            result.setCustomerId(new CustomerId(customerId));
        }
        return result;
    }

}