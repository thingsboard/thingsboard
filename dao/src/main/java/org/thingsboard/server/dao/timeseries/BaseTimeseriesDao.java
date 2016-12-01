/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.dao.timeseries;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.dao.AbstractDao;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
public class BaseTimeseriesDao extends AbstractDao implements TimeseriesDao {

    @Value("${cassandra.query.max_limit_per_request}")
    protected Integer maxLimitPerRequest;

    private PreparedStatement partitionInsertStmt;
    private PreparedStatement[] latestInsertStmts;
    private PreparedStatement[] saveStmts;
    private PreparedStatement findLatestStmt;
    private PreparedStatement findAllLatestStmt;

    @Override
    public List<TsKvEntry> find(String entityType, UUID entityId, TsKvQuery query, Optional<Long> minPartition, Optional<Long> maxPartition) {
        List<Row> rows = Collections.emptyList();
        Long[] parts = fetchPartitions(entityType, entityId, query.getKey(), minPartition, maxPartition);
        int partsLength = parts.length;
        if (parts != null && partsLength > 0) {
            int limit = maxLimitPerRequest;
            Optional<Integer> lim = query.getLimit();
            if (lim.isPresent() && lim.get() < maxLimitPerRequest) {
                limit = lim.get();
            }

            rows = new ArrayList<>(limit);
            int lastIdx = partsLength - 1;
            for (int i = 0; i < partsLength; i++) {
                int currentLimit;
                if (rows.size() >= limit) {
                    break;
                } else {
                    currentLimit = limit - rows.size();
                }
                Long partition = parts[i];
                Select.Where where = select().from(ModelConstants.TS_KV_CF).where(eq(ModelConstants.ENTITY_TYPE_COLUMN, entityType))
                        .and(eq(ModelConstants.ENTITY_ID_COLUMN, entityId))
                        .and(eq(ModelConstants.KEY_COLUMN, query.getKey()))
                        .and(eq(ModelConstants.PARTITION_COLUMN, partition));
                if (i == 0 && query.getStartTs().isPresent()) {
                    where.and(QueryBuilder.gt(ModelConstants.TS_COLUMN, query.getStartTs().get()));
                } else if (i == lastIdx && query.getEndTs().isPresent()) {
                    where.and(QueryBuilder.lte(ModelConstants.TS_COLUMN, query.getEndTs().get()));
                }
                where.limit(currentLimit);
                rows.addAll(executeRead(where).all());
            }
        }
        return convertResultToTsKvEntryList(rows);
    }

    @Override
    public ResultSetFuture findLatest(String entityType, UUID entityId, String key) {
        BoundStatement stmt = getFindLatestStmt().bind();
        stmt.setString(0, entityType);
        stmt.setUUID(1, entityId);
        stmt.setString(2, key);
        log.debug("Generated query [{}] for entityType {} and entityId {}", stmt, entityType, entityId);
        return executeAsyncRead(stmt);
    }

    @Override
    public ResultSetFuture findAllLatest(String entityType, UUID entityId) {
        BoundStatement stmt = getFindAllLatestStmt().bind();
        stmt.setString(0, entityType);
        stmt.setUUID(1, entityId);
        log.debug("Generated query [{}] for entityType {} and entityId {}", stmt, entityType, entityId);
        return executeAsyncRead(stmt);
    }

    @Override
    public ResultSetFuture save(String entityType, UUID entityId, long partition, TsKvEntry tsKvEntry) {
        DataType type = tsKvEntry.getDataType();
        BoundStatement stmt = getSaveStmt(type).bind()
                .setString(0, entityType)
                .setUUID(1, entityId)
                .setString(2, tsKvEntry.getKey())
                .setLong(3, partition)
                .setLong(4, tsKvEntry.getTs());
        addValue(tsKvEntry, stmt, 5);
        return executeAsyncWrite(stmt);
    }

    @Override
    public ResultSetFuture saveLatest(String entityType, UUID entityId, TsKvEntry tsKvEntry) {
        DataType type = tsKvEntry.getDataType();
        BoundStatement stmt = getLatestStmt(type).bind()
                .setString(0, entityType)
                .setUUID(1, entityId)
                .setString(2, tsKvEntry.getKey())
                .setLong(3, tsKvEntry.getTs());
        addValue(tsKvEntry, stmt, 4);
        return executeAsyncWrite(stmt);
    }

    @Override
    public ResultSetFuture savePartition(String entityType, UUID entityId, long partition, String key) {
        log.debug("Saving partition {} for the entity [{}-{}] and key {}", partition, entityType, entityId, key);
        return executeAsyncWrite(getPartitionInsertStmt().bind()
                .setString(0, entityType)
                .setUUID(1, entityId)
                .setLong(2, partition)
                .setString(3, key));
    }

    @Override
    public List<TsKvEntry> convertResultToTsKvEntryList(List<Row> rows) {
        List<TsKvEntry> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.stream().forEach(row -> {
                TsKvEntry kvEntry = convertResultToTsKvEntry(row);
                if (kvEntry != null) {
                    entries.add(kvEntry);
                }
            });
        }
        return entries;
    }

    @Override
    public TsKvEntry convertResultToTsKvEntry(Row row) {
        String key = row.getString(ModelConstants.KEY_COLUMN);
        long ts = row.getLong(ModelConstants.TS_COLUMN);
        return new BasicTsKvEntry(ts, toKvEntry(row, key));
    }

    public static KvEntry toKvEntry(Row row, String key) {
        KvEntry kvEntry = null;
        String strV = row.get(ModelConstants.STRING_VALUE_COLUMN, String.class);
        if (strV != null) {
            kvEntry = new StringDataEntry(key, strV);
        } else {
            Long longV = row.get(ModelConstants.LONG_VALUE_COLUMN, Long.class);
            if (longV != null) {
                kvEntry = new LongDataEntry(key, longV);
            } else {
                Double doubleV = row.get(ModelConstants.DOUBLE_VALUE_COLUMN, Double.class);
                if (doubleV != null) {
                    kvEntry = new DoubleDataEntry(key, doubleV);
                } else {
                    Boolean boolV = row.get(ModelConstants.BOOLEAN_VALUE_COLUMN, Boolean.class);
                    if (boolV != null) {
                        kvEntry = new BooleanDataEntry(key, boolV);
                    } else {
                        log.warn("All values in key-value row are nullable ");
                    }
                }
            }
        }
        return kvEntry;
    }

    /**
     * Select existing partitions from the table
     * <code>{@link ModelConstants#TS_KV_PARTITIONS_CF}</code> for the given entity
     */
    private Long[] fetchPartitions(String entityType, UUID entityId, String key, Optional<Long> minPartition, Optional<Long> maxPartition) {
        Select.Where select = QueryBuilder.select(ModelConstants.PARTITION_COLUMN).from(ModelConstants.TS_KV_PARTITIONS_CF).where(eq(ModelConstants.ENTITY_TYPE_COLUMN, entityType))
                .and(eq(ModelConstants.ENTITY_ID_COLUMN, entityId)).and(eq(ModelConstants.KEY_COLUMN, key));
        minPartition.ifPresent(startTs -> select.and(QueryBuilder.gte(ModelConstants.PARTITION_COLUMN, minPartition.get())));
        maxPartition.ifPresent(endTs -> select.and(QueryBuilder.lte(ModelConstants.PARTITION_COLUMN, maxPartition.get())));
        ResultSet resultSet = executeRead(select);
        return resultSet.all().stream().map(row -> row.getLong(ModelConstants.PARTITION_COLUMN)).toArray(Long[]::new);
    }

    private PreparedStatement getSaveStmt(DataType dataType) {
        if (saveStmts == null) {
            saveStmts = new PreparedStatement[DataType.values().length];
            for (DataType type : DataType.values()) {
                saveStmts[type.ordinal()] = getSession().prepare("INSERT INTO " + ModelConstants.TS_KV_CF +
                        "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                        "," + ModelConstants.ENTITY_ID_COLUMN +
                        "," + ModelConstants.KEY_COLUMN +
                        "," + ModelConstants.PARTITION_COLUMN +
                        "," + ModelConstants.TS_COLUMN +
                        "," + getColumnName(type) + ")" +
                        " VALUES(?, ?, ?, ?, ?, ?)");
            }
        }
        return saveStmts[dataType.ordinal()];
    }

    private PreparedStatement getLatestStmt(DataType dataType) {
        if (latestInsertStmts == null) {
            latestInsertStmts = new PreparedStatement[DataType.values().length];
            for (DataType type : DataType.values()) {
                latestInsertStmts[type.ordinal()] = getSession().prepare("INSERT INTO " + ModelConstants.TS_KV_LATEST_CF +
                        "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                        "," + ModelConstants.ENTITY_ID_COLUMN +
                        "," + ModelConstants.KEY_COLUMN +
                        "," + ModelConstants.TS_COLUMN +
                        "," + getColumnName(type) + ")" +
                        " VALUES(?, ?, ?, ?, ?)");
            }
        }
        return latestInsertStmts[dataType.ordinal()];
    }


    private PreparedStatement getPartitionInsertStmt() {
        if (partitionInsertStmt == null) {
            partitionInsertStmt = getSession().prepare("INSERT INTO " + ModelConstants.TS_KV_PARTITIONS_CF +
                    "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                    "," + ModelConstants.ENTITY_ID_COLUMN +
                    "," + ModelConstants.PARTITION_COLUMN +
                    "," + ModelConstants.KEY_COLUMN + ")" +
                    " VALUES(?, ?, ?, ?)");
        }
        return partitionInsertStmt;
    }

    private PreparedStatement getFindLatestStmt() {
        if (findLatestStmt == null) {
            findLatestStmt = getSession().prepare("SELECT " +
                    ModelConstants.KEY_COLUMN + "," +
                    ModelConstants.TS_COLUMN + "," +
                    ModelConstants.STRING_VALUE_COLUMN + "," +
                    ModelConstants.BOOLEAN_VALUE_COLUMN + "," +
                    ModelConstants.LONG_VALUE_COLUMN + "," +
                    ModelConstants.DOUBLE_VALUE_COLUMN + " " +
                    "FROM " + ModelConstants.TS_KV_LATEST_CF + " " +
                    "WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + " = ? " +
                    "AND " + ModelConstants.ENTITY_ID_COLUMN + " = ? " +
                    "AND " + ModelConstants.KEY_COLUMN + " = ? ");
        }
        return findLatestStmt;
    }

    private PreparedStatement getFindAllLatestStmt() {
        if (findAllLatestStmt == null) {
            findAllLatestStmt = getSession().prepare("SELECT " +
                    ModelConstants.KEY_COLUMN + "," +
                    ModelConstants.TS_COLUMN + "," +
                    ModelConstants.STRING_VALUE_COLUMN + "," +
                    ModelConstants.BOOLEAN_VALUE_COLUMN + "," +
                    ModelConstants.LONG_VALUE_COLUMN + "," +
                    ModelConstants.DOUBLE_VALUE_COLUMN + " " +
                    "FROM " + ModelConstants.TS_KV_LATEST_CF + " " +
                    "WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + " = ? " +
                    "AND " + ModelConstants.ENTITY_ID_COLUMN + " = ? ");
        }
        return findAllLatestStmt;
    }

    public static String getColumnName(DataType type) {
        switch (type) {
            case BOOLEAN:
                return ModelConstants.BOOLEAN_VALUE_COLUMN;
            case STRING:
                return ModelConstants.STRING_VALUE_COLUMN;
            case LONG:
                return ModelConstants.LONG_VALUE_COLUMN;
            case DOUBLE:
                return ModelConstants.DOUBLE_VALUE_COLUMN;
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    public static void addValue(KvEntry kvEntry, BoundStatement stmt, int column) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                stmt.setBool(column, kvEntry.getBooleanValue().get().booleanValue());
                break;
            case STRING:
                stmt.setString(column, kvEntry.getStrValue().get());
                break;
            case LONG:
                stmt.setLong(column, kvEntry.getLongValue().get().longValue());
                break;
            case DOUBLE:
                stmt.setDouble(column, kvEntry.getDoubleValue().get().doubleValue());
                break;
        }
    }

}
