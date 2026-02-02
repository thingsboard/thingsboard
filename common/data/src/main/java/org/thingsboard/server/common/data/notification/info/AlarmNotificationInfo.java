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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmNotificationInfo implements RuleOriginatedNotificationInfo {

    private String alarmType;
    private String action;
    private UUID alarmId;
    private EntityId alarmOriginator;
    private String alarmOriginatorName;
    private AlarmSeverity alarmSeverity;
    private AlarmStatus alarmStatus;
    private boolean acknowledged;
    private boolean cleared;
    private CustomerId alarmCustomerId;
    private DashboardId dashboardId;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "alarmType", alarmType,
                "action", action,
                "alarmId", alarmId.toString(),
                "alarmSeverity", alarmSeverity.name().toLowerCase(),
                "alarmStatus", alarmStatus.toString(),
                "alarmOriginatorEntityType", alarmOriginator.getEntityType().getNormalName(),
                "alarmOriginatorName", alarmOriginatorName,
                "alarmOriginatorId", alarmOriginator.getId().toString()
        );
    }

    @Override
    public CustomerId getAffectedCustomerId() {
        return alarmCustomerId;
    }

    @Override
    public EntityId getStateEntityId() {
        return alarmOriginator;
    }

    @Override
    public DashboardId getDashboardId() {
        return dashboardId;
    }

}
