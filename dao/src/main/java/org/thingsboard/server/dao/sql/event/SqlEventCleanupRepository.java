/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Repository
public class SqlEventCleanupRepository extends JpaAbstractDaoListeningExecutorService implements EventCleanupRepository {

    private static final String SELECT_PARTITIONS_STMT = "SELECT tablename from pg_tables WHERE schemaname = 'public' and tablename like concat(?, '_%')";
    private static final int PSQL_VERSION_14 = 140000;

    @Autowired
    private EventPartitionConfiguration partitionConfiguration;

    private volatile Integer currentServerVersion;

    @Override
    public void cleanupEvents(long eventExpTime, boolean debug) {
        for (EventType eventType : EventType.values()) {
            if (eventType.isDebug() == debug) {
                cleanupEvents(eventType, eventExpTime);
            }
        }
    }

    @Override
    public void migrateEvents(long regularEventTs, long debugEventTs) {
        callMigrateFunction("migrate_regular_events", regularEventTs, partitionConfiguration.getRegularPartitionSizeInHours());
        callMigrateFunction("migrate_debug_events", debugEventTs, partitionConfiguration.getDebugPartitionSizeInHours());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement dropFunction1 = connection.prepareStatement("DROP PROCEDURE IF EXISTS migrate_regular_events");
             PreparedStatement dropFunction2 = connection.prepareStatement("DROP PROCEDURE IF EXISTS migrate_debug_events");
             PreparedStatement dropTable = connection.prepareStatement("DROP TABLE IF EXISTS event")) {
            dropFunction1.execute();
            dropFunction2.execute();
            dropTable.execute();
        } catch (SQLException e) {
            log.error("SQLException occurred during drop of the `events` table", e);
            throw new RuntimeException(e);
        }
    }

    private void callMigrateFunction(String functionName, long startTs, int partitionSizeInHours) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call " + functionName + "(?,?)")) {
            stmt.setLong(1, startTs);
            stmt.setInt(2, partitionSizeInHours);
            stmt.execute();
        } catch (SQLException e) {
            if (e.getMessage() == null || !e.getMessage().contains("relation \"event\" does not exist")) {
                log.error("[{}] SQLException occurred during execution of {} with parameters {} and {}", functionName, startTs, partitionSizeInHours, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void cleanupEvents(EventType eventType, long eventExpTime) {
        var partitionDuration = partitionConfiguration.getPartitionSizeInMs(eventType);
        List<Long> partitions = fetchPartitions(eventType);
        for (var partitionTs : partitions) {
            var partitionEndTs = partitionTs + partitionDuration;
            if (partitionEndTs < eventExpTime) {
                log.info("[{}] Detaching expired partition: [{}-{}]", eventType, partitionTs, partitionEndTs);
                if (detachAndDropPartition(eventType, partitionTs)) {
                    log.info("[{}] Detached expired partition: {}", eventType, partitionTs);
                }
            } else {
                log.debug("[{}] Skip valid partition: {}", eventType, partitionTs);
            }
        }
    }

    private List<Long> fetchPartitions(EventType eventType) {
        List<Long> partitions = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_PARTITIONS_STMT)) {
            stmt.setString(1, eventType.getTable());
            stmt.execute();
            try (ResultSet resultSet = stmt.getResultSet()) {
                while (resultSet.next()) {
                    String partitionTableName = resultSet.getString(1);
                    String partitionTsStr = partitionTableName.substring(eventType.getTable().length() + 1);
                    try {
                        partitions.add(Long.parseLong(partitionTsStr));
                    } catch (NumberFormatException nfe) {
                        log.warn("Failed to parse table name: {}", partitionTableName);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during events TTL task execution ", e);
        }
        return partitions;
    }

    private boolean detachAndDropPartition(EventType eventType, long partitionTs) {
        String tablePartition = eventType.getTable() + "_" + partitionTs;
        String detachPsqlStmtStr = "ALTER TABLE " + eventType.getTable() + " DETACH PARTITION " + tablePartition;
        if (getCurrentServerVersion() >= PSQL_VERSION_14) {
            detachPsqlStmtStr += " CONCURRENTLY";
        }

        String dropStmtStr = "DROP TABLE " + tablePartition;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement detachStmt = connection.prepareStatement(detachPsqlStmtStr);
             PreparedStatement dropStmt = connection.prepareStatement(dropStmtStr)) {
            detachStmt.execute();
            dropStmt.execute();
            return true;
        } catch (SQLException e) {
            log.error("[{}] SQLException occurred during detach and drop of the partition: {}", eventType, partitionTs, e);
        }
        return false;
    }

    private synchronized int getCurrentServerVersion() {
        if (currentServerVersion == null) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement versionStmt = connection.prepareStatement("SELECT current_setting('server_version_num')")) {
                versionStmt.execute();
                try (ResultSet resultSet = versionStmt.getResultSet()) {
                    while (resultSet.next()) {
                        currentServerVersion = resultSet.getInt(1);
                    }
                }
            } catch (SQLException e) {
                log.warn("SQLException occurred during fetch of the server version", e);
            }
            if (currentServerVersion == null) {
                currentServerVersion = 0;
            }
        }
        return currentServerVersion;
    }

}
