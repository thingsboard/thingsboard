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
package org.thingsboard.rule.engine.action;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TbAlarmResult {

    boolean isCreated;
    boolean isUpdated;
    boolean isSeverityUpdated;
    boolean isCleared;
    Alarm alarm;

    Long conditionRepeats;
    Long conditionDuration;

    public TbAlarmResult(boolean isCreated, boolean isUpdated, boolean isCleared, Alarm alarm) {
        this.isCreated = isCreated;
        this.isUpdated = isUpdated;
        this.isCleared = isCleared;
        this.alarm = alarm;
    }

    public static TbAlarmResult fromAlarmResult(AlarmApiCallResult result) {
        boolean isSeverityChanged = result.isSeverityChanged();
        return TbAlarmResult.builder()
                .isCreated(result.isCreated())
                .isUpdated(result.isModified() && !isSeverityChanged)
                .isSeverityUpdated(isSeverityChanged)
                .isCleared(result.isCleared())
                .alarm(result.getAlarm())
                .build();
    }

}
