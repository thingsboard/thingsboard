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
