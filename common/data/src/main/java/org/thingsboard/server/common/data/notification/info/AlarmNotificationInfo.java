/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private String alarmOriginatorLabel;
    private AlarmSeverity alarmSeverity;
    private AlarmStatus alarmStatus;
    private boolean acknowledged;
    private boolean cleared;
    private CustomerId alarmCustomerId;
    private DashboardId dashboardId;
    private Map<String, String> details;

    @Override
    public Map<String, String> getTemplateData() {
        Map<String, String> templateData = new HashMap<>(details);
        templateData.put("alarmType", alarmType);
        templateData.put("action", action);
        templateData.put("alarmId", alarmId.toString());
        templateData.put("alarmSeverity", alarmSeverity.name().toLowerCase());
        templateData.put("alarmStatus", alarmStatus.toString());
        templateData.put("alarmOriginatorEntityType", alarmOriginator.getEntityType().getNormalName());
        templateData.put("alarmOriginatorName", alarmOriginatorName);
        templateData.put("alarmOriginatorLabel", alarmOriginatorLabel);
        templateData.put("alarmOriginatorId", alarmOriginator.getId().toString());
        return templateData;
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
