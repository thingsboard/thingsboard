// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

@Service
@TimescaleDBTsDao
@Profile("install")
@Slf4j
public class TimescaleTsDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements TsDatabaseSchemaService {

    @Value("${sql.timescale.chunk_time_interval:86400000}")
    private long chunkTimeInterval;

    public TimescaleTsDatabaseSchemaService() {
        super("schema-timescale.sql", null);
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        super.createDatabaseSchema();
        executeQuery("SELECT create_hypertable('ts_kv', 'ts', chunk_time_interval => " + chunkTimeInterval + ", if_not_exists => true);");
    }

}