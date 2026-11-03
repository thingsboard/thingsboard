// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.NoSqlAnyDaoNonCloud;

/*
* Create keyspace for Cassandra NoSQL database for non-cloud deployment.
* For cloud service like Astra DBaas admin have to create keyspace manually on cloud UI.
* Then create tokens with database admin role and put it on Thingsboard parameters.
* Without this service cloud DB will end up with exception like
* UnauthorizedException: Missing correct permission on thingsboard
* */
@Service
@NoSqlAnyDaoNonCloud
@Profile("install")
public class CassandraKeyspaceService extends CassandraAbstractDatabaseSchemaService
        implements NoSqlKeyspaceService {
    public CassandraKeyspaceService() {
        super("schema-keyspace.cql");
    }
}
