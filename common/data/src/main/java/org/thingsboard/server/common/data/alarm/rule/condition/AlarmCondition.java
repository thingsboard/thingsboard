// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AlarmSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AnyTimeSchedule;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SIMPLE", schema = SimpleAlarmCondition.class),
                @DiscriminatorMapping(value = "DURATION", schema = DurationAlarmCondition.class),
                @DiscriminatorMapping(value = "REPEATING", schema = RepeatingAlarmCondition.class)
        }
)
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
