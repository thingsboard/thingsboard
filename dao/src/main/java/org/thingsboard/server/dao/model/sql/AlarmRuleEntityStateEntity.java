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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_ENTITY_STATE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_ENTITY_STATE_DATA_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_ENTITY_STATE_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_ENTITY_STATE_ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_ENTITY_STATE_TENANT_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = ALARM_RULE_ENTITY_STATE_COLUMN_FAMILY_NAME)
@Entity
public class AlarmRuleEntityStateEntity extends BaseSqlEntity<AlarmRuleEntityState> {

    @Column(name = ALARM_RULE_ENTITY_STATE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ALARM_RULE_ENTITY_STATE_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = ALARM_RULE_ENTITY_STATE_ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = ALARM_RULE_ENTITY_STATE_DATA_PROPERTY)
    private String data;

    public AlarmRuleEntityStateEntity() {
        super();
    }

    public AlarmRuleEntityStateEntity(AlarmRuleEntityState alarmRuleEntityState) {
        this.tenantId = alarmRuleEntityState.getTenantId().getId();
        this.entityId = alarmRuleEntityState.getEntityId().getId();
        this.data = alarmRuleEntityState.getData();
    }

    @Override
    public AlarmRuleEntityState toData() {
        AlarmRuleEntityState alarmRuleEntityState = new AlarmRuleEntityState();
        alarmRuleEntityState.setTenantId(TenantId.fromUUID(tenantId));
        alarmRuleEntityState.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType.name(), entityId));
        alarmRuleEntityState.setData(data);
        return alarmRuleEntityState;
    }
}
