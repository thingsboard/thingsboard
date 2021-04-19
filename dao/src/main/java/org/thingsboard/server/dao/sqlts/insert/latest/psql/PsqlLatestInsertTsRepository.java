/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sqlts.insert.latest.psql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.util.PsqlTsLatestAnyDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@PsqlTsLatestAnyDao
@Repository
@Transactional
public class PsqlLatestInsertTsRepository extends AbstractInsertRepository implements InsertLatestTsRepository {

    @Value("${sql.ts_latest.update_by_latest_ts:true}")
    private Boolean updateByLatestTs;

    private static final String BATCH_UPDATE =
            "UPDATE ts_kv_latest SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json) WHERE entity_id = ? AND key = ?";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO ts_kv_latest (entity_id, key, ts, bool_v, str_v, long_v, dbl_v,  json_v) VALUES(?, ?, ?, ?, ?, ?, ?, cast(? AS json)) " +
                    "ON CONFLICT (entity_id, key) DO UPDATE SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json)";

    private static final String BATCH_UPDATE_BY_LATEST_TS = BATCH_UPDATE + " AND ts_kv_latest.ts <= ?";

    private static final String INSERT_OR_UPDATE_BY_LATEST_TS = INSERT_OR_UPDATE + " WHERE ts_kv_latest.ts <= ?";

    @Override
    public void saveOrUpdate(List<TsKvLatestEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                String batchUpdateQuery = updateByLatestTs ? BATCH_UPDATE_BY_LATEST_TS : BATCH_UPDATE;
                String insertOrUpdateQuery = updateByLatestTs ? INSERT_OR_UPDATE_BY_LATEST_TS : INSERT_OR_UPDATE;

                int[] result = jdbcTemplate.batchUpdate(batchUpdateQuery, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
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
                    public int getBatchSize() {
                        return entities.size();
                    }
                });

                int updatedCount = 0;
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        updatedCount++;
                    }
                }

                List<TsKvLatestEntity> insertEntities = new ArrayList<>(updatedCount);
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        insertEntities.add(entities.get(i));
                    }
                }

                jdbcTemplate.batchUpdate(insertOrUpdateQuery, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
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
                    public int getBatchSize() {
                        return insertEntities.size();
                    }
                });
            }
        });
    }
}
