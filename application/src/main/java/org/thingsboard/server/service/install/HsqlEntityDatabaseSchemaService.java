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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.HsqlDao;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

@Service
@Slf4j
@HsqlDao
@Profile("install")
public class HsqlEntityDatabaseSchemaService extends SqlAbstractDatabaseSchemaService
        implements EntityDatabaseSchemaService {
    protected HsqlEntityDatabaseSchemaService() {
        super("schema-entities-hsql.sql", "schema-entities-idx.sql");
    }

    private final String schemaTypesSql = "schema-types-hsql.sql";

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {

        log.info("Installing SQL DataBase types part: " + schemaTypesSql);

        Path schemaFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaTypesSql);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            String sql = new String(Files.readAllBytes(schemaFile), Charset.forName("UTF-8"));
            conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial thingsboard database schema
        }

        super.createDatabaseSchema(createIndexes);
    }
}

