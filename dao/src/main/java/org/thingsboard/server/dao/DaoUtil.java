/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.model.ToData;

import java.util.*;
import java.util.stream.Collectors;

public abstract class DaoUtil {

    private DaoUtil() {
    }

    public static <I extends UUIDBased, D extends SearchTextBased<I>> List<D> filterDataListByRelation(List<EntityRelation> relations, List<D> dataList) {
        List<UUID> ids = relations.stream().map(entityRelation -> entityRelation.getTo().getId()).collect(Collectors.toList());
        List<D> result = new ArrayList<>();
        for (D data : dataList) {
            if (ids.contains(data.getId().getId())) {
                result.add(data);
            }
        }
        return result;
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
