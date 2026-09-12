// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;

@Schema(name = "AlarmRuleBooleanFilterPredicate")
@Data
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
