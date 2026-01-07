/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
