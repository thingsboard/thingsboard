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
package org.thingsboard.server.dao.sqlts.insert.latest.hsql;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
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
public class HsqlLatestInsertTsRepository extends AbstractInsertRepository implements InsertLatestTsRepository {

    private static final String INSERT_OR_UPDATE =
            "MERGE INTO ts_kv_latest USING(VALUES ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "T (entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                    "ON (ts_kv_latest.entity_id=T.entity_id " +
                    "AND ts_kv_latest.key=T.key) " +
                    "WHEN MATCHED THEN UPDATE SET ts_kv_latest.ts = T.ts, ts_kv_latest.bool_v = T.bool_v, ts_kv_latest.str_v = T.str_v, ts_kv_latest.long_v = T.long_v, ts_kv_latest.dbl_v = T.dbl_v, ts_kv_latest.json_v = T.json_v " +
                    "WHEN NOT MATCHED THEN INSERT (entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                    "VALUES (T.entity_id, T.key, T.ts, T.bool_v, T.str_v, T.long_v, T.dbl_v, T.json_v);";

    @Override
    public void saveOrUpdate(List<TsKvLatestEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setObject(1, entities.get(i).getEntityId());
                ps.setInt(2, entities.get(i).getKey());
                ps.setLong(3, entities.get(i).getTs());

                if (entities.get(i).getBooleanValue() != null) {
                    ps.setBoolean(4, entities.get(i).getBooleanValue());
                } else {
                    ps.setNull(4, Types.BOOLEAN);
                }

                ps.setString(5, entities.get(i).getStrValue());

                if (entities.get(i).getLongValue() != null) {
                    ps.setLong(6, entities.get(i).getLongValue());
                } else {
                    ps.setNull(6, Types.BIGINT);
                }

                if (entities.get(i).getDoubleValue() != null) {
                    ps.setDouble(7, entities.get(i).getDoubleValue());
                } else {
                    ps.setNull(7, Types.DOUBLE);
                }

                ps.setString(8, entities.get(i).getJsonValue());
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }
}
