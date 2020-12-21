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
package org.thingsboard.server.dao.timeseries;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.nosql.CassandraAbstractAsyncDao;
import org.thingsboard.server.dao.util.NoSqlTsDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
@NoSqlTsDao
public class CassandraBaseTimeseriesDao extends CassandraAbstractAsyncDao implements TimeseriesDao {

    private static final int MIN_AGGREGATION_STEP_MS = 1000;
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID = "Generated query [{}] for entityType {} and entityId {}";
    public static final String SELECT_PREFIX = "SELECT ";
    public static final String EQUALS_PARAM = " = ? ";
    public static final String ASC_ORDER = "ASC";
    public static final String DESC_ORDER = "DESC";
    private static List<Long> FIXED_PARTITION = Arrays.asList(new Long[]{0L});

    private CassandraTsPartitionsCache cassandraTsPartitionsCache;

    @Autowired
    private Environment environment;

    @Value("${cassandra.query.ts_key_value_partitioning}")
    private String partitioning;

    @Value("${cassandra.query.ts_key_value_partitions_max_cache_size:100000}")
    private long partitionsCacheSize;

    @Value("${cassandra.query.ts_key_value_ttl}")
    private long systemTtl;

    @Value("${cassandra.query.set_null_values_enabled}")
    private boolean setNullValuesEnabled;

    private NoSqlTsPartitionDate tsFormat;

    private PreparedStatement partitionInsertStmt;
    private PreparedStatement partitionInsertTtlStmt;
    private PreparedStatement latestInsertStmt;
    private PreparedStatement[] saveStmts;
    private PreparedStatement[] saveTtlStmts;
    private PreparedStatement[] fetchStmtsAsc;
    private PreparedStatement[] fetchStmtsDesc;
    private PreparedStatement findLatestStmt;
    private PreparedStatement findAllLatestStmt;
    private PreparedStatement deleteStmt;
    private PreparedStatement deletePartitionStmt;

    private boolean isInstall() {
        return environment.acceptsProfiles("install");
    }

    @PostConstruct
    public void init() {
        super.startExecutor();
        if (!isInstall()) {
            getFetchStmt(Aggregation.NONE, DESC_ORDER);
            Optional<NoSqlTsPartitionDate> partition = NoSqlTsPartitionDate.parse(partitioning);
            if (partition.isPresent()) {
                tsFormat = partition.get();
                if (!isFixedPartitioning() && partitionsCacheSize > 0) {
                    cassandraTsPartitionsCache = new CassandraTsPartitionsCache(partitionsCacheSize);
                }
            } else {
                log.warn("Incorrect configuration of partitioning {}", partitioning);
                throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
            }
        }
    }

    @PreDestroy
    public void stop() {
        super.stopExecutor();
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> futures = queries.stream().map(query -> findAllAsync(tenantId, entityId, query)).collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<List<List<TsKvEntry>>, List<TsKvEntry>>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<List<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
        }, readResultsProcessingExecutor);
    }


    private ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            long step = Math.max(query.getInterval(), MIN_AGGREGATION_STEP_MS);
            long stepTs = query.getStartTs();
            List<ListenableFuture<Optional<TsKvEntry>>> futures = new ArrayList<>();
            while (stepTs < query.getEndTs()) {
                long startTs = stepTs;
                long endTs = stepTs + step;
                ReadTsKvQuery subQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, step, 1, query.getAggregation(), query.getOrderBy());
                futures.add(findAndAggregateAsync(tenantId, entityId, subQuery, toPartitionTs(startTs), toPartitionTs(endTs)));
                stepTs = endTs;
            }
            ListenableFuture<List<Optional<TsKvEntry>>> future = Futures.allAsList(futures);
            return Futures.transform(future, new Function<List<Optional<TsKvEntry>>, List<TsKvEntry>>() {
                @Nullable
                @Override
                public List<TsKvEntry> apply(@Nullable List<Optional<TsKvEntry>> input) {
                    return input == null ? Collections.emptyList() : input.stream().filter(v -> v.isPresent()).map(v -> v.get()).collect(Collectors.toList());
                }
            }, readResultsProcessingExecutor);
        }
    }

    public boolean isFixedPartitioning() {
        return tsFormat.getTruncateUnit().equals(ChronoUnit.FOREVER);
    }

    private ListenableFuture<List<Long>> getPartitionsFuture(TenantId tenantId, ReadTsKvQuery query, EntityId entityId, long minPartition, long maxPartition) {
        if (isFixedPartitioning()) { //no need to fetch partitions from DB
            return Futures.immediateFuture(FIXED_PARTITION);
        }
        ResultSetFuture partitionsFuture = fetchPartitions(tenantId, entityId, query.getKey(), minPartition, maxPartition);
        return Futures.transform(partitionsFuture, getPartitionsArrayFunction(), readResultsProcessingExecutor);
    }

    private ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        long minPartition = toPartitionTs(query.getStartTs());
        long maxPartition = toPartitionTs(query.getEndTs());
        final ListenableFuture<List<Long>> partitionsListFuture = getPartitionsFuture(tenantId, query, entityId, minPartition, maxPartition);
        final SimpleListenableFuture<List<TsKvEntry>> resultFuture = new SimpleListenableFuture<>();

        Futures.addCallback(partitionsListFuture, new FutureCallback<List<Long>>() {
            @Override
            public void onSuccess(@Nullable List<Long> partitions) {
                TsKvQueryCursor cursor = new TsKvQueryCursor(entityId.getEntityType().name(), entityId.getId(), query, partitions);
                findAllAsyncSequentiallyWithLimit(tenantId, cursor, resultFuture);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}][{}] Failed to fetch partitions for interval {}-{}", entityId.getEntityType().name(), entityId.getId(), toPartitionTs(query.getStartTs()), toPartitionTs(query.getEndTs()), t);
            }
        }, readResultsProcessingExecutor);

        return resultFuture;
    }

    private long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private void findAllAsyncSequentiallyWithLimit(TenantId tenantId, final TsKvQueryCursor cursor, final SimpleListenableFuture<List<TsKvEntry>> resultFuture) {
        if (cursor.isFull() || !cursor.hasNextPartition()) {
            resultFuture.set(cursor.getData());
        } else {
            PreparedStatement proto = getFetchStmt(Aggregation.NONE, cursor.getOrderBy());
            BoundStatement stmt = proto.bind();
            stmt.setString(0, cursor.getEntityType());
            stmt.setUUID(1, cursor.getEntityId());
            stmt.setString(2, cursor.getKey());
            stmt.setLong(3, cursor.getNextPartition());
            stmt.setLong(4, cursor.getStartTs());
            stmt.setLong(5, cursor.getEndTs());
            stmt.setInt(6, cursor.getCurrentLimit());

            Futures.addCallback(executeAsyncRead(tenantId, stmt), new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(@Nullable ResultSet result) {
                    cursor.addData(convertResultToTsKvEntryList(result == null ? Collections.emptyList() : result.all()));
                    findAllAsyncSequentiallyWithLimit(tenantId, cursor, resultFuture);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to fetch data for query {}-{}", stmt, t);
                }
            }, readResultsProcessingExecutor);
        }
    }

    private ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query, long minPartition, long maxPartition) {
        final Aggregation aggregation = query.getAggregation();
        final String key = query.getKey();
        final long startTs = query.getStartTs();
        final long endTs = query.getEndTs();
        final long ts = startTs + (endTs - startTs) / 2;
        ListenableFuture<List<Long>> partitionsListFuture = getPartitionsFuture(tenantId, query, entityId, minPartition, maxPartition);
        ListenableFuture<List<ResultSet>> aggregationChunks = Futures.transformAsync(partitionsListFuture,
                getFetchChunksAsyncFunction(tenantId, entityId, key, aggregation, startTs, endTs), readResultsProcessingExecutor);

        return Futures.transform(aggregationChunks, new AggregatePartitionsFunction(aggregation, key, ts), readResultsProcessingExecutor);
    }

    private Function<ResultSet, List<Long>> getPartitionsArrayFunction() {
        return rows -> rows.all().stream()
                .map(row -> row.getLong(ModelConstants.PARTITION_COLUMN)).collect(Collectors.toList());
    }

    private AsyncFunction<List<Long>, List<ResultSet>> getFetchChunksAsyncFunction(TenantId tenantId, EntityId entityId, String key, Aggregation aggregation, long startTs, long endTs) {
        return partitions -> {
            try {
                PreparedStatement proto = getFetchStmt(aggregation, DESC_ORDER);
                List<ResultSetFuture> futures = new ArrayList<>(partitions.size());
                for (Long partition : partitions) {
                    log.trace("Fetching data for partition [{}] for entityType {} and entityId {}", partition, entityId.getEntityType(), entityId.getId());
                    BoundStatement stmt = proto.bind();
                    stmt.setString(0, entityId.getEntityType().name());
                    stmt.setUUID(1, entityId.getId());
                    stmt.setString(2, key);
                    stmt.setLong(3, partition);
                    stmt.setLong(4, startTs);
                    stmt.setLong(5, endTs);
                    log.debug(GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID, stmt, entityId.getEntityType(), entityId.getId());
                    futures.add(executeAsyncRead(tenantId, stmt));
                }
                return Futures.allAsList(futures);
            } catch (Throwable e) {
                log.error("Failed to fetch data", e);
                throw e;
            }
        };
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        BoundStatement stmt = getFindLatestStmt().bind();
        stmt.setString(0, entityId.getEntityType().name());
        stmt.setUUID(1, entityId.getId());
        stmt.setString(2, key);
        log.debug(GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID, stmt, entityId.getEntityType(), entityId.getId());
        return getFuture(executeAsyncRead(tenantId, stmt), rs -> convertResultToTsKvEntry(key, rs.one()));
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        BoundStatement stmt = getFindAllLatestStmt().bind();
        stmt.setString(0, entityId.getEntityType().name());
        stmt.setUUID(1, entityId.getId());
        log.debug(GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID, stmt, entityId.getEntityType(), entityId.getId());
        return getFuture(executeAsyncRead(tenantId, stmt), rs -> convertResultToTsKvEntryList(rs.all()));
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        ttl = computeTtl(ttl);
        long partition = toPartitionTs(tsKvEntry.getTs());
        DataType type = tsKvEntry.getDataType();
        if (setNullValuesEnabled) {
            processSetNullValues(tenantId, entityId, tsKvEntry, ttl, futures, partition, type);
        }
        BoundStatement stmt = (ttl == 0 ? getSaveStmt(type) : getSaveTtlStmt(type)).bind();
        stmt.setString(0, entityId.getEntityType().name())
                .setUUID(1, entityId.getId())
                .setString(2, tsKvEntry.getKey())
                .setLong(3, partition)
                .setLong(4, tsKvEntry.getTs());
        addValue(tsKvEntry, stmt, 5);
        if (ttl > 0) {
            stmt.setInt(6, (int) ttl);
        }
        futures.add(getFuture(executeAsyncWrite(tenantId, stmt), rs -> null));
        return Futures.transform(Futures.allAsList(futures), result -> null, MoreExecutors.directExecutor());
    }

    private void processSetNullValues(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl, List<ListenableFuture<Void>> futures, long partition, DataType type) {
        switch (type) {
            case LONG:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case BOOLEAN:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case DOUBLE:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case STRING:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.JSON));
                break;
            case JSON:
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.BOOLEAN));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.DOUBLE));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.LONG));
                futures.add(saveNull(tenantId, entityId, tsKvEntry, ttl, partition, DataType.STRING));
                break;
        }
    }

    private ListenableFuture<Void> saveNull(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl, long partition, DataType type) {
        BoundStatement stmt = (ttl == 0 ? getSaveStmt(type) : getSaveTtlStmt(type)).bind();
        stmt.setString(0, entityId.getEntityType().name())
                .setUUID(1, entityId.getId())
                .setString(2, tsKvEntry.getKey())
                .setLong(3, partition)
                .setLong(4, tsKvEntry.getTs());
        stmt.setToNull(getColumnName(type));
        if (ttl > 0) {
            stmt.setInt(6, (int) ttl);
        }
        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> null);
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        if (isFixedPartitioning()) {
            return Futures.immediateFuture(null);
        }
        ttl = computeTtl(ttl);
        long partition = toPartitionTs(tsKvEntryTs);
        if (cassandraTsPartitionsCache == null) {
            return doSavePartition(tenantId, entityId, key, ttl, partition);
        } else {
            CassandraPartitionCacheKey partitionSearchKey = new CassandraPartitionCacheKey(entityId, key, partition);
            CompletableFuture<Boolean> hasInCacheFuture = cassandraTsPartitionsCache.has(partitionSearchKey);
            if (hasInCacheFuture == null) {
                return doSavePartitionWithCache(tenantId, entityId, key, partition, partitionSearchKey, ttl);
            } else {
                long finalTtl = ttl;
                SettableFuture<Void> futureResult = SettableFuture.create();
                hasInCacheFuture.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        futureResult.setException(throwable);
                    } else if (result) {
                        futureResult.set(null);
                    } else {
                        futureResult.setFuture(doSavePartitionWithCache(tenantId, entityId, key, partition, partitionSearchKey, finalTtl));
                    }
                });
                return futureResult;
            }
        }
    }

    private ListenableFuture<Void> doSavePartitionWithCache(TenantId tenantId, EntityId entityId, String key, long partition, CassandraPartitionCacheKey partitionSearchKey, long ttl) {
        return Futures.transform(doSavePartition(tenantId, entityId, key, ttl, partition), input -> {
            cassandraTsPartitionsCache.put(partitionSearchKey);
            return input;
        }, readResultsProcessingExecutor);
    }

    private ListenableFuture<Void> doSavePartition(TenantId tenantId, EntityId entityId, String key, long ttl, long partition) {
        log.debug("Saving partition {} for the entity [{}-{}] and key {}", partition, entityId.getEntityType(), entityId.getId(), key);
        BoundStatement stmt = (ttl == 0 ? getPartitionInsertStmt() : getPartitionInsertTtlStmt()).bind();
        stmt = stmt.setString(0, entityId.getEntityType().name())
                .setUUID(1, entityId.getId())
                .setLong(2, partition)
                .setString(3, key);
        if (ttl > 0) {
            stmt.setInt(4, (int) ttl);
        }
        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> null);
    }

    private long computeTtl(long ttl) {
        if (systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        BoundStatement stmt = getLatestStmt().bind()
                .setString(0, entityId.getEntityType().name())
                .setUUID(1, entityId.getId())
                .setString(2, tsKvEntry.getKey())
                .setLong(3, tsKvEntry.getTs())
                .set(4, tsKvEntry.getBooleanValue().orElse(null), Boolean.class)
                .set(5, tsKvEntry.getStrValue().orElse(null), String.class)
                .set(6, tsKvEntry.getLongValue().orElse(null), Long.class)
                .set(7, tsKvEntry.getDoubleValue().orElse(null), Double.class);
        Optional<String> jsonV = tsKvEntry.getJsonValue();
        if (jsonV.isPresent()) {
            stmt.setString(8, tsKvEntry.getJsonValue().get());
        } else {
            stmt.setToNull(8);
        }

        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> null);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long minPartition = toPartitionTs(query.getStartTs());
        long maxPartition = toPartitionTs(query.getEndTs());

        ResultSetFuture partitionsFuture = fetchPartitions(tenantId, entityId, query.getKey(), minPartition, maxPartition);

        final SimpleListenableFuture<Void> resultFuture = new SimpleListenableFuture<>();
        final ListenableFuture<List<Long>> partitionsListFuture = Futures.transform(partitionsFuture, getPartitionsArrayFunction(), readResultsProcessingExecutor);

        Futures.addCallback(partitionsListFuture, new FutureCallback<List<Long>>() {
            @Override
            public void onSuccess(@Nullable List<Long> partitions) {
                QueryCursor cursor = new QueryCursor(entityId.getEntityType().name(), entityId.getId(), query, partitions);
                deleteAsync(tenantId, cursor, resultFuture);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}][{}] Failed to fetch partitions for interval {}-{}", entityId.getEntityType().name(), entityId.getId(), minPartition, maxPartition, t);
            }
        }, readResultsProcessingExecutor);
        return resultFuture;
    }

    private void deleteAsync(TenantId tenantId, final QueryCursor cursor, final SimpleListenableFuture<Void> resultFuture) {
        if (!cursor.hasNextPartition()) {
            resultFuture.set(null);
        } else {
            PreparedStatement proto = getDeleteStmt();
            BoundStatement stmt = proto.bind();
            stmt.setString(0, cursor.getEntityType());
            stmt.setUUID(1, cursor.getEntityId());
            stmt.setString(2, cursor.getKey());
            stmt.setLong(3, cursor.getNextPartition());
            stmt.setLong(4, cursor.getStartTs());
            stmt.setLong(5, cursor.getEndTs());

            Futures.addCallback(executeAsyncWrite(tenantId, stmt), new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(@Nullable ResultSet result) {
                    deleteAsync(tenantId, cursor, resultFuture);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to delete data for query {}-{}", stmt, t);
                }
            }, readResultsProcessingExecutor);
        }
    }

    private PreparedStatement getDeleteStmt() {
        if (deleteStmt == null) {
            deleteStmt = prepare("DELETE FROM " + ModelConstants.TS_KV_CF +
                    " WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM
                    + "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM
                    + "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM
                    + "AND " + ModelConstants.PARTITION_COLUMN + EQUALS_PARAM
                    + "AND " + ModelConstants.TS_COLUMN + " > ? "
                    + "AND " + ModelConstants.TS_COLUMN + " <= ?");
        }
        return deleteStmt;
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<TsKvEntry> latestEntryFuture = findLatest(tenantId, entityId, query.getKey());

        ListenableFuture<Boolean> booleanFuture = Futures.transform(latestEntryFuture, latestEntry -> {
            long ts = latestEntry.getTs();
            if (ts > query.getStartTs() && ts <= query.getEndTs()) {
                return true;
            } else {
                log.trace("Won't be deleted latest value for [{}], key - {}", entityId, query.getKey());
            }
            return false;
        }, readResultsProcessingExecutor);

        ListenableFuture<Void> removedLatestFuture = Futures.transformAsync(booleanFuture, isRemove -> {
            if (isRemove) {
                return deleteLatest(tenantId, entityId, query.getKey());
            }
            return Futures.immediateFuture(null);
        }, readResultsProcessingExecutor);

        final SimpleListenableFuture<Void> resultFuture = new SimpleListenableFuture<>();
        Futures.addCallback(removedLatestFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                if (query.getRewriteLatestIfDeleted()) {
                    ListenableFuture<Void> savedLatestFuture = Futures.transformAsync(booleanFuture, isRemove -> {
                        if (isRemove) {
                            return getNewLatestEntryFuture(tenantId, entityId, query);
                        }
                        return Futures.immediateFuture(null);
                    }, readResultsProcessingExecutor);

                    try {
                        resultFuture.set(savedLatestFuture.get());
                    } catch (InterruptedException | ExecutionException e) {
                        log.warn("Could not get latest saved value for [{}], {}", entityId, query.getKey(), e);
                    }
                } else {
                    resultFuture.set(null);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to process remove of the latest value", entityId, t);
            }
        }, MoreExecutors.directExecutor());
        return resultFuture;
    }

    private ListenableFuture<Void> getNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        ListenableFuture<List<TsKvEntry>> future = findAllAsync(tenantId, entityId, findNewLatestQuery);

        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                return saveLatest(tenantId, entityId, entryList.get(0));
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(null);
        }, readResultsProcessingExecutor);
    }

    private ListenableFuture<Void> deleteLatest(TenantId tenantId, EntityId entityId, String key) {
        Statement delete = QueryBuilder.delete().all().from(ModelConstants.TS_KV_LATEST_CF)
                .where(eq(ModelConstants.ENTITY_TYPE_COLUMN, entityId.getEntityType()))
                .and(eq(ModelConstants.ENTITY_ID_COLUMN, entityId.getId()))
                .and(eq(ModelConstants.KEY_COLUMN, key));
        log.debug("Remove request: {}", delete.toString());
        return getFuture(executeAsyncWrite(tenantId, delete), rs -> null);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long minPartition = toPartitionTs(query.getStartTs());
        long maxPartition = toPartitionTs(query.getEndTs());
        if (minPartition == maxPartition) {
            return Futures.immediateFuture(null);
        } else {
            ResultSetFuture partitionsFuture = fetchPartitions(tenantId, entityId, query.getKey(), minPartition, maxPartition);

            final SimpleListenableFuture<Void> resultFuture = new SimpleListenableFuture<>();
            final ListenableFuture<List<Long>> partitionsListFuture = Futures.transform(partitionsFuture, getPartitionsArrayFunction(), readResultsProcessingExecutor);

            Futures.addCallback(partitionsListFuture, new FutureCallback<List<Long>>() {
                @Override
                public void onSuccess(@Nullable List<Long> partitions) {
                    int index = 0;
                    if (minPartition != query.getStartTs()) {
                        index = 1;
                    }
                    List<Long> partitionsToDelete = new ArrayList<>();
                    for (int i = index; i < partitions.size() - 1; i++) {
                        partitionsToDelete.add(partitions.get(i));
                    }
                    QueryCursor cursor = new QueryCursor(entityId.getEntityType().name(), entityId.getId(), query, partitionsToDelete);
                    deletePartitionAsync(tenantId, cursor, resultFuture);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to fetch partitions for interval {}-{}", entityId.getEntityType().name(), entityId.getId(), minPartition, maxPartition, t);
                }
            }, readResultsProcessingExecutor);
            return resultFuture;
        }
    }

    private void deletePartitionAsync(TenantId tenantId, final QueryCursor cursor, final SimpleListenableFuture<Void> resultFuture) {
        if (!cursor.hasNextPartition()) {
            resultFuture.set(null);
        } else {
            PreparedStatement proto = getDeletePartitionStmt();
            BoundStatement stmt = proto.bind();
            stmt.setString(0, cursor.getEntityType());
            stmt.setUUID(1, cursor.getEntityId());
            stmt.setLong(2, cursor.getNextPartition());
            stmt.setString(3, cursor.getKey());

            Futures.addCallback(executeAsyncWrite(tenantId, stmt), new FutureCallback<ResultSet>() {
                @Override
                public void onSuccess(@Nullable ResultSet result) {
                    deletePartitionAsync(tenantId, cursor, resultFuture);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to delete data for query {}-{}", stmt, t);
                }
            }, readResultsProcessingExecutor);
        }
    }

    private PreparedStatement getDeletePartitionStmt() {
        if (deletePartitionStmt == null) {
            deletePartitionStmt = prepare("DELETE FROM " + ModelConstants.TS_KV_PARTITIONS_CF +
                    " WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM
                    + "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM
                    + "AND " + ModelConstants.PARTITION_COLUMN + EQUALS_PARAM
                    + "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM);
        }
        return deletePartitionStmt;
    }

    private List<TsKvEntry> convertResultToTsKvEntryList(List<Row> rows) {
        List<TsKvEntry> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.forEach(row -> entries.add(convertResultToTsKvEntry(row)));
        }
        return entries;
    }

    private TsKvEntry convertResultToTsKvEntry(String key, Row row) {
        if (row != null) {
            long ts = row.getLong(ModelConstants.TS_COLUMN);
            return new BasicTsKvEntry(ts, toKvEntry(row, key));
        } else {
            return new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
    }

    private TsKvEntry convertResultToTsKvEntry(Row row) {
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
                        String jsonV = row.get(ModelConstants.JSON_VALUE_COLUMN, String.class);
                        if (StringUtils.isNoneEmpty(jsonV)) {
                            kvEntry = new JsonDataEntry(key, jsonV);
                        } else {
                            log.warn("All values in key-value row are nullable ");
                        }
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
    private ResultSetFuture fetchPartitions(TenantId tenantId, EntityId entityId, String key, long minPartition, long maxPartition) {
        Select.Where select = QueryBuilder.select(ModelConstants.PARTITION_COLUMN).from(ModelConstants.TS_KV_PARTITIONS_CF).where(eq(ModelConstants.ENTITY_TYPE_COLUMN, entityId.getEntityType().name()))
                .and(eq(ModelConstants.ENTITY_ID_COLUMN, entityId.getId())).and(eq(ModelConstants.KEY_COLUMN, key));
        select.and(QueryBuilder.gte(ModelConstants.PARTITION_COLUMN, minPartition));
        select.and(QueryBuilder.lte(ModelConstants.PARTITION_COLUMN, maxPartition));
        return executeAsyncRead(tenantId, select);
    }

    private PreparedStatement getSaveStmt(DataType dataType) {
        if (saveStmts == null) {
            saveStmts = new PreparedStatement[DataType.values().length];
            for (DataType type : DataType.values()) {
                saveStmts[type.ordinal()] = prepare(INSERT_INTO + ModelConstants.TS_KV_CF +
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

    private PreparedStatement getSaveTtlStmt(DataType dataType) {
        if (saveTtlStmts == null) {
            saveTtlStmts = new PreparedStatement[DataType.values().length];
            for (DataType type : DataType.values()) {
                saveTtlStmts[type.ordinal()] = prepare(INSERT_INTO + ModelConstants.TS_KV_CF +
                        "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                        "," + ModelConstants.ENTITY_ID_COLUMN +
                        "," + ModelConstants.KEY_COLUMN +
                        "," + ModelConstants.PARTITION_COLUMN +
                        "," + ModelConstants.TS_COLUMN +
                        "," + getColumnName(type) + ")" +
                        " VALUES(?, ?, ?, ?, ?, ?) USING TTL ?");
            }
        }
        return saveTtlStmts[dataType.ordinal()];
    }

    private PreparedStatement getFetchStmt(Aggregation aggType, String orderBy) {
        switch (orderBy) {
            case ASC_ORDER:
                if (fetchStmtsAsc == null) {
                    fetchStmtsAsc = initFetchStmt(orderBy);
                }
                return fetchStmtsAsc[aggType.ordinal()];
            case DESC_ORDER:
                if (fetchStmtsDesc == null) {
                    fetchStmtsDesc = initFetchStmt(orderBy);
                }
                return fetchStmtsDesc[aggType.ordinal()];
            default:
                throw new RuntimeException("Not supported" + orderBy + "order!");
        }
    }

    private PreparedStatement[] initFetchStmt(String orderBy) {
        PreparedStatement[] fetchStmts = new PreparedStatement[Aggregation.values().length];
        for (Aggregation type : Aggregation.values()) {
            if (type == Aggregation.SUM && fetchStmts[Aggregation.AVG.ordinal()] != null) {
                fetchStmts[type.ordinal()] = fetchStmts[Aggregation.AVG.ordinal()];
            } else if (type == Aggregation.AVG && fetchStmts[Aggregation.SUM.ordinal()] != null) {
                fetchStmts[type.ordinal()] = fetchStmts[Aggregation.SUM.ordinal()];
            } else {
                fetchStmts[type.ordinal()] = prepare(SELECT_PREFIX +
                        String.join(", ", ModelConstants.getFetchColumnNames(type)) + " FROM " + ModelConstants.TS_KV_CF
                        + " WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.PARTITION_COLUMN + EQUALS_PARAM
                        + "AND " + ModelConstants.TS_COLUMN + " > ? "
                        + "AND " + ModelConstants.TS_COLUMN + " <= ?"
                        + (type == Aggregation.NONE ? " ORDER BY " + ModelConstants.TS_COLUMN + " " + orderBy + " LIMIT ?" : ""));
            }
        }
        return fetchStmts;
    }

    private PreparedStatement getLatestStmt() {
        if (latestInsertStmt == null) {
            latestInsertStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_LATEST_CF +
                    "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                    "," + ModelConstants.ENTITY_ID_COLUMN +
                    "," + ModelConstants.KEY_COLUMN +
                    "," + ModelConstants.TS_COLUMN +
                    "," + ModelConstants.BOOLEAN_VALUE_COLUMN +
                    "," + ModelConstants.STRING_VALUE_COLUMN +
                    "," + ModelConstants.LONG_VALUE_COLUMN +
                    "," + ModelConstants.DOUBLE_VALUE_COLUMN +
                    "," + ModelConstants.JSON_VALUE_COLUMN + ")" +
                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
        }
        return latestInsertStmt;
    }


    private PreparedStatement getPartitionInsertStmt() {
        if (partitionInsertStmt == null) {
            partitionInsertStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_PARTITIONS_CF +
                    "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                    "," + ModelConstants.ENTITY_ID_COLUMN +
                    "," + ModelConstants.PARTITION_COLUMN +
                    "," + ModelConstants.KEY_COLUMN + ")" +
                    " VALUES(?, ?, ?, ?)");
        }
        return partitionInsertStmt;
    }

    private PreparedStatement getPartitionInsertTtlStmt() {
        if (partitionInsertTtlStmt == null) {
            partitionInsertTtlStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_PARTITIONS_CF +
                    "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                    "," + ModelConstants.ENTITY_ID_COLUMN +
                    "," + ModelConstants.PARTITION_COLUMN +
                    "," + ModelConstants.KEY_COLUMN + ")" +
                    " VALUES(?, ?, ?, ?) USING TTL ?");
        }
        return partitionInsertTtlStmt;
    }


    private PreparedStatement getFindLatestStmt() {
        if (findLatestStmt == null) {
            findLatestStmt = prepare(SELECT_PREFIX +
                    ModelConstants.KEY_COLUMN + "," +
                    ModelConstants.TS_COLUMN + "," +
                    ModelConstants.STRING_VALUE_COLUMN + "," +
                    ModelConstants.BOOLEAN_VALUE_COLUMN + "," +
                    ModelConstants.LONG_VALUE_COLUMN + "," +
                    ModelConstants.DOUBLE_VALUE_COLUMN + "," +
                    ModelConstants.JSON_VALUE_COLUMN + " " +
                    "FROM " + ModelConstants.TS_KV_LATEST_CF + " " +
                    "WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM +
                    "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM +
                    "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM);
        }
        return findLatestStmt;
    }

    private PreparedStatement getFindAllLatestStmt() {
        if (findAllLatestStmt == null) {
            findAllLatestStmt = prepare(SELECT_PREFIX +
                    ModelConstants.KEY_COLUMN + "," +
                    ModelConstants.TS_COLUMN + "," +
                    ModelConstants.STRING_VALUE_COLUMN + "," +
                    ModelConstants.BOOLEAN_VALUE_COLUMN + "," +
                    ModelConstants.LONG_VALUE_COLUMN + "," +
                    ModelConstants.DOUBLE_VALUE_COLUMN + "," +
                    ModelConstants.JSON_VALUE_COLUMN + " " +
                    "FROM " + ModelConstants.TS_KV_LATEST_CF + " " +
                    "WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM +
                    "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM);
        }
        return findAllLatestStmt;
    }

    private static String getColumnName(DataType type) {
        switch (type) {
            case BOOLEAN:
                return ModelConstants.BOOLEAN_VALUE_COLUMN;
            case STRING:
                return ModelConstants.STRING_VALUE_COLUMN;
            case LONG:
                return ModelConstants.LONG_VALUE_COLUMN;
            case DOUBLE:
                return ModelConstants.DOUBLE_VALUE_COLUMN;
            case JSON:
                return ModelConstants.JSON_VALUE_COLUMN;
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private static void addValue(KvEntry kvEntry, BoundStatement stmt, int column) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                Optional<Boolean> booleanValue = kvEntry.getBooleanValue();
                booleanValue.ifPresent(b -> stmt.setBool(column, b));
                break;
            case STRING:
                Optional<String> stringValue = kvEntry.getStrValue();
                stringValue.ifPresent(s -> stmt.setString(column, s));
                break;
            case LONG:
                Optional<Long> longValue = kvEntry.getLongValue();
                longValue.ifPresent(l -> stmt.setLong(column, l));
                break;
            case DOUBLE:
                Optional<Double> doubleValue = kvEntry.getDoubleValue();
                doubleValue.ifPresent(d -> stmt.setDouble(column, d));
                break;
            case JSON:
                Optional<String> jsonValue = kvEntry.getJsonValue();
                jsonValue.ifPresent(jsonObject -> stmt.setString(column, jsonObject));
                break;
        }
    }

}
