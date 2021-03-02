/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DurationAlarmConditionSpec implements AlarmConditionSpec {

    private TimeUnit unit;
    private long value;

    public DurationAlarmConditionSpec() {
    }

    public DurationAlarmConditionSpec(TimeUnit unit, long value) {
        this.unit = unit;
        this.value = value;
    }

    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.DURATION;
    }
}
