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
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Service
public class AlarmAssignmentTriggerProcessor implements RuleEngineMsgNotificationRuleTriggerProcessor<AlarmAssignmentNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(TbMsg ruleEngineMsg, AlarmAssignmentNotificationRuleTriggerConfig triggerConfig) {
        if ((ruleEngineMsg.getType().equals(DataConstants.ALARM_ASSIGN) && !triggerConfig.isNotifyOnAssign())
                || (ruleEngineMsg.getType().equals(DataConstants.ALARM_UNASSIGN) && !triggerConfig.isNotifyOnUnassign())) {
            return false;
        }
        Alarm alarm = JacksonUtil.fromString(ruleEngineMsg.getData(), Alarm.class);
        return (isEmpty(triggerConfig.getAlarmTypes()) || triggerConfig.getAlarmTypes().contains(alarm.getType())) &&
                (isEmpty(triggerConfig.getAlarmSeverities()) || triggerConfig.getAlarmSeverities().contains(alarm.getSeverity())) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarm));
    }

    @Override
    public NotificationInfo constructNotificationInfo(TbMsg ruleEngineMsg, AlarmAssignmentNotificationRuleTriggerConfig triggerConfig) {
        AlarmInfo alarmInfo = JacksonUtil.fromString(ruleEngineMsg.getData(), AlarmInfo.class);
        AlarmAssignee assignee = alarmInfo.getAssignee();
        return AlarmAssignmentNotificationInfo.builder()
                .action(ruleEngineMsg.getType().equals(DataConstants.ALARM_ASSIGN) ? "assigned" : "unassigned")
                .assigneeFirstName(assignee != null ? assignee.getFirstName() : null)
                .assigneeLastName(assignee != null ? assignee.getLastName() : null)
                .assigneeEmail(assignee != null ? assignee.getEmail() : null)
                .assigneeId(assignee != null ? assignee.getId() : null)
                .userName(ruleEngineMsg.getMetaData().getValue("userName"))
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
