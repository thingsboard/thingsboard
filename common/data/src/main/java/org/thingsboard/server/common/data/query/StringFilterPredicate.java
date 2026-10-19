// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

@Data
@Schema
public class StringFilterPredicate implements SimpleKeyFilterPredicate<String> {

    private StringOperation operation;
    @Valid
    private FilterPredicateValue<String> value;
    private boolean ignoreCase;

    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.STRING;
    }

    public enum StringOperation {
        EQUAL,
        NOT_EQUAL,
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS,
        NOT_CONTAINS,
        IN,
        NOT_IN
    }
}
