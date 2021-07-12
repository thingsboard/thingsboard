/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@TbCoreComponent
@Slf4j
@Service
public class EdgeEventsCleanUpService extends AbstractCleanUpService {

    @Value("${sql.ttl.edge_events.edge_events_ttl}")
    private long ttl;

    @Value("${sql.ttl.edge_events.enabled}")
    private boolean ttlTaskExecutionEnabled;

    private final EdgeEventService edgeEventService;

    public EdgeEventsCleanUpService(PartitionService partitionService, EdgeEventService edgeEventService) {
        super(partitionService);
        this.edgeEventService = edgeEventService;
    }

    @Scheduled(initialDelayString = "${sql.ttl.edge_events.execution_interval_ms}", fixedDelayString = "${sql.ttl.edge_events.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            edgeEventService.cleanupEvents(ttl);
        }
    }

}
