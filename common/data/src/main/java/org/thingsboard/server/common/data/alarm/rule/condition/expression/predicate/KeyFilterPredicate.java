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
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = StringFilterPredicate.class, name = "STRING"),
        @Type(value = NumericFilterPredicate.class, name = "NUMERIC"),
        @Type(value = BooleanFilterPredicate.class, name = "BOOLEAN"),
        @Type(value = NoDataFilterPredicate.class, name = "NO_DATA"),
        @Type(value = ComplexFilterPredicate.class, name = "COMPLEX")
})
public interface KeyFilterPredicate extends Serializable {

    @JsonIgnore
    FilterPredicateType getType();

}
