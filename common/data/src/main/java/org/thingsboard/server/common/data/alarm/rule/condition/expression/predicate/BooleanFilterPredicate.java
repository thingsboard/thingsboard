// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;

@Schema(name = "AlarmRuleBooleanFilterPredicate")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BooleanFilterPredicate implements SimpleKeyFilterPredicate<Boolean> {

    @NotNull
    private BooleanOperation operation;
    @Valid
    @NotNull
    private AlarmConditionValue<Boolean> value;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, ref = "#/components/schemas/AlarmRuleFilterPredicateType")
    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.BOOLEAN;
    }

    @Schema(name = "AlarmRuleBooleanOperation")
    public enum BooleanOperation {
        EQUAL,
        NOT_EQUAL
    }

}
