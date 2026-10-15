// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.NoSqlTsDao;

@Service
@NoSqlTsDao
@Profile("install")
public class CassandraTsDatabaseSchemaService extends CassandraAbstractDatabaseSchemaService
        implements TsDatabaseSchemaService {
    public CassandraTsDatabaseSchemaService() {
        super("schema-ts.cql");
    }
}
