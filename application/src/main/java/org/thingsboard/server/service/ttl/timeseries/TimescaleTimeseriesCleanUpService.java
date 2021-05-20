/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.ttl.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.sql.Connection;
import java.sql.SQLException;

@TimescaleDBTsDao
@Service
@Slf4j
public class TimescaleTimeseriesCleanUpService extends AbstractTimeseriesCleanUpService {

    @Override
    protected void doCleanUp(Connection connection) throws SQLException {
        long totalEntitiesTelemetryRemoved = executeQuery(connection, "call cleanup_timeseries_by_ttl('" + ModelConstants.NULL_UUID + "'," + systemTtl + ", 0);");
        log.info("Total telemetry removed stats by TTL for entities: [{}]", totalEntitiesTelemetryRemoved);
    }
}
