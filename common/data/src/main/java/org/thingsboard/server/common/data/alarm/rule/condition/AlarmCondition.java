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
package org.thingsboard.server.common.data.alarm.rule.condition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AlarmSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AnyTimeSchedule;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "SIMPLE", value = SimpleAlarmCondition.class),
        @Type(name = "DURATION", value = DurationAlarmCondition.class),
        @Type(name = "REPEATING", value = RepeatingAlarmCondition.class),
})
@Data
@NoArgsConstructor
public abstract class AlarmCondition {

    @NotNull
    @Valid
    private AlarmConditionExpression expression;
    @Valid
    private AlarmConditionValue<AlarmSchedule> schedule;

    @JsonIgnore
    public boolean hasSchedule() {
        return schedule != null && !(schedule.getStaticValue() instanceof AnyTimeSchedule);
    }

    @JsonIgnore
    public boolean requiresScheduledReevaluation() {
        return hasSchedule() || expression.requiresScheduledReevaluation();
    }

    @JsonIgnore
    @AssertTrue(message = "Expressions requiring scheduled reevaluation can only be used with simple alarm conditions")
    public boolean isValid() {
        if (getType() != AlarmConditionType.SIMPLE && expression.requiresScheduledReevaluation()) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public abstract AlarmConditionType getType();

}
