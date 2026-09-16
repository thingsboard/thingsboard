// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;

import java.util.concurrent.TimeUnit;

@Schema
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoDataFilterPredicate implements KeyFilterPredicate {

    @NotNull
    private TimeUnit unit;
    @Valid
    @NotNull
    private AlarmConditionValue<Long> duration;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, ref = "#/components/schemas/AlarmRuleFilterPredicateType")
    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.NO_DATA;
    }

}
