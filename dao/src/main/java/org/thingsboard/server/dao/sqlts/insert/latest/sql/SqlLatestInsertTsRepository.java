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
package org.thingsboard.server.dao.sqlts.insert.latest.sql;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.AbstractVersionedInsertRepository;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@SqlTsLatestAnyDao
@Repository
public class SqlLatestInsertTsRepository extends AbstractVersionedInsertRepository<TsKvLatestEntity> implements InsertLatestTsRepository {

    @Value("${sql.ts_latest.update_by_latest_ts:true}")
    private Boolean updateByLatestTs;

    private static final String BATCH_UPDATE =
            "UPDATE ts_kv_latest SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json), version = nextval('ts_kv_latest_version_seq') WHERE entity_id = ? AND key = ?";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO ts_kv_latest (entity_id, key, ts, bool_v, str_v, long_v, dbl_v,  json_v, version) VALUES(?, ?, ?, ?, ?, ?, ?, cast(? AS json), nextval('ts_kv_latest_version_seq')) " +
                    "ON CONFLICT (entity_id, key) DO UPDATE SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json), version = nextval('ts_kv_latest_version_seq')";

    private static final String BATCH_UPDATE_BY_LATEST_TS = BATCH_UPDATE + " AND ts_kv_latest.ts <= ?";

    private static final String INSERT_OR_UPDATE_BY_LATEST_TS = INSERT_OR_UPDATE + " WHERE ts_kv_latest.ts <= ?";

    private static final String RETURNING = " RETURNING version";

    private String batchUpdateQuery;
    private String insertOrUpdateQuery;

    @PostConstruct
    private void init() {
        this.batchUpdateQuery = (updateByLatestTs ? BATCH_UPDATE_BY_LATEST_TS : BATCH_UPDATE) + RETURNING;
        this.insertOrUpdateQuery = (updateByLatestTs ? INSERT_OR_UPDATE_BY_LATEST_TS : INSERT_OR_UPDATE) + RETURNING;
    }

    @Override
    protected void setOnBatchUpdateValues(PreparedStatement ps, int i, List<TsKvLatestEntity> entities) throws SQLException {
        TsKvLatestEntity tsKvLatestEntity = entities.get(i);
        ps.setLong(1, tsKvLatestEntity.getTs());

        if (tsKvLatestEntity.getBooleanValue() != null) {
            ps.setBoolean(2, tsKvLatestEntity.getBooleanValue());
        } else {
            ps.setNull(2, Types.BOOLEAN);
        }

        ps.setString(3, replaceNullChars(tsKvLatestEntity.getStrValue()));

        if (tsKvLatestEntity.getLongValue() != null) {
            ps.setLong(4, tsKvLatestEntity.getLongValue());
        } else {
            ps.setNull(4, Types.BIGINT);
        }

        if (tsKvLatestEntity.getDoubleValue() != null) {
            ps.setDouble(5, tsKvLatestEntity.getDoubleValue());
        } else {
            ps.setNull(5, Types.DOUBLE);
        }

        ps.setString(6, replaceNullChars(tsKvLatestEntity.getJsonValue()));

        ps.setObject(7, tsKvLatestEntity.getEntityId());
        ps.setInt(8, tsKvLatestEntity.getKey());
        if (updateByLatestTs) {
            ps.setLong(9, tsKvLatestEntity.getTs());
        }
    }

    @Override
    protected void setOnInsertOrUpdateValues(PreparedStatement ps, int i, List<TsKvLatestEntity> insertEntities) throws SQLException {
        TsKvLatestEntity tsKvLatestEntity = insertEntities.get(i);
        ps.setObject(1, tsKvLatestEntity.getEntityId());
        ps.setInt(2, tsKvLatestEntity.getKey());

        ps.setLong(3, tsKvLatestEntity.getTs());
        ps.setLong(9, tsKvLatestEntity.getTs());
        if (updateByLatestTs) {
            ps.setLong(15, tsKvLatestEntity.getTs());
        }

        if (tsKvLatestEntity.getBooleanValue() != null) {
            ps.setBoolean(4, tsKvLatestEntity.getBooleanValue());
            ps.setBoolean(10, tsKvLatestEntity.getBooleanValue());
        } else {
            ps.setNull(4, Types.BOOLEAN);
            ps.setNull(10, Types.BOOLEAN);
        }

        ps.setString(5, replaceNullChars(tsKvLatestEntity.getStrValue()));
        ps.setString(11, replaceNullChars(tsKvLatestEntity.getStrValue()));

        if (tsKvLatestEntity.getLongValue() != null) {
            ps.setLong(6, tsKvLatestEntity.getLongValue());
            ps.setLong(12, tsKvLatestEntity.getLongValue());
        } else {
            ps.setNull(6, Types.BIGINT);
            ps.setNull(12, Types.BIGINT);
        }

        if (tsKvLatestEntity.getDoubleValue() != null) {
            ps.setDouble(7, tsKvLatestEntity.getDoubleValue());
            ps.setDouble(13, tsKvLatestEntity.getDoubleValue());
        } else {
            ps.setNull(7, Types.DOUBLE);
            ps.setNull(13, Types.DOUBLE);
        }

        ps.setString(8, replaceNullChars(tsKvLatestEntity.getJsonValue()));
        ps.setString(14, replaceNullChars(tsKvLatestEntity.getJsonValue()));
    }

    @Override
    protected String getBatchUpdateQuery() {
        return batchUpdateQuery;
    }

    @Override
    protected String getInsertOrUpdateQuery() {
        return insertOrUpdateQuery;
    }
}
