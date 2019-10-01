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
package org.thingsboard.server.dao.sqlts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.timeseries.PsqlPartition;
import org.thingsboard.server.dao.timeseries.SqlTsPartitionDate;
import org.thingsboard.server.dao.timeseries.TsInsertExecutorType;
import org.thingsboard.server.dao.util.PsqlDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.timeseries.SqlTsPartitionDate.EPOCH_START;

@Slf4j
public abstract class AbstractSqlTimeseriesDao extends JpaAbstractDaoListeningExecutorService {

    private static final String DESC_ORDER = "DESC";

    @Value("${sql.ts_inserts_executor_type}")
    private String insertExecutorType;

    @Value("${sql.ts_inserts_fixed_thread_pool_size}")
    private int insertFixedThreadPoolSize;

    @PsqlDao
    @Value("${sql.ts_key_value_partitioning}")
    private String partitioning;

    @Value("${spring.datasource.hikari.maximumPoolSize}")
    private int maximumPoolSize;

    private SqlTsPartitionDate tsFormat;

    protected ListeningExecutorService insertService;

    @PostConstruct
    void init() {
        Optional<TsInsertExecutorType> executorTypeOptional = TsInsertExecutorType.parse(insertExecutorType);
        TsInsertExecutorType executorType;
        executorType = executorTypeOptional.orElse(TsInsertExecutorType.FIXED);
        switch (executorType) {
            case SINGLE:
                insertService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
                break;
            case FIXED:
            case CACHED:
                int poolSize = insertFixedThreadPoolSize;
                if (poolSize <= 0) {
                    poolSize = maximumPoolSize * 4;
                }
                insertService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(poolSize));
                break;
        }
        Optional<SqlTsPartitionDate> partition = SqlTsPartitionDate.parse(partitioning);
        if (partition.isPresent()) {
            tsFormat = partition.get();
        } else {
            log.warn("Incorrect configuration of partitioning {}", partitioning);
            throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
        }
    }

    @PreDestroy
    void preDestroy() {
        if (insertService != null) {
            insertService.shutdown();
        }
    }

//    protected PsqlPartition toPartition(long ts) {
//        if(!isFixedPartitioning()) {
//            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
//            LocalDateTime localDateTimeStart = tsFormat.trancateTo(time);
//            if (localDateTimeStart != null) {
//                LocalDateTime localDateTimeEnd = tsFormat.plusTo(localDateTimeStart);
//                if (localDateTimeEnd != null) {
//                    return new PsqlPartition(toMills(localDateTimeStart), toMills(localDateTimeEnd), tsFormat.getPattern());
//                }
//            }
//        }
//        return new PsqlPartition(Long.MIN_VALUE, Long.MAX_VALUE, tsFormat.getPattern());
//    }

    protected PsqlPartition toPartition(long ts) {
        if(!isFixedPartitioning()) {
            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
            LocalDateTime localDateTimeStart = tsFormat.trancateTo(time);
            if(localDateTimeStart == SqlTsPartitionDate.EPOCH_START) {
                return new PsqlPartition(toMills(EPOCH_START), Long.MAX_VALUE, tsFormat.getPattern());
            } else {
                LocalDateTime localDateTimeEnd = tsFormat.plusTo(localDateTimeStart);
                return new PsqlPartition(toMills(localDateTimeStart), toMills(localDateTimeEnd), tsFormat.getPattern());
            }
        }
        return new PsqlPartition(Long.MIN_VALUE, Long.MAX_VALUE, tsFormat.getPattern());
    }

    private boolean isFixedPartitioning() {
        return tsFormat.getTruncateUnit().equals(ChronoUnit.FOREVER);
    }

    private long toMills(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    protected ListenableFuture<List<TsKvEntry>> processFindAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> futures = queries
                .stream()
                .map(query -> findAllAsync(tenantId, entityId, query))
                .collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<List<List<TsKvEntry>>, List<TsKvEntry>>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<List<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
        }, service);
    }

    protected abstract ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query);

    protected ListenableFuture<List<TsKvEntry>> getTskvEntriesFuture(ListenableFuture<List<Optional<TsKvEntry>>> future) {
        return Futures.transform(future, new Function<List<Optional<TsKvEntry>>, List<TsKvEntry>>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<Optional<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            }
        }, service);
    }

    protected ListenableFuture<List<TsKvEntry>> findNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        return findAllAsync(tenantId, entityId, findNewLatestQuery);
    }
}