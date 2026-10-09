// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;

import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_JDBC_TEMPLATE;

@DedicatedEventsDataSource
@Repository
public class DedicatedRuleEngineStatsInsertRepository extends RuleEngineStatsInsertRepository {

    public DedicatedRuleEngineStatsInsertRepository(@Qualifier(EVENTS_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

}
