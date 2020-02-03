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
package org.thingsboard.server.dao.sql.attributes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SqlDao
@Repository
@Slf4j
public abstract class AttributeKvInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    private static final String EMPTY_STR = "";

    private static final String BATCH_UPDATE = "UPDATE attribute_kv SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, last_update_ts = ? " +
            "WHERE entity_type = ? and entity_id = ? and attribute_type =? and attribute_key = ?;";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, last_update_ts) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) " +
                    "DO UPDATE SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, last_update_ts = ?;";

    protected static final String BOOL_V = "bool_v";
    protected static final String STR_V = "str_v";
    protected static final String LONG_V = "long_v";
    protected static final String DBL_V = "dbl_v";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Value("${sql.remove_null_chars}")
    private boolean removeNullChars;

    @PersistenceContext
    protected EntityManager entityManager;

    public abstract void saveOrUpdate(AttributeKvEntity entity);

    protected void processSaveOrUpdate(AttributeKvEntity entity, String requestBoolValue, String requestStrValue, String requestLongValue, String requestDblValue) {
        if (entity.getBooleanValue() != null) {
            saveOrUpdateBoolean(entity, requestBoolValue);
        }
        if (entity.getStrValue() != null) {
            saveOrUpdateString(entity, requestStrValue);
        }
        if (entity.getLongValue() != null) {
            saveOrUpdateLong(entity, requestLongValue);
        }
        if (entity.getDoubleValue() != null) {
            saveOrUpdateDouble(entity, requestDblValue);
        }
    }

    @Modifying
    private void saveOrUpdateBoolean(AttributeKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("bool_v", entity.getBooleanValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

    @Modifying
    private void saveOrUpdateString(AttributeKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("str_v", replaceNullChars(entity.getStrValue()))
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

    @Modifying
    private void saveOrUpdateLong(AttributeKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("long_v", entity.getLongValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

    @Modifying
    private void saveOrUpdateDouble(AttributeKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("dbl_v", entity.getDoubleValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
    }

    protected void saveOrUpdate(List<AttributeKvEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                int[] result = jdbcTemplate.batchUpdate(BATCH_UPDATE, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        AttributeKvEntity kvEntity = entities.get(i);
                        ps.setString(1, replaceNullChars(kvEntity.getStrValue()));

                        if (kvEntity.getLongValue() != null) {
                            ps.setLong(2, kvEntity.getLongValue());
                        } else {
                            ps.setNull(2, Types.BIGINT);
                        }

                        if (kvEntity.getDoubleValue() != null) {
                            ps.setDouble(3, kvEntity.getDoubleValue());
                        } else {
                            ps.setNull(3, Types.DOUBLE);
                        }

                        if (kvEntity.getBooleanValue() != null) {
                            ps.setBoolean(4, kvEntity.getBooleanValue());
                        } else {
                            ps.setNull(4, Types.BOOLEAN);
                        }

                        ps.setLong(5, kvEntity.getLastUpdateTs());
                        ps.setString(6, kvEntity.getId().getEntityType().name());
                        ps.setString(7, kvEntity.getId().getEntityId());
                        ps.setString(8, kvEntity.getId().getAttributeType());
                        ps.setString(9, kvEntity.getId().getAttributeKey());
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

                List<AttributeKvEntity> insertEntities = new ArrayList<>(updatedCount);
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        insertEntities.add(entities.get(i));
                    }
                }

                jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        AttributeKvEntity kvEntity = insertEntities.get(i);
                        ps.setString(1, kvEntity.getId().getEntityType().name());
                        ps.setString(2, kvEntity.getId().getEntityId());
                        ps.setString(3, kvEntity.getId().getAttributeType());
                        ps.setString(4, kvEntity.getId().getAttributeKey());
                        ps.setString(5, replaceNullChars(kvEntity.getStrValue()));
                        ps.setString(10, replaceNullChars(kvEntity.getStrValue()));

                        if (kvEntity.getLongValue() != null) {
                            ps.setLong(6, kvEntity.getLongValue());
                            ps.setLong(11, kvEntity.getLongValue());
                        } else {
                            ps.setNull(6, Types.BIGINT);
                            ps.setNull(11, Types.BIGINT);
                        }

                        if (kvEntity.getDoubleValue() != null) {
                            ps.setDouble(7, kvEntity.getDoubleValue());
                            ps.setDouble(12, kvEntity.getDoubleValue());
                        } else {
                            ps.setNull(7, Types.DOUBLE);
                            ps.setNull(12, Types.DOUBLE);
                        }

                        if (kvEntity.getBooleanValue() != null) {
                            ps.setBoolean(8, kvEntity.getBooleanValue());
                            ps.setBoolean(13, kvEntity.getBooleanValue());
                        } else {
                            ps.setNull(8, Types.BOOLEAN);
                            ps.setNull(13, Types.BOOLEAN);
                        }

                        ps.setLong(9, kvEntity.getLastUpdateTs());
                        ps.setLong(14, kvEntity.getLastUpdateTs());
                    }

                    @Override
                    public int getBatchSize() {
                        return insertEntities.size();
                    }
                });
            }
        });
    }

    private String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }
}