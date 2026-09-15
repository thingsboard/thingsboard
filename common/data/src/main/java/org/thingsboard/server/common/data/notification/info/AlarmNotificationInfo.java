// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        Map<String, String> templateData = details != null ? new HashMap<>(details) : new HashMap<>();
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
