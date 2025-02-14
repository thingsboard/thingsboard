/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.common.data.alarm.AlarmType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

@Data
@Entity
@Table(name = ModelConstants.ALARM_TYPES_TABLE_NAME)
@IdClass(AlarmTypeCompositeKey.class)
public class AlarmTypeEntity implements ToData<AlarmType> {

    @Id
    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = ModelConstants.ALARM_TYPE_PROPERTY, nullable = false)
    private String type;

    public AlarmTypeEntity() {}

    public AlarmTypeEntity(AlarmType alarmType) {
        setTenantId(alarmType.getTenantId().getId());
        setType(alarmType.getType());
    }

    public AlarmTypeEntity(UUID tenantId, String type) {
        this.tenantId = tenantId;
        this.type = type;
    }

    @Override
    public AlarmType toData() {
        AlarmType alarmType = new AlarmType();
        alarmType.setTenantId(TenantId.fromUUID(tenantId));
        alarmType.setType(type);
        return alarmType;
    }

}
