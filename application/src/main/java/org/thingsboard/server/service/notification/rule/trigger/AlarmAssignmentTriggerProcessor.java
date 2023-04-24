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
package org.thingsboard.server.service.notification.rule.trigger;

import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.notification.info.AlarmAssignmentNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentNotificationRuleTriggerConfig.Action;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.notification.trigger.RuleEngineMsgTrigger;

import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

@Service
public class AlarmAssignmentTriggerProcessor implements RuleEngineMsgNotificationRuleTriggerProcessor<AlarmAssignmentNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(RuleEngineMsgTrigger trigger, AlarmAssignmentNotificationRuleTriggerConfig triggerConfig) {
        Action action = trigger.getMsg().getType().equals(DataConstants.ALARM_ASSIGN) ? Action.ASSIGNED : Action.UNASSIGNED;
        if (!triggerConfig.getNotifyOn().contains(action)) {
            return false;
        }
        Alarm alarm = JacksonUtil.fromString(trigger.getMsg().getData(), Alarm.class);
        return emptyOrContains(triggerConfig.getAlarmTypes(), alarm.getType()) &&
                emptyOrContains(triggerConfig.getAlarmSeverities(), alarm.getSeverity()) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarm));
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(RuleEngineMsgTrigger trigger) {
        AlarmInfo alarmInfo = JacksonUtil.fromString(trigger.getMsg().getData(), AlarmInfo.class);
        AlarmAssignee assignee = alarmInfo.getAssignee();
        return AlarmAssignmentNotificationInfo.builder()
                .action(trigger.getMsg().getType().equals(DataConstants.ALARM_ASSIGN) ? "assigned" : "unassigned")
                .assigneeFirstName(assignee != null ? assignee.getFirstName() : null)
                .assigneeLastName(assignee != null ? assignee.getLastName() : null)
                .assigneeEmail(assignee != null ? assignee.getEmail() : null)
                .assigneeId(assignee != null ? assignee.getId() : null)
                .userEmail(trigger.getMsg().getMetaData().getValue("userEmail"))
                .userFirstName(trigger.getMsg().getMetaData().getValue("userFirstName"))
                .userLastName(trigger.getMsg().getMetaData().getValue("userLastName"))
                .alarmId(alarmInfo.getUuidId())
                .alarmType(alarmInfo.getType())
                .alarmOriginator(alarmInfo.getOriginator())
                .alarmOriginatorName(alarmInfo.getOriginatorName())
                .alarmSeverity(alarmInfo.getSeverity())
                .alarmStatus(alarmInfo.getStatus())
                .alarmCustomerId(alarmInfo.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_ASSIGNMENT;
    }

    @Override
    public Set<String> getSupportedMsgTypes() {
        return Set.of(DataConstants.ALARM_ASSIGN, DataConstants.ALARM_UNASSIGN);
    }

}
