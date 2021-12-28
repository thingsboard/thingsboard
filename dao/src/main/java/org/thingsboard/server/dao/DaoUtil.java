/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.model.ToData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class DaoUtil {

    public static final String DEFAULT_SORT_PROPERTY = "id";
    public static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, DEFAULT_SORT_PROPERTY);

    private DaoUtil() {
    }

    public static <T> PageData<T> toPageData(Page<? extends ToData<T>> page) {
        List<T> data = convertDataList(page.getContent());
        return new PageData<>(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static <T> PageData<T> pageToPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static Pageable toPageable(PageLink pageLink) {
        return toPageable(pageLink, Collections.emptyMap());
    }

    public static Pageable toPageable(PageLink pageLink, Map<String,String> columnMap) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), toSort(pageLink.getSortOrder(), columnMap));
    }

    public static Pageable toPageable(PageLink pageLink, List<SortOrder> sortOrders) {
        return toPageable(pageLink, Collections.emptyMap(), sortOrders);
    }

    public static Pageable toPageable(PageLink pageLink, Map<String,String> columnMap, List<SortOrder> sortOrders) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), toSort(sortOrders, columnMap));
    }

    public static Sort toSort(SortOrder sortOrder) {
        return toSort(sortOrder, Collections.emptyMap());
    }

    public static Sort toSort(SortOrder sortOrder, Map<String,String> columnMap) {
        if (sortOrder == null) {
            return DEFAULT_SORT;
        } else {
            String property = sortOrder.getProperty();
            if (columnMap.containsKey(property)) {
                property = columnMap.get(property);
            }
            return Sort.by(Sort.Direction.fromString(sortOrder.getDirection().name()), property);
        }
    }

    public static Sort toSort(List<SortOrder> sortOrders) {
        return toSort(sortOrders, Collections.emptyMap());
    }

    public static Sort toSort(List<SortOrder> sortOrders, Map<String,String> columnMap) {
        return toSort(sortOrders, columnMap, Sort.NullHandling.NULLS_LAST);
    }

    public static Sort toSort(List<SortOrder> sortOrders, Map<String,String> columnMap, Sort.NullHandling nullHandlingHint) {
        return Sort.by(sortOrders.stream().map(s -> toSortOrder(s, columnMap, nullHandlingHint)).collect(Collectors.toList()));
    }

    public static Sort.Order toSortOrder(SortOrder sortOrder, Map<String,String> columnMap, Sort.NullHandling nullHandlingHint) {
        String property = sortOrder.getProperty();
        if (columnMap.containsKey(property)) {
            property = columnMap.get(property);
        }
        return new Sort.Order(Sort.Direction.fromString(sortOrder.getDirection().name()), property, nullHandlingHint);
    }

    public static <T> List<T> convertDataList(Collection<? extends ToData<T>> toDataList) {
        List<T> list = Collections.emptyList();
        if (toDataList != null && !toDataList.isEmpty()) {
            list = new ArrayList<>();
            for (ToData<T> object : toDataList) {
                if (object != null) {
                    list.add(object.toData());
                }
            }
        }
        return list;
    }

    public static <T> T getData(ToData<T> data) {
        T object = null;
        if (data != null) {
            object = data.toData();
        }
        return object;
    }

    public static <T> T getData(Optional<? extends ToData<T>> data) {
        T object = null;
        if (data.isPresent()) {
            object = data.get().toData();
        }
        return object;
    }

    public static UUID getId(UUIDBased idBased) {
        UUID id = null;
        if (idBased != null) {
            id = idBased.getId();
        }
        return id;
    }

    public static List<UUID> toUUIDs(List<? extends UUIDBased> idBasedIds) {
        List<UUID> ids = new ArrayList<>();
        for (UUIDBased idBased : idBasedIds) {
            ids.add(getId(idBased));
        }
        return ids;
    }

}
