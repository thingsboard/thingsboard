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
package org.thingsboard.server.dao.sqlts.insert.hsql;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.insert.InsertTsRepository;
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
public class HsqlInsertTsRepository extends AbstractInsertRepository implements InsertTsRepository<TsKvEntity> {

    private static final String INSERT_OR_UPDATE =
            "MERGE INTO ts_kv USING(VALUES ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "T (entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                    "ON (ts_kv.entity_id=T.entity_id " +
                    "AND ts_kv.key=T.key " +
                    "AND ts_kv.ts=T.ts) " +
                    "WHEN MATCHED THEN UPDATE SET ts_kv.bool_v = T.bool_v, ts_kv.str_v = T.str_v, ts_kv.long_v = T.long_v, ts_kv.dbl_v = T.dbl_v ,ts_kv.json_v = T.json_v " +
                    "WHEN NOT MATCHED THEN INSERT (entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                    "VALUES (T.entity_id, T.key, T.ts, T.bool_v, T.str_v, T.long_v, T.dbl_v, T.json_v);";

    @Override
    public void saveOrUpdate(List<TsKvEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TsKvEntity tsKvEntity = entities.get(i);
                ps.setObject(1, tsKvEntity.getEntityId());
                ps.setInt(2, tsKvEntity.getKey());
                ps.setLong(3, tsKvEntity.getTs());

                if (tsKvEntity.getBooleanValue() != null) {
                    ps.setBoolean(4, tsKvEntity.getBooleanValue());
                } else {
                    ps.setNull(4, Types.BOOLEAN);
                }

                ps.setString(5, tsKvEntity.getStrValue());

                if (tsKvEntity.getLongValue() != null) {
                    ps.setLong(6, tsKvEntity.getLongValue());
                } else {
                    ps.setNull(6, Types.BIGINT);
                }

                if (tsKvEntity.getDoubleValue() != null) {
                    ps.setDouble(7, tsKvEntity.getDoubleValue());
                } else {
                    ps.setNull(7, Types.DOUBLE);
                }

                ps.setString(8, tsKvEntity.getJsonValue());
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }
}
