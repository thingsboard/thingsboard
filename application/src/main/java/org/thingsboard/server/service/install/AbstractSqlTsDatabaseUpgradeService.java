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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;

@Slf4j
public abstract class AbstractSqlTsDatabaseUpgradeService {

    protected static final String CALL_REGEX = "call ";
    protected static final String CHECK_VERSION = "check_version()";
    protected static final String DROP_TABLE = "DROP TABLE ";
    protected static final String DROP_FUNCTION_IF_EXISTS = "DROP FUNCTION IF EXISTS ";

    private static final String CALL_CHECK_VERSION = CALL_REGEX + CHECK_VERSION;


    private static final String FUNCTION = "function: {}";
    private static final String DROP_STATEMENT = "drop statement: {}";
    private static final String QUERY = "query: {}";
    private static final String SUCCESSFULLY_EXECUTED = "Successfully executed ";
    private static final String FAILED_TO_EXECUTE = "Failed to execute ";
    private static final String FAILED_DUE_TO = " due to: {}";

    protected static final String SUCCESSFULLY_EXECUTED_FUNCTION = SUCCESSFULLY_EXECUTED + FUNCTION;
    protected static final String FAILED_TO_EXECUTE_FUNCTION_DUE_TO = FAILED_TO_EXECUTE + FUNCTION + FAILED_DUE_TO;

    protected static final String SUCCESSFULLY_EXECUTED_DROP_STATEMENT = SUCCESSFULLY_EXECUTED + DROP_STATEMENT;
    protected static final String FAILED_TO_EXECUTE_DROP_STATEMENT = FAILED_TO_EXECUTE + DROP_STATEMENT + FAILED_DUE_TO;

    protected static final String SUCCESSFULLY_EXECUTED_QUERY = SUCCESSFULLY_EXECUTED + QUERY;
    protected static final String FAILED_TO_EXECUTE_QUERY = FAILED_TO_EXECUTE + QUERY + FAILED_DUE_TO;

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
        log.info("Check the current PostgreSQL version...");
        boolean versionValid = false;
        try {
            CallableStatement callableStatement = conn.prepareCall("{? = " + CALL_CHECK_VERSION + " }");
            callableStatement.registerOutParameter(1, Types.BOOLEAN);
            callableStatement.execute();
            versionValid = callableStatement.getBoolean(1);
            callableStatement.close();
        } catch (Exception e) {
            log.info("Failed to check current PostgreSQL version due to: {}", e.getMessage());
        }
        return versionValid;
    }

    protected void executeFunction(Connection conn, String query) {
        log.info("{} ... ", query);
        try {
            CallableStatement callableStatement = conn.prepareCall("{" + query + "}");
            callableStatement.execute();
            SQLWarning warnings = callableStatement.getWarnings();
            if (warnings != null) {
                log.info("{}", warnings.getMessage());
                SQLWarning nextWarning = warnings.getNextWarning();
                while (nextWarning != null) {
                    log.info("{}", nextWarning.getMessage());
                    nextWarning = nextWarning.getNextWarning();
                }
            }
            callableStatement.close();
            log.info(SUCCESSFULLY_EXECUTED_FUNCTION, query.replace(CALL_REGEX, ""));
            Thread.sleep(2000);
        } catch (Exception e) {
            log.info(FAILED_TO_EXECUTE_FUNCTION_DUE_TO, query, e.getMessage());
        }
    }

    protected void executeDropStatement(Connection conn, String query) {
        try {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info(SUCCESSFULLY_EXECUTED_DROP_STATEMENT, query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.info(FAILED_TO_EXECUTE_DROP_STATEMENT, query, e.getMessage());
        }
    }

    protected void executeQuery(Connection conn, String query) {
        try {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info(SUCCESSFULLY_EXECUTED_QUERY, query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.info(FAILED_TO_EXECUTE_QUERY, query, e.getMessage());
        }
    }

}