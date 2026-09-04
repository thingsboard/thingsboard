// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.dao.cassandra.CassandraInstallCluster;
import org.thingsboard.server.service.install.cql.CQLStatementsParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public abstract class CassandraAbstractDatabaseSchemaService implements DatabaseSchemaService {

    private static final String CASSANDRA_DIR = "cassandra";
    private static final String CASSANDRA_STANDARD_KEYSPACE = "thingsboard";

    @Autowired
    @Qualifier("CassandraInstallCluster")
    private CassandraInstallCluster cluster;

    @Autowired
    private InstallScripts installScripts;

    @Value("${cassandra.keyspace_name}")
    private String keyspaceName;

    private final String schemaCql;

    protected CassandraAbstractDatabaseSchemaService(String schemaCql) {
        this.schemaCql = schemaCql;
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("Installing Cassandra DataBase schema part: " + schemaCql);
        Path schemaFile = Paths.get(installScripts.getDataDir(), CASSANDRA_DIR, schemaCql);
        loadCql(schemaFile);
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
    }

    private void loadCql(Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> cluster.getSession().execute(getCassandraKeyspaceName(statement)));
    }

    private String getCassandraKeyspaceName(String statement) {
        return statement.replaceFirst(CASSANDRA_STANDARD_KEYSPACE, keyspaceName);
    }
}
