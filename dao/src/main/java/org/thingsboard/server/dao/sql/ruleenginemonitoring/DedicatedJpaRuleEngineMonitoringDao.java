// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;
import org.thingsboard.server.dao.util.SqlDao;

@DedicatedEventsDataSource
@Component
@SqlDao
public class DedicatedJpaRuleEngineMonitoringDao extends JpaRuleEngineMonitoringDao {

    public DedicatedJpaRuleEngineMonitoringDao(DedicatedRuleEngineStatsInsertRepository statsInsertRepository,
                                               DedicatedRuleEngineStatsQueryRepository statsQueryRepository,
                                               RuleEngineMonitoringLookupRepository lookupRepository) {
        super(statsInsertRepository, statsQueryRepository, lookupRepository);
    }

}
