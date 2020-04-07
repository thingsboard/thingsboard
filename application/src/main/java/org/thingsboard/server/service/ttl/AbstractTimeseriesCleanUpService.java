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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.thingsboard.server.dao.util.PsqlTsAnyDao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

@PsqlTsAnyDao
@Slf4j
public abstract class AbstractTimeseriesCleanUpService {

    @Value("${sql.ttl.ts_key_value_ttl}")
    protected long systemTtl;

    @Value("${sql.ttl.enabled}")
    private boolean ttlTaskExecutionEnabled;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Scheduled(initialDelayString = "${sql.ttl.execution_interval_ms}", fixedDelayString = "${sql.ttl.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled) {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                doCleanUp(conn);
            } catch (SQLException e) {
                log.error("SQLException occurred during TTL task execution ", e);
            }
        }
    }

    protected abstract void doCleanUp(Connection connection);

    protected long executeQuery(Connection conn, String query) {
        long removed = 0L;
        try {
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            getWarnings(statement);
            resultSet.next();
            removed = resultSet.getLong(1);
            log.debug("Successfully executed query: {}", query);
        } catch (SQLException e) {
            log.debug("Failed to execute query: {} due to: {}", query, e.getMessage());
        }
        return removed;
    }

    private void getWarnings(Statement statement) throws SQLException {
        SQLWarning warnings = statement.getWarnings();
        if (warnings != null) {
            log.debug("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.debug("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

}