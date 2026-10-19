// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.FilterPredicateType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleAlarmConditionExpression implements AlarmConditionExpression {

    @Valid
    @NotEmpty
    private List<AlarmConditionFilter> filters;
    private ComplexOperation operation;

    @Override
    public AlarmConditionExpressionType getType() {
        return AlarmConditionExpressionType.SIMPLE;
    }

    @Override
    public boolean requiresScheduledReevaluation() {
        return filters.stream().anyMatch(filter -> filter.hasPredicate(FilterPredicateType.NO_DATA));
    }

}
