// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.event;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sqlts.insert.sql.DedicatedEventsSqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

@DedicatedEventsDataSource
@Component
@SqlDao
public class DedicatedJpaEventDao extends JpaBaseEventDao {

    public DedicatedJpaEventDao(EventPartitionConfiguration partitionConfiguration,
                                DedicatedEventsSqlPartitioningRepository partitioningRepository,
                                LifecycleEventRepository lcEventRepository,
                                StatisticsEventRepository statsEventRepository,
                                ErrorEventRepository errorEventRepository,
                                DedicatedEventInsertRepository eventInsertRepository,
                                RuleNodeDebugEventRepository ruleNodeDebugEventRepository,
                                RuleChainDebugEventRepository ruleChainDebugEventRepository,
                                ScheduledLogExecutorComponent logExecutor,
                                StatsFactory statsFactory,
                                CalculatedFieldDebugEventRepository cfDebugEventRepository) {
        super(partitionConfiguration, partitioningRepository, lcEventRepository, statsEventRepository,
                errorEventRepository, eventInsertRepository, ruleNodeDebugEventRepository,
                ruleChainDebugEventRepository, logExecutor, statsFactory, cfDebugEventRepository);
    }

}
