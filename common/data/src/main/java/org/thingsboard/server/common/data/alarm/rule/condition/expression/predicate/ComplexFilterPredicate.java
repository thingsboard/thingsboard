// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.query.ComplexOperation;

import java.util.List;

@Schema(name = "AlarmRuleComplexFilterPredicate")
@Data
public class ComplexFilterPredicate implements KeyFilterPredicate {

    private ComplexOperation operation;
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/AlarmRuleKeyFilterPredicate"))
    private List<KeyFilterPredicate> predicates;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, ref = "#/components/schemas/AlarmRuleFilterPredicateType")
    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.COMPLEX;
    }

}
