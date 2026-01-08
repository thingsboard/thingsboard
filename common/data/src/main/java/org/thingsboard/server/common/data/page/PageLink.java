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
package org.thingsboard.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class PageLink {

    protected static final String DEFAULT_SORT_PROPERTY = "id";
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, DEFAULT_SORT_PROPERTY);

    private final String textSearch;
    private final int pageSize;
    private final int page;
    private final SortOrder sortOrder;

    public PageLink(PageLink pageLink) {
        this.pageSize = pageLink.getPageSize();
        this.page = pageLink.getPage();
        this.textSearch = pageLink.getTextSearch();
        this.sortOrder = pageLink.getSortOrder();
    }

    public PageLink(int pageSize) {
        this(pageSize, 0);
    }

    public PageLink(int pageSize, int page) {
        this(pageSize, page, null, null);
    }

    public PageLink(int pageSize, int page, String textSearch) {
        this(pageSize, page, textSearch, null);
    }

    public PageLink(int pageSize, int page, String textSearch, SortOrder sortOrder) {
        this.pageSize = pageSize;
        this.page = page;
        this.textSearch = textSearch;
        this.sortOrder = sortOrder;
    }

    @JsonIgnore
    public PageLink nextPageLink() {
        return new PageLink(this.pageSize, this.page + 1, this.textSearch, this.sortOrder);
    }

    public Sort toSort(SortOrder sortOrder, Map<String, String> columnMap, boolean addDefaultSorting) {
        if (sortOrder == null) {
            return DEFAULT_SORT;
        } else {
            return toSort(List.of(sortOrder), columnMap, addDefaultSorting);
        }
    }

    public Sort toSort(List<SortOrder> sortOrders, Map<String, String> columnMap, boolean addDefaultSorting) {
        if (addDefaultSorting && !isDefaultSortOrderAvailable(sortOrders)) {
            sortOrders = new ArrayList<>(sortOrders);
            sortOrders.add(new SortOrder(DEFAULT_SORT_PROPERTY, SortOrder.Direction.ASC));
        }
        return Sort.by(sortOrders.stream().map(s -> toSortOrder(s, columnMap)).collect(Collectors.toList()));
    }

    private Sort.Order toSortOrder(SortOrder sortOrder, Map<String, String> columnMap) {
        String property = sortOrder.getProperty();
        if (columnMap.containsKey(property)) {
            property = columnMap.get(property);
        }
        return new Sort.Order(Sort.Direction.fromString(sortOrder.getDirection().name()), property);
    }

    public boolean isDefaultSortOrderAvailable(List<SortOrder> sortOrders) {
        for (SortOrder sortOrder : sortOrders) {
            if (DEFAULT_SORT_PROPERTY.equals(sortOrder.getProperty())) {
                return true;
            }
        }
        return false;
    }

}
