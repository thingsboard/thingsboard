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
package org.thingsboard.server.dao.sqlts.hsql;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.hsql.TsKvEntity;
import org.thingsboard.server.dao.sqlts.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.EntityContainer;
import org.thingsboard.server.dao.sqlts.InsertTsRepository;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@SqlTsDao
@HsqlDao
@Repository
@Transactional
public class HsqlTimeseriesInsertRepository extends AbstractInsertRepository implements InsertTsRepository<TsKvEntity> {

    private static final String INSERT_OR_UPDATE =
            "MERGE INTO ts_kv USING(VALUES ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "T (entity_type, entity_id, key, ts, bool_v, str_v, long_v, dbl_v) " +
                    "ON (ts_kv.entity_type=T.entity_type " +
                    "AND ts_kv.entity_id=T.entity_id " +
                    "AND ts_kv.key=T.key " +
                    "AND ts_kv.ts=T.ts) " +
                    "WHEN MATCHED THEN UPDATE SET ts_kv.bool_v = T.bool_v, ts_kv.str_v = T.str_v, ts_kv.long_v = T.long_v, ts_kv.dbl_v = T.dbl_v " +
                    "WHEN NOT MATCHED THEN INSERT (entity_type, entity_id, key, ts, bool_v, str_v, long_v, dbl_v) " +
                    "VALUES (T.entity_type, T.entity_id, T.key, T.ts, T.bool_v, T.str_v, T.long_v, T.dbl_v);";

    @Override
    public void saveOrUpdate(List<EntityContainer<TsKvEntity>> entities) {
        jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EntityContainer<TsKvEntity> tsKvEntityEntityContainer = entities.get(i);
                TsKvEntity tsKvEntity = tsKvEntityEntityContainer.getEntity();
                ps.setString(1, tsKvEntity.getEntityType().name());
                ps.setString(2, tsKvEntity.getEntityId());
                ps.setString(3, tsKvEntity.getKey());
                ps.setLong(4, tsKvEntity.getTs());

                if (tsKvEntity.getBooleanValue() != null) {
                    ps.setBoolean(5, tsKvEntity.getBooleanValue());
                } else {
                    ps.setNull(5, Types.BOOLEAN);
                }

                ps.setString(6, tsKvEntity.getStrValue());

                if (tsKvEntity.getLongValue() != null) {
                    ps.setLong(7, tsKvEntity.getLongValue());
                } else {
                    ps.setNull(7, Types.BIGINT);
                }

                if (tsKvEntity.getDoubleValue() != null) {
                    ps.setDouble(8, tsKvEntity.getDoubleValue());
                } else {
                    ps.setNull(8, Types.DOUBLE);
                }
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }
}