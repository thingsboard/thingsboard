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
package org.thingsboard.server.dao.sqlts.insert.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;
import org.thingsboard.server.dao.timeseries.SqlPartition;

import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_JDBC_TEMPLATE;
import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_TRANSACTION_MANAGER;

@DedicatedEventsDataSource
@Repository
public class DedicatedEventsSqlPartitioningRepository extends SqlPartitioningRepository {

    @Autowired
    @Qualifier(EVENTS_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.NOT_SUPPORTED, transactionManager = EVENTS_TRANSACTION_MANAGER)
    @Override
    public void save(SqlPartition partition) {
        super.save(partition);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, transactionManager = EVENTS_TRANSACTION_MANAGER)
    @Override
    public void createPartitionIfNotExists(String table, long entityTs, long partitionDurationMs) {
        super.createPartitionIfNotExists(table, entityTs, partitionDurationMs);
    }

    @Override
    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
