// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query;

import lombok.Builder;
import org.thingsboard.server.common.data.query.ComplexOperation;
import org.thingsboard.server.common.data.query.EntityFilter;

import java.util.List;

public class EdqsCountQuery extends EdqsQuery {

    @Builder
    EdqsCountQuery(EntityFilter entityFilter, boolean hasKeyFilters, List<EdqsFilter> keyFilters, ComplexOperation keyFiltersOperation) {
        super(entityFilter, hasKeyFilters, keyFilters, keyFiltersOperation);
    }

}
