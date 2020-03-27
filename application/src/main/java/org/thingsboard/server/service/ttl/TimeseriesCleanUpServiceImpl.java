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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.PsqlTsAnyDao;
import org.thingsboard.server.service.install.InstallScripts;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

@PsqlTsAnyDao
@Service
@Slf4j
public abstract class TimeseriesCleanUpServiceImpl implements TimeseriesCleanUpService {

    private static final String TTL_FUNCTIONS = "ttl-functions.sql";

    @Value("${sql.ttl.enabled}")
    protected boolean ttlTaskExecutionEnabled;

    @Value("${sql.ttl.ts_key_value_ttl}")
    protected long systemTtl;

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Autowired
    private InstallScripts installScripts;

    @PostConstruct
    public void init() {
        if (ttlTaskExecutionEnabled) {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                loadTTLFunctions(conn);
            } catch (SQLException e) {
                log.error("SQLException occurred during establishing connection to DataBase: ", e);
            }
        }
    }

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

    private void loadTTLFunctions(Connection conn) {
        Path ttlFunctionsFile = Paths.get(installScripts.getDataDir(), "ttl", TTL_FUNCTIONS);
        try {
            log.info("Creating TTL functions...");
            String sql = new String(Files.readAllBytes(ttlFunctionsFile), StandardCharsets.UTF_8);
            conn.createStatement().execute(sql);
            log.info("TTL functions successfully created!");
        } catch (SQLException | IOException e) {
            log.info("Failed to create TTL functions due to: {}", e.getMessage());
        }
    }
}