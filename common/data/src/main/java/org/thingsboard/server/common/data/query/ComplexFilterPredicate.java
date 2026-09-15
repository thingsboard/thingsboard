// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema
public class ComplexFilterPredicate implements KeyFilterPredicate {

    private ComplexOperation operation;
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/KeyFilterPredicate"))
    private List<KeyFilterPredicate> predicates;

    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.COMPLEX;
    }

}
