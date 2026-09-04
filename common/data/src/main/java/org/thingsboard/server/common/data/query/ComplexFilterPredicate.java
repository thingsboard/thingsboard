// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;

import java.util.List;

@Data
public class ComplexFilterPredicate implements KeyFilterPredicate {

    private ComplexOperation operation;
    private List<KeyFilterPredicate> predicates;

    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.COMPLEX;
    }

    public enum ComplexOperation {
        AND,
        OR
    }
}
