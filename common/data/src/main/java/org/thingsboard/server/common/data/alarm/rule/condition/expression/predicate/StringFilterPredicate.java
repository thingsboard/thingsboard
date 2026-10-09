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

@Schema(name = "AlarmRuleStringFilterPredicate")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringFilterPredicate implements SimpleKeyFilterPredicate<String> {

    @NotNull
    private StringOperation operation;
    @Valid
    @NotNull
    private AlarmConditionValue<String> value;
    private boolean ignoreCase;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, ref = "#/components/schemas/AlarmRuleFilterPredicateType")
    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.STRING;
    }

    @Schema(name= "AlarmRuleStringOperation")
    public enum StringOperation {
        EQUAL,
        NOT_EQUAL,
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS,
        NOT_CONTAINS,
        IN,
        NOT_IN
    }

}
