// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.NoSqlTsLatestDao;

@Service
@NoSqlTsLatestDao
@Profile("install")
public class CassandraTsLatestDatabaseSchemaService extends CassandraAbstractDatabaseSchemaService
        implements TsLatestDatabaseSchemaService {
    public CassandraTsLatestDatabaseSchemaService() {
        super("schema-ts-latest.cql");
    }
}
