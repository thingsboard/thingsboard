// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.ComplexFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.FilterPredicateType;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.EntityKeyValueType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Schema
@Data
public class AlarmConditionFilter implements Serializable {

    @NotBlank
    private String argument;
    @NotNull
    private EntityKeyValueType valueType;
    private ComplexOperation operation;
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/AlarmRuleKeyFilterPredicate"))
    @Valid
    @NotEmpty
    private List<KeyFilterPredicate> predicates;

    public boolean hasPredicate(FilterPredicateType type) {
        return containsPredicate(predicates, type);
    }

    private boolean containsPredicate(List<KeyFilterPredicate> predicates, FilterPredicateType type) {
        return predicates.stream().anyMatch(predicate -> {
            if (predicate instanceof ComplexFilterPredicate complexPredicate) {
                return containsPredicate(complexPredicate.getPredicates(), type);
            } else {
                return predicate.getType() == type;
            }
        });
    }

}
