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
package org.thingsboard.server.dao.sqlts.sql;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.sqlts.AbstractChunkedAggregationTimeseriesDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.timeseries.SqlPartition;
import org.thingsboard.server.dao.timeseries.SqlTsPartitionDate;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@SqlTsDao
public class JpaSqlTimeseriesDao extends AbstractChunkedAggregationTimeseriesDao {

    private final Map<Long, SqlPartition> partitions = new ConcurrentHashMap<>();
    private static final ReentrantLock partitionCreationLock = new ReentrantLock();

    @Autowired
    private SqlPartitioningRepository partitioningRepository;
    @Autowired
    private KeyDictionaryDao keyDictionaryDao;

    private SqlTsPartitionDate tsFormat;

    @Value("${sql.postgres.ts_key_value_partitioning:MONTHS}")
    private String partitioning;


    @Override
    protected void init() {
        super.init();
        Optional<SqlTsPartitionDate> partition = SqlTsPartitionDate.parse(partitioning);
        if (partition.isPresent()) {
            tsFormat = partition.get();
        } else {
            log.warn("Incorrect configuration of partitioning {}", partitioning);
            throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
        }
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        int dataPointDays = getDataPointDays(tsKvEntry, computeTtl(ttl));
        savePartitionIfNotExist(tsKvEntry.getTs());
        String strKey = tsKvEntry.getKey();
        Integer keyId = keyDictionaryDao.getOrSaveKeyId(strKey);
        TsKvEntity entity = new TsKvEntity();
        entity.setEntityId(entityId.getId());
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(keyId);
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        entity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));
        log.trace("Saving entity: {}", entity);
        return Futures.transform(tsQueue.add(entity), v -> dataPointDays, MoreExecutors.directExecutor());
    }

    @Override
    public void cleanup(long systemTtl) {
        if (systemTtl > 0) {
            cleanupPartitions(systemTtl);
        }
        super.cleanup(systemTtl);
    }

    private void cleanupPartitions(long systemTtl) {
        log.info("Going to cleanup old timeseries data partitions using partition type: {} and ttl: {}s", partitioning, systemTtl);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call drop_partitions_by_system_ttl(?,?,?)")) {
            stmt.setString(1, partitioning);
            stmt.setLong(2, systemTtl);
            stmt.setLong(3, 0);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultSet.next();
                log.info("Total partitions removed by TTL: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during TTL task execution ", e);
        }
    }

    private void savePartitionIfNotExist(long ts) {
        if (!tsFormat.equals(SqlTsPartitionDate.INDEFINITE) && ts >= 0) {
            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
            LocalDateTime localDateTimeStart = tsFormat.trancateTo(time);
            long partitionStartTs = toMills(localDateTimeStart);
            if (partitions.get(partitionStartTs) == null) {
                LocalDateTime localDateTimeEnd = tsFormat.plusTo(localDateTimeStart);
                long partitionEndTs = toMills(localDateTimeEnd);
                ZonedDateTime zonedDateTime = localDateTimeStart.atZone(ZoneOffset.UTC);
                String partitionDate = zonedDateTime.format(DateTimeFormatter.ofPattern(tsFormat.getPattern()));
                savePartition(new SqlPartition(SqlPartition.TS_KV, partitionStartTs, partitionEndTs, partitionDate));
            }
        }
    }

    private void savePartition(SqlPartition sqlPartition) {
        if (!partitions.containsKey(sqlPartition.getStart())) {
            partitionCreationLock.lock();
            try {
                log.trace("Saving partition: {}", sqlPartition);
                partitioningRepository.save(sqlPartition);
                log.trace("Adding partition to Set: {}", sqlPartition);
                partitions.put(sqlPartition.getStart(), sqlPartition);
            } catch (DataIntegrityViolationException ex) {
                log.trace("Error occurred during partition save:", ex);
                if (ex.getCause() instanceof ConstraintViolationException) {
                    log.warn("Saving partition [{}] rejected. Timeseries data will save to the ts_kv_indefinite (DEFAULT) partition.", sqlPartition.getPartitionDate());
                    partitions.put(sqlPartition.getStart(), sqlPartition);
                } else {
                    throw new RuntimeException(ex);
                }
            } finally {
                partitionCreationLock.unlock();
            }
        }
    }

    private static long toMills(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
