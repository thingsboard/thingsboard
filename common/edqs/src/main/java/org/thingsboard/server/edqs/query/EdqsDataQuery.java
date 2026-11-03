// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
