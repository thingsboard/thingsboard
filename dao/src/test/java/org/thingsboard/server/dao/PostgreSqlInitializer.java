/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class PostgreSqlInitializer {

    private static final List<String> sqlFiles = List.of(
            "sql/schema-ts-psql.sql",
            "sql/schema-entities.sql",
            "sql/schema-entities-idx.sql",
            "sql/schema-entities-idx-psql-addon.sql",
            "sql/schema-views.sql",
            "sql/schema-functions.sql",
            "sql/system-data.sql",
            "sql/system-test-psql.sql");
    private static final String dropAllTablesSqlFile = "sql/psql/drop-all-tables.sql";

    public static void initDb(Connection conn) {
        cleanUpDb(conn);
        log.info("initialize Postgres DB...");
        try {
            for (String sqlFile : sqlFiles) {
                URL sqlFileUrl = Resources.getResource(sqlFile);
                String sql = Resources.toString(sqlFileUrl, Charsets.UTF_8);
                conn.createStatement().execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to init the Postgres database. Reason: " + e.getMessage(), e);
        }
        log.info("Postgres DB is initialized!");
    }

    private static void cleanUpDb(Connection conn) {
        log.info("clean up Postgres DB...");
        try {
            URL dropAllTableSqlFileUrl = Resources.getResource(dropAllTablesSqlFile);
            String dropAllTablesSql = Resources.toString(dropAllTableSqlFileUrl, Charsets.UTF_8);
            conn.createStatement().execute(dropAllTablesSql);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to clean up the Postgres database. Reason: " + e.getMessage(), e);
        }
    }

}
