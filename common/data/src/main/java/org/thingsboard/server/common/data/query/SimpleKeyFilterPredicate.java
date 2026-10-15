// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Base interface for simple key filter predicates")
public interface SimpleKeyFilterPredicate<T> extends KeyFilterPredicate {

    @Schema(description = "The value associated with the filter predicate")
    FilterPredicateValue<T> getValue();

}
