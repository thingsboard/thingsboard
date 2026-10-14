// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
@ConditionalOnExpression("${sql.ttl.agent_app_events.enabled:true} && ${sql.ttl.agent_app_events.ttl:0} > 0")
public class AgentAppEventCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.agent_app_events.execution_interval_ms:86400000})}";

    @Value("${sql.ttl.agent_app_events.ttl:604800}")
    private long ttlInSec;
    @Value("${sql.ttl.agent_app_events.removal_batch_size:10000}")
    private int removalBatchSize;

    private final AgentAppEventService agentAppEventService;

    public AgentAppEventCleanUpService(PartitionService partitionService, AgentAppEventService agentAppEventService) {
        super(partitionService);
        this.agentAppEventService = agentAppEventService;
    }

    @Scheduled(initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION,
            fixedDelayString = "${sql.ttl.agent_app_events.execution_interval_ms:86400000}")
    public void cleanUp() {
        if (!isSystemTenantPartitionMine()) {
            return;
        }
        long expirationTs = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        int removed = agentAppEventService.cleanUpExpiredEvents(expirationTs, removalBatchSize);
        if (removed > 0) {
            log.info("Cleaned up {} agent app events older than {}", removed, expirationTs);
        }
    }
}
