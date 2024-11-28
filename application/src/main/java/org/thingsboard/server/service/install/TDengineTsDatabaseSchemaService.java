/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.taosdata.jdbc.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.TDengineTsDao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * for tdengine
 */
@Service
@TDengineTsDao
@Profile("install")
@Slf4j
public class TDengineTsDatabaseSchemaService implements TsDatabaseSchemaService {

    protected static final String SQL_DIR = "sql";
    @Value("${tdengine.url}")
    protected String url;
    @Value("${tdengine.username}")
    protected String userName;
    @Value("${tdengine.password}")
    protected String password;
    @Autowired
    protected InstallScripts installScripts;
    private static final String SCHEMA_SQL = "schema-tdengine.sql";

    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("Installing SQL DataBase schema part: " + SCHEMA_SQL);
        Path schemaIdxFile = Paths.get(installScripts.getDataDir(), SQL_DIR, SCHEMA_SQL);
        String sql = Files.readString(schemaIdxFile);
        if (!StringUtils.isEmpty(sql)) {
            try (Connection conn = DriverManager.getConnection(url, userName, password)) {
                conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial thingsboard database schema
            }
        }
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
    }
}