/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_ALARM_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_DESCRIPTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_ENABLED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_RULE_TENANT_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = ALARM_RULE_COLUMN_FAMILY_NAME)
@Entity
public class AlarmRuleInfoEntity extends BaseSqlEntity<AlarmRuleInfo> {

    @Column(name = ALARM_RULE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ALARM_RULE_ALARM_TYPE_PROPERTY)
    private String alarmType;

    @Column(name = ALARM_RULE_NAME_PROPERTY)
    private String name;

    @Column(name = ALARM_RULE_ENABLED_PROPERTY)
    private boolean enabled;

    @Column(name = ALARM_RULE_DESCRIPTION_PROPERTY)
    private String description;

    public AlarmRuleInfoEntity() {
        super();
    }

    public AlarmRuleInfoEntity(AlarmRuleInfo alarmRuleInfo) {
        if (alarmRuleInfo.getId() != null) {
            this.setUuid(alarmRuleInfo.getUuidId());
        }
        this.setCreatedTime(alarmRuleInfo.getCreatedTime());
        if (alarmRuleInfo.getTenantId() != null) {
            this.tenantId = alarmRuleInfo.getTenantId().getId();
        }
        this.alarmType = alarmRuleInfo.getAlarmType();
        this.name = alarmRuleInfo.getName();
        this.enabled = alarmRuleInfo.isEnabled();
        this.description = alarmRuleInfo.getDescription();
    }

    @Override
    public AlarmRuleInfo toData() {
        AlarmRuleInfo alarmRuleInfo = new AlarmRuleInfo(new AlarmRuleId(id));
        alarmRuleInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            alarmRuleInfo.setTenantId(TenantId.fromUUID(tenantId));
        }
        alarmRuleInfo.setAlarmType(alarmType);
        alarmRuleInfo.setName(name);
        alarmRuleInfo.setEnabled(enabled);
        alarmRuleInfo.setDescription(description);

        return alarmRuleInfo;
    }
}
