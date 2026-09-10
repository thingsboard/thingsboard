// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "SIMPLE", schema = SimpleAlarmConditionExpression.class),
                @DiscriminatorMapping(value = "TBEL", schema = TbelAlarmConditionExpression.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "SIMPLE", value = SimpleAlarmConditionExpression.class),
        @Type(name = "TBEL", value = TbelAlarmConditionExpression.class),
})
public interface AlarmConditionExpression {

    @JsonIgnore
    AlarmConditionExpressionType getType();

    @JsonIgnore
    default boolean requiresScheduledReevaluation() {
        return false;
    }

}
