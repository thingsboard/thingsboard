// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(
        name = "AlarmRuleKeyFilterPredicate",
        description = "Filter predicate for alarm rule key-based filtering",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "STRING", schema = StringFilterPredicate.class),
                @DiscriminatorMapping(value = "NUMERIC", schema = NumericFilterPredicate.class),
                @DiscriminatorMapping(value = "BOOLEAN", schema = BooleanFilterPredicate.class),
                @DiscriminatorMapping(value = "NO_DATA", schema = NoDataFilterPredicate.class),
                @DiscriminatorMapping(value = "COMPLEX", schema = ComplexFilterPredicate.class)
        }
)
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
