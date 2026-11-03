// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.event;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;

import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_JDBC_TEMPLATE;
import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_TRANSACTION_TEMPLATE;

@DedicatedEventsDataSource
@Repository
public class DedicatedEventInsertRepository extends EventInsertRepository {

    public DedicatedEventInsertRepository(@Qualifier(EVENTS_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate,
                                          @Qualifier(EVENTS_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate) {
        super(jdbcTemplate, transactionTemplate);
    }

}
