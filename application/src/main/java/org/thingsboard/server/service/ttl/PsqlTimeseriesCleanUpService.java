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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.PsqlTsDao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@PsqlTsDao
@Service
@Slf4j
public class PsqlTimeseriesCleanUpService extends TimeseriesCleanUpServiceImpl {

    @Scheduled(fixedDelayString = "${sql.ttl.execution_interval_ms}")
    @Override
    public void cleanUp() {
        if (ttlTaskExecutionEnabled) {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                long totalEntitiesTelemetryRemoved = executeQuery(conn, "call cleanup_timeseries_by_ttl('" + ModelConstants.NULL_UUID_STR + "'," + systemTtl + ", 0);");
                long totalPartitionsRemoved = executeQuery(conn, "call drop_empty_partitions(0);");
                log.info("Total telemetry deleted stats by TTL for entities: [{}]", totalEntitiesTelemetryRemoved);
                log.info("Total empty timeseries partitions deleted: [{}]", totalPartitionsRemoved);
            } catch (SQLException e) {
                log.error("SQLException occurred during TTL task execution ", e);
            }
        }
    }
}