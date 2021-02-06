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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

@Service
@Profile("install")
@Slf4j
@TimescaleDBTsDao
@PsqlDao
public class TimescaleTsDatabaseUpgradeService extends AbstractSqlTsDatabaseUpgradeService implements DatabaseTsUpgradeService {

    @Value("${sql.timescale.chunk_time_interval:86400000}")
    private long chunkTimeInterval;

    private static final String LOAD_FUNCTIONS_SQL = "schema_update_timescale_ts.sql";
    private static final String LOAD_TTL_FUNCTIONS_SQL = "schema_update_ttl.sql";

    private static final String TENANT_TS_KV_OLD_TABLE = "tenant_ts_kv_old;";

    private static final String CREATE_TS_KV_LATEST_TABLE = "create_ts_kv_latest_table()";
    private static final String CREATE_NEW_TS_KV_TABLE = "create_new_ts_kv_table()";
    private static final String CREATE_TS_KV_DICTIONARY_TABLE = "create_ts_kv_dictionary_table()";
    private static final String INSERT_INTO_DICTIONARY = "insert_into_dictionary()";
    private static final String INSERT_INTO_TS_KV = "insert_into_ts_kv(IN path_to_file varchar)";
    private static final String INSERT_INTO_TS_KV_CURSOR = "insert_into_ts_kv_cursor()";
    private static final String INSERT_INTO_TS_KV_LATEST = "insert_into_ts_kv_latest()";

    private static final String CALL_CREATE_TS_KV_LATEST_TABLE = CALL_REGEX + CREATE_TS_KV_LATEST_TABLE;
    private static final String CALL_CREATE_NEW_TENANT_TS_KV_TABLE = CALL_REGEX + CREATE_NEW_TS_KV_TABLE;
    private static final String CALL_CREATE_TS_KV_DICTIONARY_TABLE = CALL_REGEX + CREATE_TS_KV_DICTIONARY_TABLE;
    private static final String CALL_INSERT_INTO_DICTIONARY = CALL_REGEX + INSERT_INTO_DICTIONARY;
    private static final String CALL_INSERT_INTO_TS_KV_LATEST = CALL_REGEX + INSERT_INTO_TS_KV_LATEST;
    private static final String CALL_INSERT_INTO_TS_KV_CURSOR = CALL_REGEX + INSERT_INTO_TS_KV_CURSOR;

    private static final String DROP_OLD_TENANT_TS_KV_TABLE = DROP_TABLE + TENANT_TS_KV_OLD_TABLE;

    private static final String DROP_PROCEDURE_CREATE_TS_KV_LATEST_TABLE = DROP_PROCEDURE_IF_EXISTS + CREATE_TS_KV_LATEST_TABLE;
    private static final String DROP_PROCEDURE_CREATE_TENANT_TS_KV_TABLE_COPY = DROP_PROCEDURE_IF_EXISTS + CREATE_NEW_TS_KV_TABLE;
    private static final String DROP_PROCEDURE_CREATE_TS_KV_DICTIONARY_TABLE = DROP_PROCEDURE_IF_EXISTS + CREATE_TS_KV_DICTIONARY_TABLE;
    private static final String DROP_PROCEDURE_INSERT_INTO_DICTIONARY = DROP_PROCEDURE_IF_EXISTS + INSERT_INTO_DICTIONARY;
    private static final String DROP_PROCEDURE_INSERT_INTO_TS_KV = DROP_PROCEDURE_IF_EXISTS + INSERT_INTO_TS_KV;
    private static final String DROP_PROCEDURE_INSERT_INTO_TS_KV_CURSOR = DROP_PROCEDURE_IF_EXISTS + INSERT_INTO_TS_KV_CURSOR;
    private static final String DROP_PROCEDURE_INSERT_INTO_TS_KV_LATEST = DROP_PROCEDURE_IF_EXISTS + INSERT_INTO_TS_KV_LATEST;

    @Autowired
    private InstallScripts installScripts;

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
                            log.info("Updating timescale schema ...");
                            executeQuery(conn, CALL_CREATE_TS_KV_LATEST_TABLE);
                            executeQuery(conn, CALL_CREATE_NEW_TENANT_TS_KV_TABLE);

                            executeQuery(conn, "SELECT create_hypertable('ts_kv', 'ts', chunk_time_interval => " + chunkTimeInterval + ", if_not_exists => true);");

                            executeQuery(conn, CALL_CREATE_TS_KV_DICTIONARY_TABLE);
                            executeQuery(conn, CALL_INSERT_INTO_DICTIONARY);

                            Path pathToTempTsKvFile = null;
                            if (SystemUtils.IS_OS_WINDOWS) {
                                Path pathToDir;
                                log.info("Lookup for environment variable: {} ...", THINGSBOARD_WINDOWS_UPGRADE_DIR);
                                String thingsboardWindowsUpgradeDir = System.getenv(THINGSBOARD_WINDOWS_UPGRADE_DIR);
                                if (StringUtils.isNotEmpty(thingsboardWindowsUpgradeDir)) {
                                    log.info("Environment variable: {} was found!", THINGSBOARD_WINDOWS_UPGRADE_DIR);
                                    pathToDir = Paths.get(thingsboardWindowsUpgradeDir);
                                } else {
                                    log.info("Failed to lookup environment variable: {}", THINGSBOARD_WINDOWS_UPGRADE_DIR);
                                    pathToDir = Paths.get(PATH_TO_USERS_PUBLIC_FOLDER);
                                }
                                log.info("Directory: {} will be used for creation temporary upgrade file!", pathToDir);
                                try {
                                    Path tsKvFile = Files.createTempFile(pathToDir, "ts_kv", ".sql");
                                    pathToTempTsKvFile = tsKvFile.toAbsolutePath();
                                    try {
                                        executeQuery(conn, "call insert_into_ts_kv('" + pathToTempTsKvFile + "')");
                                    } catch (Exception e) {
                                        insertTimeseries(conn);
                                    }
                                } catch (IOException | SecurityException e) {
                                    log.warn("Failed to create time-series upgrade files due to: {}", e.getMessage());
                                    insertTimeseries(conn);
                                }
                            } else {
                                try {
                                    Path tempDirPath = Files.createTempDirectory("ts_kv");
                                    File tempDirAsFile = tempDirPath.toFile();
                                    boolean writable = tempDirAsFile.setWritable(true, false);
                                    boolean readable = tempDirAsFile.setReadable(true, false);
                                    boolean executable = tempDirAsFile.setExecutable(true, false);
                                    pathToTempTsKvFile = tempDirPath.resolve(TS_KV_SQL).toAbsolutePath();
                                    try {
                                        if (writable && readable && executable) {
                                            executeQuery(conn, "call insert_into_ts_kv('" + pathToTempTsKvFile + "')");
                                        } else {
                                            throw new RuntimeException("Failed to grant write permissions for the: " + tempDirPath + "folder!");
                                        }
                                    } catch (Exception e) {
                                        insertTimeseries(conn);
                                    }
                                } catch (IOException | SecurityException e) {
                                    log.warn("Failed to create time-series upgrade files due to: {}", e.getMessage());
                                    insertTimeseries(conn);
                                }
                            }
                            removeUpgradeFile(pathToTempTsKvFile);

                            executeQuery(conn, CALL_INSERT_INTO_TS_KV_LATEST);

                            executeQuery(conn, DROP_OLD_TENANT_TS_KV_TABLE);

                            executeQuery(conn, DROP_PROCEDURE_CREATE_TS_KV_LATEST_TABLE);
                            executeQuery(conn, DROP_PROCEDURE_CREATE_TENANT_TS_KV_TABLE_COPY);
                            executeQuery(conn, DROP_PROCEDURE_CREATE_TS_KV_DICTIONARY_TABLE);
                            executeQuery(conn, DROP_PROCEDURE_INSERT_INTO_DICTIONARY);
                            executeQuery(conn, DROP_PROCEDURE_INSERT_INTO_TS_KV);
                            executeQuery(conn, DROP_PROCEDURE_INSERT_INTO_TS_KV_CURSOR);
                            executeQuery(conn, DROP_PROCEDURE_INSERT_INTO_TS_KV_LATEST);

                            executeQuery(conn, "ALTER TABLE ts_kv ADD COLUMN IF NOT EXISTS json_v json;");
                            executeQuery(conn, "ALTER TABLE ts_kv_latest ADD COLUMN IF NOT EXISTS json_v json;");
                        }

                        log.info("Load TTL functions ...");
                        loadSql(conn, LOAD_TTL_FUNCTIONS_SQL);

                        executeQuery(conn, "UPDATE tb_schema_settings SET schema_version = 2005000");
                        log.info("schema timescale updated!");
                    }
                }
                break;
            case "2.5.0":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    executeQuery(conn, "UPDATE tb_schema_settings SET schema_version = 2005001");
                }
                break;
            case "3.1.1":
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    private void insertTimeseries(Connection conn) {
        log.warn("Upgrade script failed using the copy to/from files strategy!" +
                " Trying to perfrom the upgrade using Inserts strategy ...");
        executeQuery(conn, CALL_INSERT_INTO_TS_KV_CURSOR);
    }

    private void removeUpgradeFile(Path pathToTempTsKvFile) {
        if (pathToTempTsKvFile != null && pathToTempTsKvFile.toFile().exists()) {
            boolean deleteTsKvFile = pathToTempTsKvFile.toFile().delete();
            if (deleteTsKvFile) {
                log.info("Successfully deleted the temp file for ts_kv table upgrade!");
            }
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
