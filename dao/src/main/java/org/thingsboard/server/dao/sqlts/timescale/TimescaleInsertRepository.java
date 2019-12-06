/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.sqlts.timescale;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvEntity;
import org.thingsboard.server.dao.sqlts.AbstractTimeseriesInsertRepository;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@TimescaleDBTsDao
@PsqlDao
@Repository
@Transactional
public class TimescaleInsertRepository extends AbstractTimeseriesInsertRepository<TimescaleTsKvEntity> {

    private static final String INSERT_OR_UPDATE_BOOL_STATEMENT = getInsertOrUpdateString(BOOL_V, PSQL_ON_BOOL_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_OR_UPDATE_STR_STATEMENT = getInsertOrUpdateString(STR_V, PSQL_ON_STR_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_OR_UPDATE_LONG_STATEMENT = getInsertOrUpdateString(LONG_V, PSQL_ON_LONG_VALUE_UPDATE_SET_NULLS);
    private static final String INSERT_OR_UPDATE_DBL_STATEMENT = getInsertOrUpdateString(DBL_V, PSQL_ON_DBL_VALUE_UPDATE_SET_NULLS);

    private static final String BATCH_UPDATE =
            "UPDATE tenant_ts_kv SET bool_v = ?, str_v = ?, long_v = ?, dbl_v = ? WHERE entity_type = ? AND entity_id = ? and key = ? and ts = ?";


    private static final String INSERT_OR_UPDATE =
            "INSERT INTO tenant_ts_kv (tenant_id, entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) VALUES(?, ?, ?, ?, ?, ?, ?, ?, cast(? AS json)) " +
                    "ON CONFLICT (tenant_id, entity_id, key, ts) DO UPDATE SET bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json);";

    @Override
    public void saveOrUpdate(TimescaleTsKvEntity entity) {
        processSaveOrUpdate(entity, INSERT_OR_UPDATE_BOOL_STATEMENT, INSERT_OR_UPDATE_STR_STATEMENT, INSERT_OR_UPDATE_LONG_STATEMENT, INSERT_OR_UPDATE_DBL_STATEMENT);
    }

    @Override
    public void saveOrUpdate(List<TimescaleTsKvEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, entities.get(i).getTenantId());
                ps.setString(2, entities.get(i).getEntityId());
                ps.setString(3, entities.get(i).getKey());
                ps.setLong(4, entities.get(i).getTs());

                if (entities.get(i).getBooleanValue() != null) {
                    ps.setBoolean(5, entities.get(i).getBooleanValue());
                    ps.setBoolean(10, entities.get(i).getBooleanValue());
                } else {
                    ps.setNull(5, Types.BOOLEAN);
                    ps.setNull(10, Types.BOOLEAN);
                }

                ps.setString(6, replaceNullChars(entities.get(i).getStrValue()));
                ps.setString(11, replaceNullChars(entities.get(i).getStrValue()));


                if (entities.get(i).getLongValue() != null) {
                    ps.setLong(7, entities.get(i).getLongValue());
                    ps.setLong(12, entities.get(i).getLongValue());
                } else {
                    ps.setNull(7, Types.BIGINT);
                    ps.setNull(12, Types.BIGINT);
                }

                if (entities.get(i).getDoubleValue() != null) {
                    ps.setDouble(8, entities.get(i).getDoubleValue());
                    ps.setDouble(13, entities.get(i).getDoubleValue());
                } else {
                    ps.setNull(8, Types.DOUBLE);
                    ps.setNull(13, Types.DOUBLE);
                }

                ps.setString(9, replaceNullChars(entities.get(i).getJsonValue()));
                ps.setString(14, replaceNullChars(entities.get(i).getJsonValue()));
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }

    @Override
    protected void saveOrUpdateBoolean(TimescaleTsKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("tenant_id", entity.getTenantId())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("bool_v", entity.getBooleanValue())
                .executeUpdate();
    }

    @Override
    protected void saveOrUpdateString(TimescaleTsKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("tenant_id", entity.getTenantId())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("str_v", replaceNullChars(entity.getStrValue()))
                .executeUpdate();
    }

    @Override
    protected void saveOrUpdateLong(TimescaleTsKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("tenant_id", entity.getTenantId())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("long_v", entity.getLongValue())
                .executeUpdate();
    }

    @Override
    protected void saveOrUpdateDouble(TimescaleTsKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("tenant_id", entity.getTenantId())
                .setParameter("entity_id", entity.getEntityId())
                .setParameter("key", entity.getKey())
                .setParameter("ts", entity.getTs())
                .setParameter("dbl_v", entity.getDoubleValue())
                .executeUpdate();
    }

    private static String getInsertOrUpdateString(String value, String nullValues) {
        return "INSERT INTO tenant_ts_kv(tenant_id, entity_id, key, ts, " + value + ") VALUES (:tenant_id, :entity_id, :key, :ts, :" + value + ") ON CONFLICT (tenant_id, entity_id, key, ts) DO UPDATE SET " + value + " = :" + value + ", ts = :ts," + nullValues;
    }
}