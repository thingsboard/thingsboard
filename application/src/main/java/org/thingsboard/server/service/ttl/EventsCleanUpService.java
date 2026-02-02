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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EventsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.events.execution_interval_ms})}";

    @Value("${sql.ttl.events.events_ttl}")
    private long ttlInSec;

    @Value("${sql.ttl.events.debug_events_ttl}")
    private long debugTtlInSec;

    @Value("${sql.ttl.events.enabled}")
    private boolean ttlTaskExecutionEnabled;

    private final EventService eventService;

    public EventsCleanUpService(PartitionService partitionService, EventService eventService) {
        super(partitionService);
        this.eventService = eventService;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION, fixedDelayString = "${sql.ttl.events.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled) {
            long ts = System.currentTimeMillis();
            long regularEventExpTs = ttlInSec > 0 ? ts - TimeUnit.SECONDS.toMillis(ttlInSec) : 0;
            long debugEventExpTs = debugTtlInSec > 0 ? ts - TimeUnit.SECONDS.toMillis(debugTtlInSec) : 0;
            eventService.cleanupEvents(regularEventExpTs, debugEventExpTs, isSystemTenantPartitionMine());
        }
    }

}