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
public class EdgeTypeFilter implements EntityFilter {

    /**
     * Replaced by {@link EdgeTypeFilter#getEdgeTypes()} instead.
     */
    @Deprecated(since = "3.5", forRemoval = true)
    private String edgeType;

    private List<String> edgeTypes;

    public List<String> getEdgeTypes() {
        return !CollectionUtils.isEmpty(edgeTypes) ? edgeTypes : Collections.singletonList(edgeType);
    }

    @Getter
    private String edgeNameFilter;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.EDGE_TYPE;
    }

}
