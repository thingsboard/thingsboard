/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sqlts.latest;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@SqlTsLatestAnyDao
@Repository
public class SearchTsKvLatestRepository {

    public static final String FIND_ALL_BY_ENTITY_ID = "findAllByEntityId";

    public static final String FIND_ALL_BY_ENTITY_IDS = "findAllByDeviceIds";

    public static final String FIND_ALL_BY_ENTITY_ID_QUERY = "SELECT ts_kv_latest.entity_id AS entityId, ts_kv_latest.key AS key, ts_kv_dictionary.key AS strKey, ts_kv_latest.str_v AS strValue," +
            " ts_kv_latest.bool_v AS boolValue, ts_kv_latest.long_v AS longValue, ts_kv_latest.dbl_v AS doubleValue, ts_kv_latest.json_v AS jsonValue, ts_kv_latest.ts AS ts FROM ts_kv_latest " +
            "INNER JOIN ts_kv_dictionary ON ts_kv_latest.key = ts_kv_dictionary.key_id WHERE ts_kv_latest.entity_id = cast(:id AS uuid)";

    public static final String FIND_ALL_BY_ENTITY_IDS_QUERY = "SELECT ts_kv_latest.entity_id AS entityId, ts_kv_latest.key AS key, ts_kv_dictionary.key AS strKey, ts_kv_latest.str_v AS strValue," +
            " ts_kv_latest.bool_v AS boolValue, ts_kv_latest.long_v AS longValue, ts_kv_latest.dbl_v AS doubleValue, ts_kv_latest.json_v AS jsonValue, ts_kv_latest.ts AS ts FROM ts_kv_latest " +
            "INNER JOIN ts_kv_dictionary ON ts_kv_latest.key = ts_kv_dictionary.key_id WHERE ts_kv_latest.entity_id in :ids";

    @PersistenceContext
    private EntityManager entityManager;

    public List<TsKvLatestEntity> findAllByEntityId(UUID entityId) {
        return entityManager.createNamedQuery(FIND_ALL_BY_ENTITY_ID, TsKvLatestEntity.class)
                .setParameter("id", entityId)
                .getResultList();
    }

    public List<TsKvLatestEntity> findAllByEntityIds(List<UUID> deviceIds) {
        return entityManager.createNamedQuery(FIND_ALL_BY_ENTITY_IDS, TsKvLatestEntity.class)
                .setParameter("ids", deviceIds)
                .getResultList();
    }
}
