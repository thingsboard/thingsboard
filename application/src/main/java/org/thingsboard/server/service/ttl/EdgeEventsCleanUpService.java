/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TABLE_NAME;

@TbCoreComponent
@Slf4j
@Service
@ConditionalOnExpression("${edges.enabled:true} && ${sql.ttl.edge_events.edge_events_ttl:0} > 0")
public class EdgeEventsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.edge_events.execution_interval_ms})}";

    @Value("${sql.ttl.edge_events.edge_events_ttl}")
    private long ttl;

    @Value("${sql.edge_events.partition_size:168}")
    private int partitionSizeInHours;

    @Value("${sql.ttl.edge_events.enabled:true}")
    private boolean ttlTaskExecutionEnabled;

    private final EdgeEventService edgeEventService;

    private final SqlPartitioningRepository partitioningRepository;

    public EdgeEventsCleanUpService(PartitionService partitionService, EdgeEventService edgeEventService, SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.edgeEventService = edgeEventService;
        this.partitioningRepository = partitioningRepository;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.edge_events.execution_interval_ms}")
    public void cleanUp() {
        long edgeEventsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttl);
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            edgeEventService.cleanupEvents(edgeEventsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(EDGE_EVENT_TABLE_NAME, edgeEventsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
