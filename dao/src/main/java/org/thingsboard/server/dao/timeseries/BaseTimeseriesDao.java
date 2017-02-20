/**
 * Copyright © 2016-2017 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.dao.AbstractDao;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    @Value("${cassandra.query.read_result_processing_threads}")
    private int readResultsProcessingThreads;

    @Value("${cassandra.query.min_read_step}")
    private int minReadStep;

    @Value("${cassandra.query.ts_key_value_partitioning}")
    private String partitioning;

    private TsPartitionDate tsFormat;

    private ExecutorService readResultsProcessingExecutor;

    private PreparedStatement partitionInsertStmt;
    private PreparedStatement[] latestInsertStmts;
    private PreparedStatement[] saveStmts;
    private PreparedStatement[] fetchStmts;
    private PreparedStatement findLatestStmt;
    private PreparedStatement findAllLatestStmt;

    @PostConstruct
    public void init() {
        getFetchStmt(Aggregation.NONE);
        readResultsProcessingExecutor = Executors.newFixedThreadPool(readResultsProcessingThreads);
        Optional<TsPartitionDate> partition = TsPartitionDate.parse(partitioning);
        if (partition.isPresent()) {
            tsFormat = partition.get();
        } else {
            log.warn("Incorrect configuration of partitioning {}", partitioning);
            throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
        }
    }

    @PreDestroy
    public void stop() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    @Override
    public long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }


    private static String[] getFetchColumnNames(Aggregation aggregation) {
        switch (aggregation) {
            case NONE:
                return ModelConstants.NONE_AGGREGATION_COLUMNS;
            case MIN:
                return ModelConstants.MIN_AGGREGATION_COLUMNS;
            case MAX:
                return ModelConstants.MAX_AGGREGATION_COLUMNS;
            case SUM:
                return ModelConstants.SUM_AGGREGATION_COLUMNS;
            case COUNT:
                return ModelConstants.COUNT_AGGREGATION_COLUMNS;
            case AVG:
                return ModelConstants.AVG_AGGREGATION_COLUMNS;
            default:
                throw new RuntimeException("Aggregation type: " + aggregation + " is not supported!");
        }
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(String entityType, UUID entityId, TsKvQuery query, long minPartition, long maxPartition) {
        if (query.getAggregation() == Aggregation.NONE) {
            //TODO:
            return null;
        } else {
            long step = Math.max((query.getEndTs() - query.getStartTs()) / query.getLimit(), minReadStep);
            long stepTs = query.getStartTs();
            List<ListenableFuture<Optional<TsKvEntry>>> futures = new ArrayList<>();
            while (stepTs < query.getEndTs()) {
                long startTs = stepTs;
                long endTs = stepTs + step;
                TsKvQuery subQuery = new BaseTsKvQuery(query.getKey(), startTs, endTs, 1, query.getAggregation());
                futures.add(findAndAggregateAsync(entityType, entityId, subQuery, toPartitionTs(startTs), toPartitionTs(endTs)));
                stepTs = endTs;
            }
            ListenableFuture<List<Optional<TsKvEntry>>> future = Futures.allAsList(futures);
            return Futures.transform(future, new Function<List<Optional<TsKvEntry>>, List<TsKvEntry>>() {
                @Nullable
                @Override
                public List<TsKvEntry> apply(@Nullable List<Optional<TsKvEntry>> input) {
                    return input.stream().filter(v -> v.isPresent()).map(v -> v.get()).collect(Collectors.toList());
                }
            });
        }
    }

    private ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(String entityType, UUID entityId, TsKvQuery query, long minPartition, long maxPartition) {
        final Aggregation aggregation = query.getAggregation();
        final long startTs = query.getStartTs();
        final long endTs = query.getEndTs();
        final long ts = startTs + (endTs - startTs) / 2;

        ResultSetFuture partitionsFuture = fetchPartitions(entityType, entityId, query.getKey(), minPartition, maxPartition);
        com.google.common.base.Function<ResultSet, List<Long>> toArrayFunction = rows -> rows.all().stream()
                .map(row -> row.getLong(ModelConstants.PARTITION_COLUMN)).collect(Collectors.toList());

        ListenableFuture<List<Long>> partitionsListFuture = Futures.transform(partitionsFuture, toArrayFunction, readResultsProcessingExecutor);

        AsyncFunction<List<Long>, List<ResultSet>> fetchChunksFunction = partitions -> {
            try {
                PreparedStatement proto = getFetchStmt(aggregation);
                List<ResultSetFuture> futures = new ArrayList<>(partitions.size());
                for (Long partition : partitions) {
                    BoundStatement stmt = proto.bind();
                    stmt.setString(0, entityType);
                    stmt.setUUID(1, entityId);
                    stmt.setString(2, query.getKey());
                    stmt.setLong(3, partition);
                    stmt.setLong(4, startTs);
                    stmt.setLong(5, endTs);
                    log.debug("Generated query [{}] for entityType {} and entityId {}", stmt, entityType, entityId);
                    futures.add(executeAsyncRead(stmt));
                }
                return Futures.allAsList(futures);
            } catch (Throwable e) {
                log.error("Failed to fetch data", e);
                throw e;
            }
        };

        ListenableFuture<List<ResultSet>> aggregationChunks = Futures.transform(partitionsListFuture, fetchChunksFunction, readResultsProcessingExecutor);

        return Futures.transform(aggregationChunks, new AggregatePartitionsFunction(aggregation, query.getKey(), ts), readResultsProcessingExecutor);
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
            rows.forEach(row -> {
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
    private ResultSetFuture fetchPartitions(String entityType, UUID entityId, String key, long minPartition, long maxPartition) {
        Select.Where select = QueryBuilder.select(ModelConstants.PARTITION_COLUMN).from(ModelConstants.TS_KV_PARTITIONS_CF).where(eq(ModelConstants.ENTITY_TYPE_COLUMN, entityType))
                .and(eq(ModelConstants.ENTITY_ID_COLUMN, entityId)).and(eq(ModelConstants.KEY_COLUMN, key));
        select.and(QueryBuilder.gte(ModelConstants.PARTITION_COLUMN, minPartition));
        select.and(QueryBuilder.lte(ModelConstants.PARTITION_COLUMN, maxPartition));
        return executeAsyncRead(select);
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

    private PreparedStatement getFetchStmt(Aggregation aggType) {
        if (fetchStmts == null) {
            fetchStmts = new PreparedStatement[Aggregation.values().length];
            for (Aggregation type : Aggregation.values()) {
                fetchStmts[type.ordinal()] = getSession().prepare("SELECT " +
                        String.join(", ", getFetchColumnNames(type)) + " FROM " + ModelConstants.TS_KV_CF
                        + " WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + " = ? "
                        + "AND " + ModelConstants.ENTITY_ID_COLUMN + " = ? "
                        + "AND " + ModelConstants.KEY_COLUMN + " = ? "
                        + "AND " + ModelConstants.PARTITION_COLUMN + " = ? "
                        + "AND " + ModelConstants.TS_COLUMN + " > ? "
                        + "AND " + ModelConstants.TS_COLUMN + " <= ?");
            }
        }
        return fetchStmts[aggType.ordinal()];
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
