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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

@Service
@TimescaleDBTsDao
@Profile("install")
@Slf4j
public class TimescaleTsDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements TsDatabaseSchemaService {

    @Value("${sql.timescale.chunk_time_interval:86400000}")
    private long chunkTimeInterval;

    @Value("${sql.timescale.compression_enabled:true}")
    private boolean compressionEnabled;

    @Value("${sql.timescale.compression_policy_days:30}")
    private int compressionPolicyDays;

    @Value("${sql.timescale.compression_schedule_interval:604800000}")
    private long compressionScheduleInterval;

    public TimescaleTsDatabaseSchemaService() {
        super("schema-timescale.sql", null);
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        super.createDatabaseSchema();
        executeQuery("SELECT create_hypertable('ts_kv', 'ts', chunk_time_interval => " + chunkTimeInterval + ", if_not_exists => true);");

        if (compressionEnabled) {
            log.info("Enabling TimescaleDB compression for ts_kv table with policy of {} days and schedule interval of {} ms", 
                    compressionPolicyDays, compressionScheduleInterval);
            try {
                // Enable compression on the hypertable
                executeQuery("ALTER TABLE ts_kv SET (timescaledb.compress = true);");

                // Add compression policy to compress data older than the specified number of days
                // Also specify how often the compression job should run (schedule_interval)
                String compressionPolicy = String.format(
                        "SELECT add_compression_policy('ts_kv', INTERVAL '%d days', schedule_interval => INTERVAL '%d milliseconds');", 
                        compressionPolicyDays, compressionScheduleInterval);
                executeQuery(compressionPolicy);

                log.info("TimescaleDB compression enabled successfully with policy of {} days and schedule interval of {} ms", 
                        compressionPolicyDays, compressionScheduleInterval);
            } catch (Exception e) {
                log.error("Failed to enable TimescaleDB compression", e);
            }
        }
    }

}
