// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;
import org.thingsboard.server.dao.sqlts.insert.sql.DedicatedEventsSqlPartitioningRepository;

import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_JDBC_TEMPLATE;
import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_TRANSACTION_TEMPLATE;

@DedicatedEventsDataSource
@Repository
public class DedicatedTraceInsertRepository extends TraceInsertRepository {

    public DedicatedTraceInsertRepository(@Qualifier(EVENTS_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate,
                                          @Qualifier(EVENTS_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate,
                                          DedicatedEventsSqlPartitioningRepository partitioningRepository) {
        super(jdbcTemplate, transactionTemplate, partitioningRepository);
    }

}
