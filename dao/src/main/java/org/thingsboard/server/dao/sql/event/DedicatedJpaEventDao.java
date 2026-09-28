// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
                                RawEventRepository rawEventRepository,
                                IntegrationDebugEventRepository integrationDebugEventRepository,
                                ConverterDebugEventRepository converterDebugEventRepository,
                                ScheduledLogExecutorComponent logExecutor,
                                StatsFactory statsFactory,
                                CalculatedFieldDebugEventRepository cfDebugEventRepository) {
        super(partitionConfiguration, partitioningRepository, lcEventRepository, statsEventRepository,
                errorEventRepository, eventInsertRepository, ruleNodeDebugEventRepository,
                ruleChainDebugEventRepository, rawEventRepository, integrationDebugEventRepository,
                converterDebugEventRepository, cfDebugEventRepository, logExecutor, statsFactory);
    }

}
