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
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.notification.info.AlarmCommentNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmCommentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.notification.trigger.AlarmCommentTrigger;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

@Service
public class AlarmCommentTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmCommentTrigger, AlarmCommentNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(AlarmCommentTrigger trigger, AlarmCommentNotificationRuleTriggerConfig triggerConfig) {
        if (trigger.getActionType() == ActionType.UPDATED_COMMENT && !triggerConfig.isNotifyOnCommentUpdate()) {
            return false;
        }
        if (triggerConfig.isOnlyUserComments()) {
            if (trigger.getComment().getType() == AlarmCommentType.SYSTEM) {
                return false;
            }
        }
        AlarmInfo alarmInfo = trigger.getAlarmInfo();
        return emptyOrContains(triggerConfig.getAlarmTypes(), alarmInfo.getType()) &&
                emptyOrContains(triggerConfig.getAlarmSeverities(), alarmInfo.getSeverity()) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarmInfo));
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(AlarmCommentTrigger trigger) {
        AlarmInfo alarmInfo = trigger.getAlarmInfo();
        return AlarmCommentNotificationInfo.builder()
                .comment(trigger.getComment().getComment().get("text").asText())
                .action(trigger.getActionType() == ActionType.ADDED_COMMENT? "added" : "updated")
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
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_COMMENT;
    }

}
