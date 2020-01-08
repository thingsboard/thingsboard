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
            "INSERT INTO tenant_ts_kv (tenant_id, entity_id, key, ts, bool_v, str_v, long_v, dbl_v) VALUES(?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (tenant_id, entity_id, key, ts) DO UPDATE SET bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?;";

    @Override
    public void saveOrUpdate(TimescaleTsKvEntity entity) {
        processSaveOrUpdate(entity, INSERT_OR_UPDATE_BOOL_STATEMENT, INSERT_OR_UPDATE_STR_STATEMENT, INSERT_OR_UPDATE_LONG_STATEMENT, INSERT_OR_UPDATE_DBL_STATEMENT);
    }

    @Override
    public void saveOrUpdate(List<TimescaleTsKvEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TimescaleTsKvEntity tsKvEntity = entities.get(i);
                ps.setString(1, tsKvEntity.getTenantId());
                ps.setString(2, tsKvEntity.getEntityId());
                ps.setString(3, tsKvEntity.getKey());
                ps.setLong(4, tsKvEntity.getTs());

                if (tsKvEntity.getBooleanValue() != null) {
                    ps.setBoolean(5, tsKvEntity.getBooleanValue());
                    ps.setBoolean(9, tsKvEntity.getBooleanValue());
                } else {
                    ps.setNull(5, Types.BOOLEAN);
                    ps.setNull(9, Types.BOOLEAN);
                }

                ps.setString(6, replaceNullChars(tsKvEntity.getStrValue()));
                ps.setString(10, replaceNullChars(tsKvEntity.getStrValue()));


                if (tsKvEntity.getLongValue() != null) {
                    ps.setLong(7, tsKvEntity.getLongValue());
                    ps.setLong(11, tsKvEntity.getLongValue());
                } else {
                    ps.setNull(7, Types.BIGINT);
                    ps.setNull(11, Types.BIGINT);
                }

                if (tsKvEntity.getDoubleValue() != null) {
                    ps.setDouble(8, tsKvEntity.getDoubleValue());
                    ps.setDouble(12, tsKvEntity.getDoubleValue());
                } else {
                    ps.setNull(8, Types.DOUBLE);
                    ps.setNull(12, Types.DOUBLE);
                }
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