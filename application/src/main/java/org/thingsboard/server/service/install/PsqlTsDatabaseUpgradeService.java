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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

@Service
@Profile("install")
@Slf4j
@SqlTsDao
@PsqlDao
public class PsqlTsDatabaseUpgradeService extends AbstractSqlTsDatabaseUpgradeService implements DatabaseTsUpgradeService {

    private static final String LOAD_FUNCTIONS_SQL = "schema_update_psql_ts.sql";

    private static final String TS_KV_OLD = "ts_kv_old;";
    private static final String TS_KV_LATEST_OLD = "ts_kv_latest_old;";

    private static final String CREATE_PARTITION_TS_KV_TABLE = "create_partition_ts_kv_table()";
    private static final String CREATE_NEW_TS_KV_LATEST_TABLE = "create_new_ts_kv_latest_table()";
    private static final String CREATE_PARTITIONS = "create_partitions()";
    private static final String CREATE_TS_KV_DICTIONARY_TABLE = "create_ts_kv_dictionary_table()";
    private static final String INSERT_INTO_DICTIONARY = "insert_into_dictionary()";
    private static final String INSERT_INTO_TS_KV = "insert_into_ts_kv()";
    private static final String INSERT_INTO_TS_KV_LATEST = "insert_into_ts_kv_latest()";

    private static final String CALL_CREATE_PARTITION_TS_KV_TABLE = CALL_REGEX + CREATE_PARTITION_TS_KV_TABLE;
    private static final String CALL_CREATE_NEW_TS_KV_LATEST_TABLE = CALL_REGEX + CREATE_NEW_TS_KV_LATEST_TABLE;
    private static final String CALL_CREATE_PARTITIONS = CALL_REGEX + CREATE_PARTITIONS;
    private static final String CALL_CREATE_TS_KV_DICTIONARY_TABLE = CALL_REGEX + CREATE_TS_KV_DICTIONARY_TABLE;
    private static final String CALL_INSERT_INTO_DICTIONARY = CALL_REGEX + INSERT_INTO_DICTIONARY;
    private static final String CALL_INSERT_INTO_TS_KV = CALL_REGEX + INSERT_INTO_TS_KV;
    private static final String CALL_INSERT_INTO_TS_KV_LATEST = CALL_REGEX + INSERT_INTO_TS_KV_LATEST;

    private static final String DROP_TABLE_TS_KV_OLD = DROP_TABLE + TS_KV_OLD;
    private static final String DROP_TABLE_TS_KV_LATEST_OLD = DROP_TABLE + TS_KV_LATEST_OLD;

    private static final String DROP_PROCEDURE_CREATE_PARTITION_TS_KV_TABLE = DROP_PROCEDURE_IF_EXISTS + CREATE_PARTITION_TS_KV_TABLE;
    private static final String DROP_PROCEDURE_CREATE_NEW_TS_KV_LATEST_TABLE = DROP_PROCEDURE_IF_EXISTS + CREATE_NEW_TS_KV_LATEST_TABLE;
    private static final String DROP_PROCEDURE_CREATE_PARTITIONS = DROP_PROCEDURE_IF_EXISTS + CREATE_PARTITIONS;
    private static final String DROP_PROCEDURE_CREATE_TS_KV_DICTIONARY_TABLE = DROP_PROCEDURE_IF_EXISTS + CREATE_TS_KV_DICTIONARY_TABLE;
    private static final String DROP_PROCEDURE_INSERT_INTO_DICTIONARY = DROP_PROCEDURE_IF_EXISTS + INSERT_INTO_DICTIONARY;
    private static final String DROP_PROCEDURE_INSERT_INTO_TS_KV = DROP_PROCEDURE_IF_EXISTS + INSERT_INTO_TS_KV;
    private static final String DROP_PROCEDURE_INSERT_INTO_TS_KV_LATEST = DROP_PROCEDURE_IF_EXISTS + INSERT_INTO_TS_KV_LATEST;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "2.4.3":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Check the current PostgreSQL version...");
                    boolean versionValid = checkVersion(conn);
                    if (!versionValid) {
                        throw new RuntimeException("PostgreSQL version should be at least more than 11, please upgrade your PostgreSQL and restart the script!");
                    } else {
                        log.info("PostgreSQL version is valid!");
                        log.info("Load upgrade functions ...");
                        loadSql(conn);
                        log.info("Updating timeseries schema ...");
                        executeQuery(conn, CALL_CREATE_PARTITION_TS_KV_TABLE);
                        executeQuery(conn, CALL_CREATE_PARTITIONS);
                        executeQuery(conn, CALL_CREATE_TS_KV_DICTIONARY_TABLE);
                        executeQuery(conn, CALL_INSERT_INTO_DICTIONARY);
                        executeQuery(conn, CALL_INSERT_INTO_TS_KV);
                        executeQuery(conn, CALL_CREATE_NEW_TS_KV_LATEST_TABLE);
                        executeQuery(conn, CALL_INSERT_INTO_TS_KV_LATEST);

                        executeQuery(conn, DROP_TABLE_TS_KV_OLD);
                        executeQuery(conn, DROP_TABLE_TS_KV_LATEST_OLD);

                        executeQuery(conn, DROP_PROCEDURE_CREATE_PARTITION_TS_KV_TABLE);
                        executeQuery(conn, DROP_PROCEDURE_CREATE_PARTITIONS);
                        executeQuery(conn, DROP_PROCEDURE_CREATE_TS_KV_DICTIONARY_TABLE);
                        executeQuery(conn, DROP_PROCEDURE_INSERT_INTO_DICTIONARY);
                        executeQuery(conn, DROP_PROCEDURE_INSERT_INTO_TS_KV);
                        executeQuery(conn, DROP_PROCEDURE_CREATE_NEW_TS_KV_LATEST_TABLE);
                        executeQuery(conn, DROP_PROCEDURE_INSERT_INTO_TS_KV_LATEST);

                        executeQuery(conn, "ALTER TABLE ts_kv ADD COLUMN json_v json;");
                        executeQuery(conn, "ALTER TABLE ts_kv_latest ADD COLUMN json_v json;");

                        log.info("schema timeseries updated!");
                    }
                }
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    protected void loadSql(Connection conn) {
        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.4.3", LOAD_FUNCTIONS_SQL);
        try {
            loadFunctions(schemaUpdateFile, conn);
            log.info("Upgrade functions successfully loaded!");
        } catch (Exception e) {
            log.info("Failed to load PostgreSQL upgrade functions due to: {}", e.getMessage());
        }
    }
}