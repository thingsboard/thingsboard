// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cassandra;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.util.NoSqlAnyDao;

@Component("CassandraInstallCluster")
@NoSqlAnyDao
@Profile("install")
public class CassandraInstallCluster extends AbstractCassandraCluster {

    @PostConstruct
    public void init() {
        super.init(null);
    }

}
