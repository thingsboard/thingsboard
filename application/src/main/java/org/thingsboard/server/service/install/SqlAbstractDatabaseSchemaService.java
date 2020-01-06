/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

@Slf4j
public abstract class SqlAbstractDatabaseSchemaService implements DatabaseSchemaService {

    private static final String SQL_DIR = "sql";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private InstallScripts installScripts;

    private final String schemaSql;
    private final String schemaIdxSql;

    protected SqlAbstractDatabaseSchemaService(String schemaSql, String schemaIdxSql) {
        this.schemaSql = schemaSql;
        this.schemaIdxSql = schemaIdxSql;
    }

    @Override
    public void createDatabaseSchema() throws Exception {

        log.info("Installing SQL DataBase schema part: " + schemaSql);

        Path schemaFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaSql);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            String sql = new String(Files.readAllBytes(schemaFile), Charset.forName("UTF-8"));
            conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial thingsboard database schema
        }

        if (schemaIdxSql != null) {
            log.info("Installing SQL DataBase schema indexes part: " + schemaIdxSql);

            Path schemaIdxFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaIdxSql);
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                String sql = new String(Files.readAllBytes(schemaIdxFile), Charset.forName("UTF-8"));
                conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial thingsboard database schema
            }
        }
    }

}
