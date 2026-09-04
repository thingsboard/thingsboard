// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class NumericFilterPredicate implements SimpleKeyFilterPredicate<Double>  {

    private NumericOperation operation;
    private FilterPredicateValue<Double> value;

    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.NUMERIC;
    }

    public enum NumericOperation {
        EQUAL,
        NOT_EQUAL,
        GREATER,
        LESS,
        GREATER_OR_EQUAL,
        LESS_OR_EQUAL
    }
}
