// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.gen.agent.v1.FinalizeClaim;
import org.thingsboard.server.gen.agent.v1.FinalizeClaimResult;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventWatchdog;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentFinalizeClaimService {

    private static final int MAX_PUSH_ATTEMPTS = 3;

    @Value("${agents.event.retry_send_delay_ms:1000}")
    private long retrySendDelayMs;

    private final AgentAppEventService appEventService;
    @Lazy
    private final AgentRpcService agentRpcService;
    private final AgentEventWatchdog eventWatchdog;

    public void onFinalizeClaim(TenantId tenantId, AgentId agentId, FinalizeClaim claim) {
        AgentAppEventId eventId = new AgentAppEventId(new UUID(claim.getCommandId().getIdMSB(), claim.getCommandId().getIdLSB()));
        String containerId = claim.getContainerId();
        boolean granted = evaluateClaim(tenantId, agentId, eventId, containerId);
        log.info("[{}][{}] Finalize claim for event {} by container {}: {}",
                tenantId, agentId, eventId, containerId, granted ? "GRANTED" : "DENIED");
        ServerToAgent msg = ServerToAgent.newBuilder()
                .setFinalizeClaimResult(FinalizeClaimResult.newBuilder()
                        .setCommandId(claim.getCommandId())
                        .setGranted(granted)
                        .setContainerId(containerId)
                        .build())
                .build();
        pushClaimResult(tenantId, agentId, eventId, msg, 0);
    }

    private void pushClaimResult(TenantId tenantId, AgentId agentId, AgentAppEventId eventId, ServerToAgent msg, int attempt) {
        try {
            if (agentRpcService.push(agentId, msg)) {
                return;
            }
            // The verdict is already committed in the DB, so a dropped result leaves the container blocked
            // until the deadline sweeper picks the event up.
            if (attempt >= MAX_PUSH_ATTEMPTS) {
                log.warn("[{}][{}] Could not deliver finalize claim result for event {}; retry limit reached: {}",
                        tenantId, agentId, eventId, MAX_PUSH_ATTEMPTS);
                return;
            }
            log.warn("[{}][{}] Failed to push finalize claim result for event {} (backpressure), scheduling retry",
                    tenantId, agentId, eventId);
            eventWatchdog.getScheduler().schedule(
                    () -> pushClaimResult(tenantId, agentId, eventId, msg, attempt + 1),
                    retrySendDelayMs, TimeUnit.MILLISECONDS);
        } catch (AgentSessionNotFoundException e) {
            log.trace("[{}] No session to deliver finalize claim result", agentId);
        }
    }

    private boolean evaluateClaim(TenantId tenantId, AgentId agentId, AgentAppEventId eventId, String containerId) {
        if (StringUtils.isEmpty(containerId)) {
            return false;
        }
        AgentAppEvent event = appEventService.findById(tenantId, eventId);
        if (event == null || event.getActionType() == null ||
                !event.getActionType().isAgentScoped() || !agentId.equals(event.getAgentId())) {
            return false;
        }
        // Re-grant to the current winner is unconditional — a restarted winner must always be able to finish.
        if (containerId.equals(event.getWinnerContainerId())) {
            return appEventService.claimFinalizeWinner(eventId, containerId);
        }
        // Single-shot: the winner is never re-assigned.
        if (event.getWinnerContainerId() == null) {
            return setNewFinalizeWinner(eventId, containerId, event);
        }
        return false;
    }

    private boolean setNewFinalizeWinner(AgentAppEventId eventId, String containerId, AgentAppEvent event) {
        if (event.getProcessingStatus() != null && event.getProcessingStatus().isTerminated()) {
            return false;
        }
        Map<String, String> meta = event.getContextMetadata();
        String newContainerId = meta != null ? meta.get(AgentUpgradeKeys.NEW_CONTAINER_ID) : null;
        String oldContainerId = meta != null ? meta.get(AgentUpgradeKeys.OLD_CONTAINER_ID) : null;
        boolean beforeDeadline = event.getFinalizeDeadlineTs() == null
                || System.currentTimeMillis() < event.getFinalizeDeadlineTs();
        String eligible = beforeDeadline ? newContainerId : oldContainerId;
        return containerId.equals(eligible) && appEventService.claimFinalizeWinner(eventId, containerId);
    }
}
