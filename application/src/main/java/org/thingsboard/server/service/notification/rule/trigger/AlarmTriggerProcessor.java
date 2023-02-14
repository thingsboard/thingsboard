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

import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig.ClearRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.service.notification.rule.trigger.AlarmTriggerProcessor.AlarmTriggerObject;

@Service
public class AlarmTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmTriggerObject, AlarmNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(AlarmTriggerObject triggerObject, AlarmNotificationRuleTriggerConfig triggerConfig) {
        Alarm alarm = triggerObject.getAlarm();
        return (CollectionUtils.isEmpty(triggerConfig.getAlarmTypes()) || triggerConfig.getAlarmTypes().contains(alarm.getType())) &&
                (CollectionUtils.isEmpty(triggerConfig.getAlarmSeverities()) || triggerConfig.getAlarmSeverities().contains(alarm.getSeverity()));
    }

    @Override
    public boolean matchesClearRule(AlarmTriggerObject triggerObject, AlarmNotificationRuleTriggerConfig triggerConfig) {
        if (triggerObject.isDeleted()) {
            return true;
        }
        Alarm alarm = triggerObject.getAlarm();
        ClearRule clearRule = triggerConfig.getClearRule();
        if (clearRule != null) {
            if (clearRule.getAlarmStatus() != null) {
                return clearRule.getAlarmStatus().equals(alarm.getStatus());
            }
        }
        return false;
    }

    @Override
    public NotificationInfo constructNotificationInfo(AlarmTriggerObject triggerObject, AlarmNotificationRuleTriggerConfig triggerConfig) {
        Alarm alarm = triggerObject.getAlarm();
        return AlarmNotificationInfo.builder()
                .alarmId(alarm.getUuidId())
                .alarmType(alarm.getType())
                .alarmOriginator(alarm.getOriginator())
                .alarmSeverity(alarm.getSeverity())
                .alarmStatus(alarm.getStatus())
                .alarmCustomerId(alarm.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

    @Data
    @Builder
    public static class AlarmTriggerObject {
        private final Alarm alarm;
        private final boolean deleted;
    }

}
