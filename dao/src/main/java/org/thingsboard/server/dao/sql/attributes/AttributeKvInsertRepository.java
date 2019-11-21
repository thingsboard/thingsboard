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
package org.thingsboard.server.dao.sql.attributes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SqlDao
@Repository
@Slf4j
public abstract class AttributeKvInsertRepository {

    private static final String BATCH_UPDATE = "UPDATE attribute_kv SET str_v = ?, long_v = ?::bigint, dbl_v = ?::double precision, bool_v = ?::boolean, last_update_ts = ?" +
            " WHERE entity_type = ? and entity_id = ? and attribute_type =? and attribute_key = ?;";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, last_update_ts) " +
                    "VALUES(:entity_type, :entity_id, :attribute_type, :attribute_key, :str_v, :long_v, :dbl_v, :bool_v, :last_update_ts)" +
                    "ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) " +
                    "DO UPDATE SET str_v = :str_v, long_v = :long_v, dbl_v = :dbl_v, bool_v = :bool_v, last_update_ts = :last_update_ts;";

    protected static final String BOOL_V = "bool_v";
    protected static final String STR_V = "str_v";
    protected static final String LONG_V = "long_v";
    protected static final String DBL_V = "dbl_v";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @PersistenceContext
    protected EntityManager entityManager;

    private final ScheduledExecutorService schedulerLogExecutor = Executors.newSingleThreadScheduledExecutor();

    private final AtomicInteger count = new AtomicInteger(0);

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

    @PostConstruct
    private void init() {
        ScheduledFuture<?> scheduledLogFuture = schedulerLogExecutor.scheduleAtFixedRate(() -> {
            try {
                log.info("Saved [{}] attributes", count.get());
            } catch (Exception ignored) {
            }
        }, 0, 1, TimeUnit.SECONDS);
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
        count.incrementAndGet();
    }

    @Modifying
    private void saveOrUpdateString(AttributeKvEntity entity, String query) {
        entityManager.createNativeQuery(query)
                .setParameter("entity_type", entity.getId().getEntityType().name())
                .setParameter("entity_id", entity.getId().getEntityId())
                .setParameter("attribute_type", entity.getId().getAttributeType())
                .setParameter("attribute_key", entity.getId().getAttributeKey())
                .setParameter("str_v", entity.getStrValue())
                .setParameter("last_update_ts", entity.getLastUpdateTs())
                .executeUpdate();
        count.incrementAndGet();
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
        count.incrementAndGet();
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
        count.incrementAndGet();
    }

    @Modifying
    protected void saveOrUpdate(List<AttributeKvEntityFutureWrapper> entities) {
        int[] result = jdbcTemplate.batchUpdate(BATCH_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, entities.get(i).getEntity().getStrValue());

                if (entities.get(i).getEntity().getLongValue() != null) {
                    ps.setLong(2, entities.get(i).getEntity().getLongValue());
                } else {
                    ps.setString(2, null);
                }

                if (entities.get(i).getEntity().getDoubleValue() != null) {
                    ps.setDouble(3, entities.get(i).getEntity().getDoubleValue());
                } else {
                    ps.setString(3, null);
                }

                if (entities.get(i).getEntity().getBooleanValue() != null) {
                    ps.setBoolean(4, entities.get(i).getEntity().getBooleanValue());
                } else {
                    ps.setString(4, null);
                }

                ps.setLong(5, entities.get(i).getEntity().getLastUpdateTs());
                ps.setString(6, entities.get(i).getEntity().getId().getEntityType().name());
                ps.setString(7, entities.get(i).getEntity().getId().getEntityId());
                ps.setString(8, entities.get(i).getEntity().getId().getAttributeType());
                ps.setString(9, entities.get(i).getEntity().getId().getAttributeKey());
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });

        for (int i = 0; i < result.length; i++) {
            if (result[i] == 0)
                save(entities.get(i).getEntity());
        }

        entities.forEach(entityFutureWrapper -> entityFutureWrapper.getFuture().set(null));
        count.addAndGet(entities.size());
    }

    private void save(AttributeKvEntity entity) {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param
                .addValue("entity_type", entity.getId().getEntityType().name())
                .addValue("entity_id", entity.getId().getEntityId())
                .addValue("attribute_type", entity.getId().getAttributeType())
                .addValue("attribute_key", entity.getId().getAttributeKey())
                .addValue("str_v", entity.getStrValue())
                .addValue("long_v", entity.getLongValue())
                .addValue("dbl_v", entity.getDoubleValue())
                .addValue("bool_v", entity.getBooleanValue())
                .addValue("last_update_ts", entity.getLastUpdateTs());
        namedParameterJdbcTemplate.update(INSERT_OR_UPDATE, param);
    }
}