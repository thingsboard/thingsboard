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
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @Serial
    private static final long serialVersionUID = -7382883720381542344L;

    private Set<String> alarmTypes;
    private Set<AlarmSeverity> alarmSeverities;
    @NotEmpty
    private Set<AlarmAction> notifyOn;

    private ClearRule clearRule;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

    @Data
    public static class ClearRule implements Serializable {
        @Serial
        private static final long serialVersionUID = 7922533150038105124L;
        private Set<AlarmSearchStatus> alarmStatuses;
    }

    public enum AlarmAction {
        CREATED, SEVERITY_CHANGED, ACKNOWLEDGED, CLEARED
    }

}
