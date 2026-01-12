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
package org.thingsboard.server.edqs.query;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
public class EdqsDataQuery extends EdqsQuery {

    private final int pageSize;
    private final int page;
    private final boolean hasTextSearch;
    private final String textSearch;
    private final boolean defaultSort;
    private final DataKey sortKey;
    private final EntityDataSortOrder.Direction sortDirection;
    private final List<DataKey> entityFields;
    private final List<DataKey> latestValues;

    @Builder
    public EdqsDataQuery(EntityFilter entityFilter, List<EdqsFilter> keyFilters,
                         int pageSize, int page, String textSearch, DataKey sortKey, EntityDataSortOrder.Direction sortDirection,
                         List<DataKey> entityFields, List<DataKey> latestValues) {
        super(entityFilter, CollectionsUtil.isNotEmpty(keyFilters), keyFilters);
        this.pageSize = pageSize;
        this.page = page;
        this.hasTextSearch = StringUtils.isNotBlank(textSearch);
        this.textSearch = textSearch;
        this.defaultSort = EntityKeyType.ENTITY_FIELD.equals(sortKey.type()) && "createdTime".equals(sortKey.key()) && EntityDataSortOrder.Direction.DESC.equals(sortDirection);
        this.sortKey = sortKey;
        this.sortDirection = sortDirection;
        this.entityFields = entityFields;
        this.latestValues = latestValues;
    }

}
