// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class AgentEventSourcingListener {

    private final TbClusterService tbClusterService;
    private final AgentApplicationService agentApplicationService;

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        if (!(event.getEntity() instanceof AgentAppEvent agentAppEvent) ||
                (agentAppEvent.getProcessingStatus() != null && agentAppEvent.getProcessingStatus() != AgentProcessingStatus.PENDING) ||
                !event.getCreated()) {
            return;
        }
        try {
            log.trace("[{}] AgentAppEvent SaveEntityEvent: {}", event.getTenantId(), agentAppEvent);
            if (agentAppEvent.getActionType() != null && agentAppEvent.getActionType().isAgentScoped()) {
                tbClusterService.onAgentAppEvent(
                        agentAppEvent.getTenantId(),
                        agentAppEvent.getAgentId(),
                        agentAppEvent);
                return;
            }
            var application = agentApplicationService.findById(
                    agentAppEvent.getTenantId(),
                    agentAppEvent.getApplicationId());
            if (application != null) {
                tbClusterService.onAgentAppEvent(
                        agentAppEvent.getTenantId(),
                        application.getAgentId(),
                        agentAppEvent);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process AgentAppEvent SaveEntityEvent: {}", event.getTenantId(), event, e);
        }
    }
}
