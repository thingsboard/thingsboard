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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${sql.postgres.ts_key_value_partitioning:MONTHS}")
    private String partitionType;

    private static final String LOAD_FUNCTIONS_SQL = "schema_update_psql_ts.sql";
    private static final String LOAD_TTL_FUNCTIONS_SQL = "schema_update_ttl.sql";
    private static final String LOAD_DROP_PARTITIONS_FUNCTIONS_SQL = "schema_update_psql_drop_partitions.sql";

    private static final String TS_KV_OLD = "ts_kv_old;";
    private static final String TS_KV_LATEST_OLD = "ts_kv_latest_old;";

    private static final String CREATE_PARTITION_TS_KV_TABLE = "create_partition_ts_kv_table()";
    private static final String CREATE_NEW_TS_KV_LATEST_TABLE = "create_new_ts_kv_latest_table()";
    private static final String CREATE_PARTITIONS = "create_partitions(IN partition_type varchar)";
    private static final String CREATE_TS_KV_DICTIONARY_TABLE = "create_ts_kv_dictionary_table()";
    private static final String INSERT_INTO_DICTIONARY = "insert_into_dictionary()";
    private static final String INSERT_INTO_TS_KV = "insert_into_ts_kv()";
    private static final String INSERT_INTO_TS_KV_LATEST = "insert_into_ts_kv_latest()";

    private static final String CALL_CREATE_PARTITION_TS_KV_TABLE = CALL_REGEX + CREATE_PARTITION_TS_KV_TABLE;
    private static final String CALL_CREATE_NEW_TS_KV_LATEST_TABLE = CALL_REGEX + CREATE_NEW_TS_KV_LATEST_TABLE;
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
    private static final String DROP_FUNCTION_GET_PARTITION_DATA = "DROP FUNCTION IF EXISTS get_partitions_data;";

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
                        if (isOldSchema(conn, 2004003)) {
                            log.info("Load upgrade functions ...");
                            loadSql(conn, LOAD_FUNCTIONS_SQL);
                            log.info("Updating timeseries schema ...");
                            executeQuery(conn, CALL_CREATE_PARTITION_TS_KV_TABLE);
                            if (!partitionType.equals("INDEFINITE")) {
                                executeQuery(conn, "call create_partitions('" + partitionType + "')");
                            } else {
                                executeQuery(conn, "CREATE TABLE IF NOT EXISTS ts_kv_indefinite PARTITION OF ts_kv DEFAULT;");
                            }
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
                            executeQuery(conn, DROP_FUNCTION_GET_PARTITION_DATA);

                            executeQuery(conn, "ALTER TABLE ts_kv ADD COLUMN IF NOT EXISTS json_v json;");
                            executeQuery(conn, "ALTER TABLE ts_kv_latest ADD COLUMN IF NOT EXISTS json_v json;");
                        } else {
                            executeQuery(conn, "ALTER TABLE ts_kv DROP CONSTRAINT IF EXISTS ts_kv_pkey;");
                            executeQuery(conn, "ALTER TABLE ts_kv ADD CONSTRAINT ts_kv_pkey PRIMARY KEY (entity_id, key, ts);");
                        }

                        log.info("Load TTL functions ...");
                        loadSql(conn, LOAD_TTL_FUNCTIONS_SQL);
                        log.info("Load Drop Partitions functions ...");
                        loadSql(conn, LOAD_DROP_PARTITIONS_FUNCTIONS_SQL);

                        executeQuery(conn, "UPDATE tb_schema_settings SET schema_version = 2005000");

                        log.info("schema timeseries updated!");
                    }
                }
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    @Override
    protected void loadSql(Connection conn, String fileName) {
        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.4.3", fileName);
        try {
            loadFunctions(schemaUpdateFile, conn);
            log.info("Functions successfully loaded!");
        } catch (Exception e) {
            log.info("Failed to load PostgreSQL upgrade functions due to: {}", e.getMessage());
        }
    }
}