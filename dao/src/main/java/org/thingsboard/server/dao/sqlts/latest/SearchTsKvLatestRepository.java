// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sqlts.latest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import java.util.List;
import java.util.UUID;

@SqlTsLatestAnyDao
@Repository
public class SearchTsKvLatestRepository {

    public static final String FIND_ALL_BY_ENTITY_ID = "findAllByEntityId";

    public static final String FIND_ALL_BY_ENTITY_ID_QUERY = "SELECT ts_kv_latest.entity_id AS entityId, ts_kv_latest.key AS key, key_dictionary.key AS strKey, ts_kv_latest.str_v AS strValue," +
            " ts_kv_latest.bool_v AS boolValue, ts_kv_latest.long_v AS longValue, ts_kv_latest.dbl_v AS doubleValue, ts_kv_latest.json_v AS jsonValue, ts_kv_latest.ts AS ts, ts_kv_latest.version AS version FROM ts_kv_latest " +
            "INNER JOIN key_dictionary ON ts_kv_latest.key = key_dictionary.key_id WHERE ts_kv_latest.entity_id = cast(:id AS uuid)";

    @PersistenceContext
    private EntityManager entityManager;

    public List<TsKvLatestEntity> findAllByEntityId(UUID entityId) {
        return entityManager.createNamedQuery(FIND_ALL_BY_ENTITY_ID, TsKvLatestEntity.class)
                .setParameter("id", entityId)
                .getResultList();
    }

}
