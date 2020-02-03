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
package org.thingsboard.server.dao.sqlts.psql;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.psql.TsKvEntity;
import org.thingsboard.server.dao.sqlts.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.EntityContainer;
import org.thingsboard.server.dao.sqlts.InsertTsRepository;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SqlTsDao
@PsqlDao
@Repository
@Transactional
public class PsqlTimeseriesInsertRepository extends AbstractInsertRepository implements InsertTsRepository<TsKvEntity> {

    private static final String INSERT_INTO_TS_KV = "INSERT INTO ts_kv_";

    private static final String VALUES_ON_CONFLICT_DO_UPDATE = " (entity_id, key, ts, bool_v, str_v, long_v, dbl_v) VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (entity_id, key, ts) DO UPDATE SET bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?;";

    @Override
    public void saveOrUpdate(List<EntityContainer<TsKvEntity>> entities) {
        Map<String, List<TsKvEntity>> partitionMap = new HashMap<>();
        for (EntityContainer<TsKvEntity> entityContainer : entities) {
            List<TsKvEntity> tsKvEntities = partitionMap.computeIfAbsent(entityContainer.getPartitionDate(), k -> new ArrayList<>());
            tsKvEntities.add(entityContainer.getEntity());
        }
        partitionMap.forEach((partition, entries) -> jdbcTemplate.batchUpdate(getInsertOrUpdateQuery(partition), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TsKvEntity tsKvEntity = entries.get(i);
                ps.setObject(1, tsKvEntity.getEntityId());
                ps.setInt(2, tsKvEntity.getKey());
                ps.setLong(3, tsKvEntity.getTs());

                if (tsKvEntity.getBooleanValue() != null) {
                    ps.setBoolean(4, tsKvEntity.getBooleanValue());
                    ps.setBoolean(8, tsKvEntity.getBooleanValue());
                } else {
                    ps.setNull(4, Types.BOOLEAN);
                    ps.setNull(8, Types.BOOLEAN);
                }

                ps.setString(5, replaceNullChars(tsKvEntity.getStrValue()));
                ps.setString(9, replaceNullChars(tsKvEntity.getStrValue()));


                if (tsKvEntity.getLongValue() != null) {
                    ps.setLong(6, tsKvEntity.getLongValue());
                    ps.setLong(10, tsKvEntity.getLongValue());
                } else {
                    ps.setNull(6, Types.BIGINT);
                    ps.setNull(10, Types.BIGINT);
                }

                if (tsKvEntity.getDoubleValue() != null) {
                    ps.setDouble(7, tsKvEntity.getDoubleValue());
                    ps.setDouble(11, tsKvEntity.getDoubleValue());
                } else {
                    ps.setNull(7, Types.DOUBLE);
                    ps.setNull(11, Types.DOUBLE);
                }
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        }));
    }

    private String getInsertOrUpdateQuery(String partitionDate) {
        return INSERT_INTO_TS_KV + partitionDate + VALUES_ON_CONFLICT_DO_UPDATE;
    }
}