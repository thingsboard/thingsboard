/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sqlts.insert.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.timeseries.SqlPartition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Primary
@Repository
@Slf4j
public class SqlPartitioningRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SELECT_PARTITIONS_STMT = "SELECT tablename from pg_tables WHERE schemaname = current_schema() and tablename like concat(?, '_%')";

    private static final int PSQL_VERSION_14 = 140000;
    private volatile Integer currentServerVersion;

    private final Map<String, Map<Long, SqlPartition>> tablesPartitions = new ConcurrentHashMap<>();
    private final ReentrantLock partitionCreationLock = new ReentrantLock();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void save(SqlPartition partition) {
        getJdbcTemplate().execute(partition.getQuery());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED) // executing non-transactionally, so that parent transaction is not aborted on partition save error
    public void createPartitionIfNotExists(String table, long entityTs, long partitionDurationMs) {
        long partitionStartTs = calculatePartitionStartTime(entityTs, partitionDurationMs);
        Map<Long, SqlPartition> partitions = tablesPartitions.computeIfAbsent(table, t -> new ConcurrentHashMap<>());
        if (!partitions.containsKey(partitionStartTs)) {
            SqlPartition partition = new SqlPartition(table, partitionStartTs, getPartitionEndTime(partitionStartTs, partitionDurationMs), Long.toString(partitionStartTs));
            partitionCreationLock.lock();
            try {
                if (partitions.containsKey(partitionStartTs)) return;
                log.info("Saving partition {}-{} for table {}", partition.getStart(), partition.getEnd(), table);
                save(partition);
                log.trace("Adding partition to map: {}", partition);
                partitions.put(partition.getStart(), partition);
            } catch (Exception e) {
                String error = ExceptionUtils.getRootCauseMessage(e);
                if (StringUtils.containsAny(error, "would overlap partition", "already exists")) {
                    partitions.put(partition.getStart(), partition);
                    log.debug("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                } else {
                    log.warn("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                }
            } finally {
                partitionCreationLock.unlock();
            }
        }
    }

    public long dropPartitionsBefore(String table, long ts, long partitionDurationMs) {
        List<Long> partitions = fetchPartitions(table);
        long lastDroppedPartitionEndTime = -1;
        for (Long partitionStartTime : partitions) {
            long partitionEndTime = getPartitionEndTime(partitionStartTime, partitionDurationMs);
            if (partitionEndTime < ts) {
                log.info("[{}] Detaching expired partition: [{}-{}]", table, partitionStartTime, partitionEndTime);
                boolean success = detachAndDropPartition(table, partitionStartTime);
                if (success) {
                    log.info("[{}] Detached expired partition: {}", table, partitionStartTime);
                    lastDroppedPartitionEndTime = Math.max(partitionEndTime, lastDroppedPartitionEndTime);
                }
            } else {
                log.debug("[{}] Skipping valid partition: {}", table, partitionStartTime);
            }
        }
        return lastDroppedPartitionEndTime;
    }

    public void cleanupPartitionsCache(String table, long expTime, long partitionDurationMs) {
        Map<Long, SqlPartition> partitions = tablesPartitions.get(table);
        if (partitions == null) return;
        partitions.keySet().removeIf(startTime -> getPartitionEndTime(startTime, partitionDurationMs) < expTime);
    }

    private boolean detachAndDropPartition(String table, long partitionTs) {
        Map<Long, SqlPartition> cachedPartitions = tablesPartitions.get(table);
        if (cachedPartitions != null) cachedPartitions.remove(partitionTs);

        String tablePartition = table + "_" + partitionTs;
        String detachPsqlStmtStr = "ALTER TABLE " + table + " DETACH PARTITION " + tablePartition;

        // hotfix of ERROR: partition "integration_debug_event_1678323600000" already pending detach in partitioned table "public.integration_debug_event"
        // https://github.com/thingsboard/thingsboard/issues/8271
        // if (getCurrentServerVersion() >= PSQL_VERSION_14) {
        //    detachPsqlStmtStr += " CONCURRENTLY";
        // }

        String dropStmtStr = "DROP TABLE " + tablePartition;
        try {
            getJdbcTemplate().execute(detachPsqlStmtStr);
            getJdbcTemplate().execute(dropStmtStr);
            return true;
        } catch (DataAccessException e) {
            log.error("[{}] Error occurred trying to detach and drop the partition {} ", table, partitionTs, e);
        }
        return false;
    }

    private static long getPartitionEndTime(long startTime, long partitionDurationMs) {
        return startTime + partitionDurationMs;
    }

    public List<Long> fetchPartitions(String table) {
        List<Long> partitions = new ArrayList<>();
        List<String> partitionsTables = getJdbcTemplate().queryForList(SELECT_PARTITIONS_STMT, String.class, table);
        for (String partitionTableName : partitionsTables) {
            String partitionTsStr = partitionTableName.substring(table.length() + 1);
            try {
                partitions.add(Long.parseLong(partitionTsStr));
            } catch (NumberFormatException nfe) {
                log.debug("Failed to parse table name: {}", partitionTableName);
            }
        }
        return partitions;
    }

    public long calculatePartitionStartTime(long ts, long partitionDuration) {
        return ts - (ts % partitionDuration);
    }

    private synchronized int getCurrentServerVersion() {
        if (currentServerVersion == null) {
            try {
                currentServerVersion = getJdbcTemplate().queryForObject("SELECT current_setting('server_version_num')", Integer.class);
            } catch (Exception e) {
                log.warn("Error occurred during fetch of the server version", e);
            }
            if (currentServerVersion == null) {
                currentServerVersion = 0;
            }
        }
        return currentServerVersion;
    }

    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
