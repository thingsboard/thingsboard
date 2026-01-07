/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntryAggWrapper;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.nosql.TbResultSet;
import org.thingsboard.server.dao.nosql.TbResultSetFuture;
import org.thingsboard.server.dao.sqlts.AggregationTimeseriesDao;
import org.thingsboard.server.dao.util.NoSqlTsDao;
import org.thingsboard.server.dao.util.TimeUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

/**
 * @author Andrew Shvayka
 */
@SuppressWarnings("UnstableApiUsage")
@Component
@Slf4j
@NoSqlTsDao
public class CassandraBaseTimeseriesDao extends AbstractCassandraBaseTimeseriesDao implements TimeseriesDao, AggregationTimeseriesDao {

    protected static final int MIN_AGGREGATION_STEP_MS = 1000;
    public static final String ASC_ORDER = "ASC";
    public static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);
    protected static final List<Long> FIXED_PARTITION = List.of(0L);
    protected static final String INSERT_WITH_NULL = INSERT_INTO + ModelConstants.TS_KV_CF +
            "(" + ModelConstants.ENTITY_TYPE_COLUMN +
            "," + ModelConstants.ENTITY_ID_COLUMN +
            "," + ModelConstants.KEY_COLUMN +
            "," + ModelConstants.PARTITION_COLUMN +
            "," + ModelConstants.TS_COLUMN +
            "," + ModelConstants.BOOLEAN_VALUE_COLUMN +
            "," + ModelConstants.STRING_VALUE_COLUMN +
            "," + ModelConstants.LONG_VALUE_COLUMN +
            "," + ModelConstants.DOUBLE_VALUE_COLUMN +
            "," + ModelConstants.JSON_VALUE_COLUMN + ")" +
            " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private CassandraTsPartitionsCache cassandraTsPartitionsCache;

    @Autowired
    private Environment environment;

    @Getter
    @Value("${cassandra.query.ts_key_value_partitioning}")
    private String partitioning;

    @Getter
    @Value("${cassandra.query.use_ts_key_value_partitioning_on_read:true}")
    private boolean useTsKeyValuePartitioningOnRead;

    @Getter
    @Value("${cassandra.query.use_ts_key_value_partitioning_on_read_max_estimated_partition_count:40}") // 3+ years for MONTHS
    private int useTsKeyValuePartitioningOnReadMaxEstimatedPartitionCount;

    @Value("${cassandra.query.ts_key_value_partitions_max_cache_size:100000}")
    private long partitionsCacheSize;

    @Value("${cassandra.query.ts_key_value_ttl}")
    private long systemTtl;

    @Value("${cassandra.query.set_null_values_enabled}")
    private boolean setNullValuesEnabled;

    private NoSqlTsPartitionDate tsFormat;

    private PreparedStatement partitionInsertStmt;
    private PreparedStatement partitionInsertTtlStmt;
    private PreparedStatement[] saveStmts;
    private PreparedStatement[] saveTtlStmts;
    private PreparedStatement[] fetchStmtsAsc;
    private PreparedStatement[] fetchStmtsDesc;
    private PreparedStatement deleteStmt;
    private PreparedStatement saveWithNullStmt;
    private PreparedStatement saveWithNullWithTtlStmt;
    private final Lock stmtCreationLock = new ReentrantLock();

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    @PostConstruct
    public void init() {
        super.startExecutor();
        if (!isInstall()) {
            getFetchStmt(Aggregation.NONE, DESC_ORDER);
        }
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

    @PreDestroy
    public void stop() {
        super.stopExecutor();
    }

    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<ReadTsKvQueryResult>> futures = queries.stream()
                .map(query -> findAllAsync(tenantId, entityId, query)).collect(Collectors.toList());
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        ttl = computeTtl(ttl);
        int dataPointDays = tsKvEntry.getDataPoints() * Math.max(1, (int) (ttl / SECONDS_IN_DAY));
        long partition = toPartitionTs(tsKvEntry.getTs());
        String entityType = entityId.getEntityType().name();
        UUID entityIdId = entityId.getId();
        String entryKey = tsKvEntry.getKey();
        long ts = tsKvEntry.getTs();
        DataType type = tsKvEntry.getDataType();
        BoundStatementBuilder stmtBuilder;
        if (setNullValuesEnabled) {
            Boolean booleanValue = tsKvEntry.getBooleanValue().orElse(null);
            String strValue = tsKvEntry.getStrValue().orElse(null);
            Long longValue = tsKvEntry.getLongValue().orElse(null);
            Double doubleValue = tsKvEntry.getDoubleValue().orElse(null);
            String jsonValue = tsKvEntry.getJsonValue().orElse(null);
            if (ttl == 0) {
                stmtBuilder = new BoundStatementBuilder(getSaveWithNullStmt()
                        .bind(entityType, entityIdId, entryKey, partition, ts, booleanValue, strValue, longValue, doubleValue, jsonValue));
            } else {
                stmtBuilder = new BoundStatementBuilder(getSaveWithNullWithTtlStmt()
                        .bind(entityType, entityIdId, entryKey, partition, ts, booleanValue, strValue, longValue, doubleValue, jsonValue, (int) ttl));
            }
        } else {
            stmtBuilder = new BoundStatementBuilder((ttl == 0 ? getSaveStmt(type) : getSaveTtlStmt(type)).bind());
            stmtBuilder.setString(0, entityType)
                    .setUuid(1, entityIdId)
                    .setString(2, entryKey)
                    .setLong(3, partition)
                    .setLong(4, ts);
            addValue(tsKvEntry, stmtBuilder, 5);
            if (ttl > 0) {
                stmtBuilder.setInt(6, (int) ttl);
            }
        }
        BoundStatement stmt = stmtBuilder.build();
        futures.add(getFuture(executeAsyncWrite(tenantId, stmt), rs -> null));
        return Futures.transform(Futures.allAsList(futures), result -> dataPointDays, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key) {
        if (isFixedPartitioning()) {
            return Futures.immediateFuture(null);
        }
        // DO NOT apply custom TTL to partition, otherwise, short TTL will remove partition too early
        // partitions must remain in the DB forever or be removed only by systemTtl
        // removal of empty partition is too expensive (we need to scan all data keys for these partitions with ALLOW FILTERING)
        long ttl = computeTtl(0);
        long partition = toPartitionTs(tsKvEntryTs);
        if (cassandraTsPartitionsCache == null) {
            return doSavePartition(tenantId, entityId, key, ttl, partition);
        } else {
            CassandraPartitionCacheKey partitionSearchKey = new CassandraPartitionCacheKey(entityId, key, partition);
            if (!cassandraTsPartitionsCache.has(partitionSearchKey)) {
                ListenableFuture<Integer> result = doSavePartition(tenantId, entityId, key, ttl, partition);
                Futures.addCallback(result, new CacheCallback<>(partitionSearchKey), MoreExecutors.directExecutor());
                return result;
            } else {
                return Futures.immediateFuture(0);
            }
        }
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long minPartition = toPartitionTs(query.getStartTs());
        long maxPartition = toPartitionTs(query.getEndTs());

        final SimpleListenableFuture<Void> resultFuture = new SimpleListenableFuture<>();
        final ListenableFuture<List<Long>> partitionsListFuture = getPartitionsFuture(tenantId, query, entityId, minPartition, maxPartition);

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

    @Override
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        var aggParams = query.getAggParameters();
        if (Aggregation.NONE.equals(aggParams.getAggregation())) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            long startPeriod = query.getStartTs();
            long endPeriod = Math.max(query.getStartTs() + 1, query.getEndTs());
            List<ListenableFuture<Optional<TsKvEntryAggWrapper>>> futures = new ArrayList<>();
            var intervalType = aggParams.getIntervalType();
            while (startPeriod < endPeriod) {
                long startTs = startPeriod;
                long endTs;
                if (IntervalType.MILLISECONDS.equals(intervalType)) {
                    endTs = startPeriod + Math.max(query.getInterval(), MIN_AGGREGATION_STEP_MS);
                } else {
                    endTs = TimeUtils.calculateIntervalEnd(startTs, aggParams.getIntervalType(), aggParams.getTzId());
                }
                endTs = Math.min(endTs, endPeriod);
                ReadTsKvQuery subQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1, query.getAggregation(), query.getOrder());
                futures.add(findAndAggregateAsync(tenantId, entityId, subQuery, toPartitionTs(startTs), toPartitionTs(endTs)));
                startPeriod = endTs;
            }
            ListenableFuture<List<Optional<TsKvEntryAggWrapper>>> future = Futures.allAsList(futures);
            return Futures.transform(future, new Function<>() {
                @Nullable
                @Override
                public ReadTsKvQueryResult apply(@Nullable List<Optional<TsKvEntryAggWrapper>> input) {
                    if (input == null) {
                        return new ReadTsKvQueryResult(query.getId(), Collections.emptyList(), query.getStartTs());
                    } else {
                        long maxTs = query.getStartTs();
                        List<TsKvEntry> data = new ArrayList<>();
                        for (var opt : input) {
                            if (opt.isPresent()) {
                                TsKvEntryAggWrapper tsKvEntryAggWrapper = opt.get();
                                maxTs = Math.max(maxTs, tsKvEntryAggWrapper.getLastEntryTs());
                                data.add(tsKvEntryAggWrapper.getEntry());
                            }
                        }
                        return new ReadTsKvQueryResult(query.getId(), data, maxTs);
                    }

                }
            }, readResultsProcessingExecutor);
        }
    }

    @Override
    public void cleanup(long systemTtl) {
        //Cleanup by TTL is native for Cassandra
    }

    private ListenableFuture<ReadTsKvQueryResult> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        long minPartition = toPartitionTs(query.getStartTs());
        long maxPartition = toPartitionTs(query.getEndTs());
        final ListenableFuture<List<Long>> partitionsListFuture = getPartitionsFuture(tenantId, query, entityId, minPartition, maxPartition);
        final SimpleListenableFuture<List<TsKvEntry>> resultFuture = new SimpleListenableFuture<>();

        Futures.addCallback(partitionsListFuture, new FutureCallback<>() {
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

        return Futures.transform(resultFuture, tsKvEntries -> {
            long lastTs = query.getStartTs();
            if (tsKvEntries != null) {
                lastTs = tsKvEntries.stream().map(TsKvEntry::getTs).max(Long::compare).orElse(query.getStartTs());
            }
            return new ReadTsKvQueryResult(query.getId(), tsKvEntries, lastTs);
        }, MoreExecutors.directExecutor());
    }

    long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private void findAllAsyncSequentiallyWithLimit(TenantId tenantId, final TsKvQueryCursor cursor, final SimpleListenableFuture<List<TsKvEntry>> resultFuture) {
        if (cursor.isFull() || !cursor.hasNextPartition()) {
            resultFuture.set(cursor.getData());
        } else {
            PreparedStatement proto = getFetchStmt(Aggregation.NONE, cursor.getOrderBy());
            BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());

            stmtBuilder.setString(0, cursor.getEntityType());
            stmtBuilder.setUuid(1, cursor.getEntityId());
            stmtBuilder.setString(2, cursor.getKey());
            stmtBuilder.setLong(3, cursor.getNextPartition());
            stmtBuilder.setLong(4, cursor.getStartTs());
            stmtBuilder.setLong(5, cursor.getEndTs());
            stmtBuilder.setInt(6, cursor.getCurrentLimit());

            BoundStatement stmt = stmtBuilder.build();

            Futures.addCallback(executeAsyncRead(tenantId, stmt), new FutureCallback<TbResultSet>() {
                @Override
                public void onSuccess(@Nullable TbResultSet result) {
                    if (result == null) {
                        cursor.addData(convertResultToTsKvEntryList(Collections.emptyList()));
                        findAllAsyncSequentiallyWithLimit(tenantId, cursor, resultFuture);
                    } else {
                        Futures.addCallback(result.allRows(readResultsProcessingExecutor), new FutureCallback<List<Row>>() {

                            @Override
                            public void onSuccess(@Nullable List<Row> result) {
                                cursor.addData(convertResultToTsKvEntryList(result == null ? Collections.emptyList() : result));
                                findAllAsyncSequentiallyWithLimit(tenantId, cursor, resultFuture);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.error("[{}][{}] Failed to fetch data for query {}-{}", stmt, t);
                            }
                        }, readResultsProcessingExecutor);


                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to fetch data for query {}-{}", stmt, t);
                }
            }, readResultsProcessingExecutor);
        }
    }

    private ListenableFuture<Optional<TsKvEntryAggWrapper>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query, long minPartition, long maxPartition) {
        final Aggregation aggregation = query.getAggregation();
        final String key = query.getKey();
        final long startTs = query.getStartTs();
        final long endTs = query.getEndTs();
        final long ts = startTs + (endTs - startTs) / 2;
        ListenableFuture<List<Long>> partitionsListFuture = getPartitionsFuture(tenantId, query, entityId, minPartition, maxPartition);
        ListenableFuture<List<TbResultSet>> aggregationChunks = Futures.transformAsync(partitionsListFuture,
                getFetchChunksAsyncFunction(tenantId, entityId, key, aggregation, startTs, endTs), readResultsProcessingExecutor);

        return Futures.transformAsync(aggregationChunks, new AggregatePartitionsFunction(aggregation, key, ts, readResultsProcessingExecutor), readResultsProcessingExecutor);
    }

    private AsyncFunction<TbResultSet, List<Long>> getPartitionsArrayFunction() {
        return rs ->
                Futures.transform(rs.allRows(readResultsProcessingExecutor), rows ->
                                rows.stream()
                                        .map(row -> row.getLong(ModelConstants.PARTITION_COLUMN)).collect(Collectors.toList()),
                        readResultsProcessingExecutor);
    }

    ListenableFuture<List<Long>> getPartitionsFuture(TenantId tenantId, TsKvQuery query, EntityId entityId, long minPartition, long maxPartition) {
        if (isFixedPartitioning()) { //no need to fetch partitions from DB
            return Futures.immediateFuture(FIXED_PARTITION);
        }
        if (!isUseTsKeyValuePartitioningOnRead()) {
            final long estimatedPartitionCount = estimatePartitionCount(minPartition, maxPartition);
            if  (estimatedPartitionCount <= useTsKeyValuePartitioningOnReadMaxEstimatedPartitionCount) {
                return Futures.immediateFuture(calculatePartitions(minPartition, maxPartition, (int) estimatedPartitionCount));
            }
        }
        return getPartitionsFromDB(tenantId, query, entityId, minPartition, maxPartition);
    }

    ListenableFuture<List<Long>> getPartitionsFromDB(TenantId tenantId, TsKvQuery query, EntityId entityId, long minPartition, long maxPartition) {
        TbResultSetFuture partitionsFuture = fetchPartitions(tenantId, entityId, query.getKey(), minPartition, maxPartition);
        return Futures.transformAsync(partitionsFuture, getPartitionsArrayFunction(), readResultsProcessingExecutor);
    }

    // Optimistic estimation of partition count, expected to be never called for infinite partitioning
    long estimatePartitionCount(long minPartition, long maxPartition) {
        if (maxPartition > minPartition) {
            return (maxPartition - minPartition) / tsFormat.getDurationMs() + 2; //at least 2 partitions, at max 2 partitions overestimated
        }
        return 1; // 1 or 0, but 1 is more optimistic
    }

    List<Long> calculatePartitions(long minPartition, long maxPartition) {
       return calculatePartitions(minPartition, maxPartition, 0);
    }

    List<Long> calculatePartitions(long minPartition, long maxPartition, int estimatedPartitionCount) {
        if (minPartition == maxPartition) {
            return Collections.singletonList(minPartition);
        }
        List<Long> partitions = estimatedPartitionCount > 0 ? new ArrayList<>(estimatedPartitionCount) : new ArrayList<>();

        long currentPartition = minPartition;
        LocalDateTime currentPartitionTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentPartition), ZoneOffset.UTC);

        while (maxPartition > currentPartition) {
            partitions.add(currentPartition);
            currentPartitionTime = calculateNextPartition(currentPartitionTime);
            currentPartition = currentPartitionTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        }

        partitions.add(maxPartition);

        return partitions;
    }

    private LocalDateTime calculateNextPartition(LocalDateTime time) {
        return time.plus(1, tsFormat.getTruncateUnit());
    }

    private AsyncFunction<List<Long>, List<TbResultSet>> getFetchChunksAsyncFunction(TenantId tenantId, EntityId entityId, String key, Aggregation aggregation, long startTs, long endTs) {
        return partitions -> {
            try {
                PreparedStatement proto = getFetchStmt(aggregation, DESC_ORDER);
                List<TbResultSetFuture> futures = new ArrayList<>(partitions.size());
                for (Long partition : partitions) {
                    log.trace("Fetching data for partition [{}] for entityType {} and entityId {}", partition, entityId.getEntityType(), entityId.getId());
                    BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());
                    stmtBuilder.setString(0, entityId.getEntityType().name());
                    stmtBuilder.setUuid(1, entityId.getId());
                    stmtBuilder.setString(2, key);
                    stmtBuilder.setLong(3, partition);
                    stmtBuilder.setLong(4, startTs);
                    stmtBuilder.setLong(5, endTs);
                    BoundStatement stmt = stmtBuilder.build();
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

    private boolean isFixedPartitioning() {
        return tsFormat.getTruncateUnit().equals(ChronoUnit.FOREVER);
    }

    private ListenableFuture<Integer> doSavePartition(TenantId tenantId, EntityId entityId, String key, long ttl, long partition) {
        log.debug("Saving partition {} for the entity [{}-{}] and key {}", partition, entityId.getEntityType(), entityId.getId(), key);
        PreparedStatement preparedStatement = ttl == 0 ? getPartitionInsertStmt() : getPartitionInsertTtlStmt();
        BoundStatement stmt = preparedStatement.bind();
        stmt = stmt.setString(0, entityId.getEntityType().name())
                .setUuid(1, entityId.getId())
                .setLong(2, partition)
                .setString(3, key);
        if (ttl > 0) {
            stmt = stmt.setInt(4, (int) ttl);
        }
        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> 0);
    }

    private class CacheCallback<Void> implements FutureCallback<Void> {
        private final CassandraPartitionCacheKey key;

        private CacheCallback(CassandraPartitionCacheKey key) {
            this.key = key;
        }

        @Override
        public void onSuccess(Void result) {
            cassandraTsPartitionsCache.put(key);
        }

        @Override
        public void onFailure(Throwable t) {

        }
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

    private void deleteAsync(TenantId tenantId, final QueryCursor cursor, final SimpleListenableFuture<Void> resultFuture) {
        if (!cursor.hasNextPartition()) {
            resultFuture.set(null);
        } else {
            PreparedStatement proto = getDeleteStmt();
            BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());
            stmtBuilder.setString(0, cursor.getEntityType());
            stmtBuilder.setUuid(1, cursor.getEntityId());
            stmtBuilder.setString(2, cursor.getKey());
            stmtBuilder.setLong(3, cursor.getNextPartition());
            stmtBuilder.setLong(4, cursor.getStartTs());
            stmtBuilder.setLong(5, cursor.getEndTs());

            BoundStatement stmt = stmtBuilder.build();

            Futures.addCallback(executeAsyncWrite(tenantId, stmt), new FutureCallback<AsyncResultSet>() {
                @Override
                public void onSuccess(@Nullable AsyncResultSet result) {
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
            stmtCreationLock.lock();
            try {
                if (deleteStmt == null) {
                    deleteStmt = prepare("DELETE FROM " + ModelConstants.TS_KV_CF +
                            " WHERE " + ModelConstants.ENTITY_TYPE_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.ENTITY_ID_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.KEY_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.PARTITION_COLUMN + EQUALS_PARAM
                            + "AND " + ModelConstants.TS_COLUMN + " >= ? "
                            + "AND " + ModelConstants.TS_COLUMN + " < ?");
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return deleteStmt;
    }

    private PreparedStatement getSaveWithNullStmt() {
        if (saveWithNullStmt == null) {
            stmtCreationLock.lock();
            try {
                if (saveWithNullStmt == null) {
                    saveWithNullStmt = prepare(INSERT_WITH_NULL);
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return saveWithNullStmt;
    }

    private PreparedStatement getSaveWithNullWithTtlStmt() {
        if (saveWithNullWithTtlStmt == null) {
            stmtCreationLock.lock();
            try {
                if (saveWithNullWithTtlStmt == null) {
                    saveWithNullWithTtlStmt = prepare(INSERT_WITH_NULL + " USING TTL ?");
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return saveWithNullWithTtlStmt;
    }

    private PreparedStatement getSaveStmt(DataType dataType) {
        if (saveStmts == null) {
            stmtCreationLock.lock();
            try {
                if (saveStmts == null) {
                    var stmts = new PreparedStatement[DataType.values().length];
                    for (DataType type : DataType.values()) {
                        stmts[type.ordinal()] = prepare(getPreparedStatementQuery(type));
                    }
                    saveStmts = stmts;
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return saveStmts[dataType.ordinal()];
    }

    private PreparedStatement getSaveTtlStmt(DataType dataType) {
        if (saveTtlStmts == null) {
            stmtCreationLock.lock();
            try {
                if (saveTtlStmts == null) {
                    var stmts = new PreparedStatement[DataType.values().length];
                    for (DataType type : DataType.values()) {
                        stmts[type.ordinal()] = prepare(getPreparedStatementQueryWithTtl(type));
                    }
                    saveTtlStmts = stmts;
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return saveTtlStmts[dataType.ordinal()];
    }

    private String getPreparedStatementQuery(DataType type) {
        return INSERT_INTO + ModelConstants.TS_KV_CF +
                "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                "," + ModelConstants.ENTITY_ID_COLUMN +
                "," + ModelConstants.KEY_COLUMN +
                "," + ModelConstants.PARTITION_COLUMN +
                "," + ModelConstants.TS_COLUMN +
                "," + getColumnName(type) + ")" +
                " VALUES(?, ?, ?, ?, ?, ?)";
    }

    private String getPreparedStatementQueryWithTtl(DataType type) {
        return getPreparedStatementQuery(type) + " USING TTL ?";
    }

    private PreparedStatement getPartitionInsertStmt() {
        if (partitionInsertStmt == null) {
            stmtCreationLock.lock();
            try {
                if (partitionInsertStmt == null) {
                    partitionInsertStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_PARTITIONS_CF +
                            "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                            "," + ModelConstants.ENTITY_ID_COLUMN +
                            "," + ModelConstants.PARTITION_COLUMN +
                            "," + ModelConstants.KEY_COLUMN + ")" +
                            " VALUES(?, ?, ?, ?)");
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return partitionInsertStmt;
    }

    private PreparedStatement getPartitionInsertTtlStmt() {
        if (partitionInsertTtlStmt == null) {
            stmtCreationLock.lock();
            try {
                if (partitionInsertTtlStmt == null) {
                    partitionInsertTtlStmt = prepare(INSERT_INTO + ModelConstants.TS_KV_PARTITIONS_CF +
                            "(" + ModelConstants.ENTITY_TYPE_COLUMN +
                            "," + ModelConstants.ENTITY_ID_COLUMN +
                            "," + ModelConstants.PARTITION_COLUMN +
                            "," + ModelConstants.KEY_COLUMN + ")" +
                            " VALUES(?, ?, ?, ?) USING TTL ?");
                }
            } finally {
                stmtCreationLock.unlock();
            }
        }
        return partitionInsertTtlStmt;
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

    private static void addValue(KvEntry kvEntry, BoundStatementBuilder stmt, int column) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                Optional<Boolean> booleanValue = kvEntry.getBooleanValue();
                booleanValue.ifPresent(b -> stmt.setBoolean(column, b));
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

    /**
     * //     * Select existing partitions from the table
     * //     * <code>{@link ModelConstants#TS_KV_PARTITIONS_CF}</code> for the given entity
     * //
     */
    private TbResultSetFuture fetchPartitions(TenantId tenantId, EntityId entityId, String key, long minPartition, long maxPartition) {
        Select select = QueryBuilder.selectFrom(ModelConstants.TS_KV_PARTITIONS_CF).column(ModelConstants.PARTITION_COLUMN)
                .whereColumn(ModelConstants.ENTITY_TYPE_COLUMN).isEqualTo(literal(entityId.getEntityType().name()))
                .whereColumn(ModelConstants.ENTITY_ID_COLUMN).isEqualTo(literal(entityId.getId()))
                .whereColumn(ModelConstants.KEY_COLUMN).isEqualTo(literal(key))
                .whereColumn(ModelConstants.PARTITION_COLUMN).isGreaterThanOrEqualTo(literal(minPartition))
                .whereColumn(ModelConstants.PARTITION_COLUMN).isLessThanOrEqualTo(literal(maxPartition));
        return executeAsyncRead(tenantId, select.build());
    }

    private PreparedStatement getFetchStmt(Aggregation aggType, String orderBy) {
        switch (orderBy) {
            case ASC_ORDER:
                if (fetchStmtsAsc == null) {
                    stmtCreationLock.lock();
                    try {
                        if (fetchStmtsAsc == null) {
                            fetchStmtsAsc = initFetchStmt(orderBy);
                        }
                    } finally {
                        stmtCreationLock.unlock();
                    }
                }
                return fetchStmtsAsc[aggType.ordinal()];
            case DESC_ORDER:
                if (fetchStmtsDesc == null) {
                    stmtCreationLock.lock();
                    try {
                        if (fetchStmtsDesc == null) {
                            fetchStmtsDesc = initFetchStmt(orderBy);
                        }
                    } finally {
                        stmtCreationLock.unlock();
                    }
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
                        + "AND " + ModelConstants.TS_COLUMN + " >= ? "
                        + "AND " + ModelConstants.TS_COLUMN + " < ?"
                        + (type == Aggregation.NONE ? " ORDER BY " + ModelConstants.TS_COLUMN + " " + orderBy + " LIMIT ?" : ""));
            }
        }
        return fetchStmts;
    }
}
