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
package org.thingsboard.server.service.notification.rule.trigger;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.notification.info.AlarmAssignmentNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmAssignmentNotificationRuleTriggerConfig.Action;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

@Service
public class AlarmAssignmentTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmAssignmentTrigger, AlarmAssignmentNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(AlarmAssignmentTrigger trigger, AlarmAssignmentNotificationRuleTriggerConfig triggerConfig) {
        Action action = trigger.getActionType() == ActionType.ALARM_ASSIGNED ? Action.ASSIGNED : Action.UNASSIGNED;
        if (!triggerConfig.getNotifyOn().contains(action)) {
            return false;
        }
        AlarmInfo alarmInfo = trigger.getAlarmInfo();
        return emptyOrContains(triggerConfig.getAlarmTypes(), alarmInfo.getType()) &&
                emptyOrContains(triggerConfig.getAlarmSeverities(), alarmInfo.getSeverity()) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarmInfo));
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(AlarmAssignmentTrigger trigger) {
        AlarmInfo alarmInfo = trigger.getAlarmInfo();
        AlarmAssignee assignee = alarmInfo.getAssignee();
        return AlarmAssignmentNotificationInfo.builder()
                .action(trigger.getActionType() == ActionType.ALARM_ASSIGNED ? "assigned" : "unassigned")
                .assigneeFirstName(assignee != null ? assignee.getFirstName() : null)
                .assigneeLastName(assignee != null ? assignee.getLastName() : null)
                .assigneeEmail(assignee != null ? assignee.getEmail() : null)
                .assigneeId(assignee != null ? assignee.getId() : null)
                .userEmail(trigger.getUser().getEmail())
                .userFirstName(trigger.getUser().getFirstName())
                .userLastName(trigger.getUser().getLastName())
                .alarmId(alarmInfo.getUuidId())
                .alarmType(alarmInfo.getType())
                .alarmOriginator(alarmInfo.getOriginator())
                .alarmOriginatorName(alarmInfo.getOriginatorName())
                .alarmSeverity(alarmInfo.getSeverity())
                .alarmStatus(alarmInfo.getStatus())
                .alarmCustomerId(alarmInfo.getCustomerId())
                .dashboardId(alarmInfo.getDashboardId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_ASSIGNMENT;
    }

}
