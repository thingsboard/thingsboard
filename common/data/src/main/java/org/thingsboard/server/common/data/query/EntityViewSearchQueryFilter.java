// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EntityViewSearchQueryFilter extends EntitySearchQueryFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_VIEW_SEARCH_QUERY;
    }

    private List<String> entityViewTypes;

}
