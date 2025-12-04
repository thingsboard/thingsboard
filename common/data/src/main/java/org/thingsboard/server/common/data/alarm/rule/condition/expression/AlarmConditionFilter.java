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
package org.thingsboard.server.common.data.alarm.rule.condition.expression;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.ComplexFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.FilterPredicateType;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.EntityKeyValueType;

import java.io.Serializable;
import java.util.List;

@Data
public class AlarmConditionFilter implements Serializable {

    @NotBlank
    private String argument;
    @NotNull
    private EntityKeyValueType valueType;
    private ComplexOperation operation;
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
