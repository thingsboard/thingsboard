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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

@Slf4j
public abstract class AbstractSqlTsDatabaseUpgradeService {

    protected static final String CALL_REGEX = "call ";
    protected static final String DROP_TABLE = "DROP TABLE ";
    protected static final String DROP_PROCEDURE_IF_EXISTS = "DROP PROCEDURE IF EXISTS ";

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Autowired
    protected InstallScripts installScripts;

    protected abstract void loadSql(Connection conn);

    protected void loadFunctions(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
        conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
    }

    protected boolean checkVersion(Connection conn) {
        boolean versionValid = false;
        try {
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT current_setting('server_version_num')");
            resultSet.next();
            if(resultSet.getLong(1) > 110000) {
                versionValid = true;
            }
            statement.close();
        } catch (Exception e) {
            log.info("Failed to check current PostgreSQL version due to: {}", e.getMessage());
        }
        return versionValid;
    }

    protected void executeQuery(Connection conn, String query) {
        try {
            Statement statement = conn.createStatement();
            statement.execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            SQLWarning warnings = statement.getWarnings();
            if (warnings != null) {
                log.info("{}", warnings.getMessage());
                SQLWarning nextWarning = warnings.getNextWarning();
                while (nextWarning != null) {
                    log.info("{}", nextWarning.getMessage());
                    nextWarning = nextWarning.getNextWarning();
                }
            }
            Thread.sleep(5000);
            log.info("Successfully executed query: {}", query);
        } catch (InterruptedException | SQLException e) {
            log.info("Failed to execute query: {} due to: {}", query, e.getMessage());
        }
    }

}