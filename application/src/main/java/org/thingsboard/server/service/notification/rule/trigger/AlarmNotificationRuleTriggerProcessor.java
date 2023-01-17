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
package org.thingsboard.server.service.notification.rule.trigger;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;

@Service
public class AlarmNotificationRuleTriggerProcessor implements NotificationRuleTriggerProcessor<Alarm, AlarmNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        return (CollectionUtils.isEmpty(triggerConfig.getAlarmTypes()) || triggerConfig.getAlarmTypes().contains(alarm.getType())) &&
                (CollectionUtils.isEmpty(triggerConfig.getAlarmSeverities()) || triggerConfig.getAlarmSeverities().contains(alarm.getSeverity()));
    }

    @Override
    public boolean matchesClearRule(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        AlarmNotificationRuleTriggerConfig.ClearRule clearRule = triggerConfig.getClearRule();
        if (clearRule != null) {
            if (clearRule.getAlarmStatus() != null) {
                return clearRule.getAlarmStatus().equals(alarm.getStatus());
            }
        }
        return false;
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

}
