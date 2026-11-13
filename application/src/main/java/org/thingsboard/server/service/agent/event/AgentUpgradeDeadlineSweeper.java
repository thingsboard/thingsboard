// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ttl.AbstractCleanUpService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
public class AgentUpgradeDeadlineSweeper extends AbstractCleanUpService {

    private static final int SWEEP_BATCH_SIZE = 100;
    private static final int MAX_SWEEP_BATCHES = 100;

    @Value("${agents.upgrade.sweep_interval_ms:60000}")
    private long sweepIntervalMs;
    @Value("${agents.upgrade.finalize_grace_ms:60000}")
    private long graceMs;
    @Value("${agents.upgrade.finalize_timeout_ms:300000}")
    private long finalizeTimeoutMs;

    private final AgentAppEventService appEventService;
    private final AgentEventErrorHandler eventErrorHandler;

    private ScheduledExecutorService scheduler;

    public AgentUpgradeDeadlineSweeper(PartitionService partitionService, AgentAppEventService appEventService,
                                       AgentEventErrorHandler eventErrorHandler) {
        super(partitionService);
        this.appEventService = appEventService;
        this.eventErrorHandler = eventErrorHandler;
    }

    @PostConstruct
    private void init() {
        scheduler = ThingsBoardExecutors.newScheduledThreadPool(1, "agent-upgrade-sweeper");
        scheduler.scheduleWithFixedDelay(this::sweep, sweepIntervalMs, sweepIntervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    private void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void sweep() {
        try {
            if (!isSystemTenantPartitionMine()) {
                return;
            }
            long now = System.currentTimeMillis();
            long deadline = now - graceMs;
            long stale = now - finalizeTimeoutMs - graceMs;
            PageLink pageLink = new PageLink(SWEEP_BATCH_SIZE);

            for (int batch = 0; batch < MAX_SWEEP_BATCHES; batch++) {
                PageData<AgentAppEvent> page = appEventService.findStuckAgentEvents(deadline, stale, pageLink);
                if (page.getData().isEmpty()) {
                    break;
                }
                for (AgentAppEvent event : page.getData()) {
                    log.warn("[{}][{}] Sweeping stuck agent-scoped event {}", event.getTenantId(), event.getAgentId(), event.getId());
                    eventErrorHandler.onFailure(event.getTenantId(), event.getAgentId(), event.getId(),
                            ErrorOrigin.SERVER, "Agent upgrade timed out");
                }
                if (!page.hasNext()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Agent upgrade deadline sweep failed", e);
        }
    }
}
