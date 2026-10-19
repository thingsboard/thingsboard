// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@ToString
@EqualsAndHashCode
@Setter
public class EntityViewTypeFilter implements EntityFilter {

    /**
     * Replaced by {@link EntityViewTypeFilter#getEntityViewTypes()} instead.
     */
    @Deprecated(since = "3.5", forRemoval = true)
    private String entityViewType;

    private List<String> entityViewTypes;

    public List<String> getEntityViewTypes() {
        return !CollectionUtils.isEmpty(entityViewTypes) ? entityViewTypes : Collections.singletonList(entityViewType);
    }

    @Getter
    private String entityViewNameFilter;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_VIEW_TYPE;
    }

}
