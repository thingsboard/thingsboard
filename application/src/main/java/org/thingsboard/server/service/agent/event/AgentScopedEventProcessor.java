// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentMsgConstructorUtils;
import org.thingsboard.server.service.agent.AgentRpcService;
import org.thingsboard.server.service.agent.AgentSessionNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentScopedEventProcessor {

    private static final int TRY_SEND_MAX_ATTEMPTS = 3;

    @Value("${agents.event.retry_send_delay_ms:1000}")
    private long retrySendDelayMs;
    @Value("${agents.upgrade.finalize_timeout_ms:300000}")
    private long finalizeTimeoutMs;

    @Lazy
    private final AgentRpcService agentRpcService;
    @Lazy
    private final AgentEventProcessor agentEventProcessor;
    private final AgentAppEventService appEventService;
    // Only borrows the shared retry scheduler — agent-scoped events never arm a per-step watchdog
    // (watchdog futures die with the session, which dies by design mid-finalize; the deadline sweep
    // is the recovery mechanism instead).
    private final AgentEventWatchdog eventWatchdog;
    private final AgentAppEventStepsResolver eventStepsResolver;
    private final AgentEventErrorHandler eventErrorHandler;

    public void resumeOrDispatch(TenantId tenantId, AgentId agentId) {
        try {
            Optional<AgentAppEvent> inFlight = appEventService.findActiveDeliveredAgentEventByAgentId(agentId);
            if (inFlight.isPresent()) {
                resumeAgentEvent(tenantId, agentId, inFlight.get());
                return;
            }
            appEventService.findOldestPendingAgentEventByAgentId(agentId)
                    .ifPresent(pending -> dispatchAgentEvent(tenantId, agentId, pending));
        } catch (Exception e) {
            log.error("[{}] Failed to resume or dispatch agent-scoped event", agentId, e);
        }
    }

    public void processNextStepOrFinish(TenantId tenantId, AgentId agentId, AgentAppEvent event) {
        try {
            List<AgentAppStep> steps = eventStepsResolver.resolveAgentSteps(event.getActionType());
            Optional<AgentAppStep> nextStep = StepLinkedListUtils.getNextStep(event.getCurrentStepId(), steps);
            nextStep.ifPresentOrElse(
                    step -> sendAgentStep(tenantId, agentId, event, step, steps.size()),
                    () -> finishAgentEvent(tenantId, agentId, event)
            );
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process next agent-scoped step for event {}, marking as ERROR", tenantId, agentId, event.getId(), e);
            eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private void resumeAgentEvent(TenantId tenantId, AgentId agentId, AgentAppEvent event) {
        log.info("[{}][{}] Resuming in-flight agent-scoped event {}", tenantId, agentId, event.getId());
        try {
            List<AgentAppStep> steps = eventStepsResolver.resolveAgentSteps(event.getActionType());
            AgentAppStep currentStep = Optional.ofNullable(StepLinkedListUtils.findByStepId(steps, event.getCurrentStepId()))
                    .orElseGet(() -> StepLinkedListUtils.findFirstStep(steps));
            sendAgentStep(tenantId, agentId, event, currentStep, steps.size());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to resume in-flight agent-scoped event {}, marking as ERROR", tenantId, agentId, event.getId(), e);
            eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private void dispatchAgentEvent(TenantId tenantId, AgentId agentId, AgentAppEvent event) {
        log.trace("[{}][{}] Dispatching agent-scoped event {}", tenantId, agentId, event.getId());
        if (!appEventService.markDelivered(event.getId())) {
            log.trace("[{}][{}] Failed to claim agent-scoped event {} (already claimed)", tenantId, agentId, event.getId());
            return;
        }
        try {
            List<AgentAppStep> steps = eventStepsResolver.resolveAgentSteps(event.getActionType());
            sendAgentStep(tenantId, agentId, event, StepLinkedListUtils.findFirstStep(steps), steps.size());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to dispatch agent-scoped event {}, marking as ERROR", tenantId, agentId, event.getId(), e);
            eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private void sendAgentStep(TenantId tenantId, AgentId agentId, AgentAppEvent event, AgentAppStep step, int totalSteps) {
        try {
            if (step.getType() == AgentAppStepType.AGENT_FINALIZE) {
                ensureFinalizeDeadline(tenantId, event);
            }
            ServerToAgent msg = AgentMsgConstructorUtils.buildAgentCommand(event, step, totalSteps);
            updateWithStatus(event, AgentProcessingStatus.PENDING, step.getId());
            // No per-step watchdog for agent-scoped events: the session dies by design mid-finalize,
            // and stuck events are recovered by the session-independent deadline sweep.
            trySendToAgent(tenantId, agentId, event, msg, 0);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to send agent-scoped step {} for event {}, marking as ERROR",
                    tenantId, agentId, step.getId(), event.getId(), e);
            eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private void finishAgentEvent(TenantId tenantId, AgentId agentId, AgentAppEvent event) {
        log.trace("[{}][{}] Finishing agent-scoped event {}", tenantId, agentId, event.getId());
        boolean transitionedToFinished = updateWithStatus(event, AgentProcessingStatus.FINISHED, event.getCurrentStepId());
        if (!transitionedToFinished) {
            log.info("[{}][{}] Agent-scoped event {} already in terminal state, skipping finish side effects", tenantId, agentId, event.getId());
            return;
        }
        agentEventProcessor.dispatchAllQueuedAppEvents(tenantId, agentId);
    }

    private void ensureFinalizeDeadline(TenantId tenantId, AgentAppEvent event) {
        if (event.getFinalizeDeadlineTs() != null) {
            return;
        }
        long deadline = System.currentTimeMillis() + finalizeTimeoutMs;
        if (appEventService.updateFinalizeDeadlineIfAbsent(event.getId(), deadline)) {
            event.setFinalizeDeadlineTs(deadline);
        } else {
            AgentAppEvent fresh = appEventService.findById(tenantId, event.getId());
            event.setFinalizeDeadlineTs(fresh != null ? fresh.getFinalizeDeadlineTs() : deadline);
        }
    }

    private void trySendToAgent(TenantId tenantId, AgentId agentId, AgentAppEvent event, ServerToAgent msg, int attempt) {
        try {
            if (attempt++ >= TRY_SEND_MAX_ATTEMPTS) {
                log.warn("[{}][{}] Couldn't send msg to agent; retry limit reached: {}", tenantId, agentId, TRY_SEND_MAX_ATTEMPTS);
                return;
            }
            final int nextAttempt = attempt;
            boolean pushed = agentRpcService.push(agentId, msg);
            if (!pushed) {
                log.warn("[{}][{}] Failed to push to agent (backpressure), scheduling retry", tenantId, agentId);
                eventWatchdog.getScheduler().schedule(
                        () -> trySendToAgent(tenantId, agentId, event, msg, nextAttempt),
                        retrySendDelayMs, TimeUnit.MILLISECONDS);
            }
        } catch (AgentSessionNotFoundException e) {
            log.trace("[{}] No active session for agent. Will resend on reconnect", agentId);
        }
    }

    private boolean updateWithStatus(AgentAppEvent event, AgentProcessingStatus processingStatus, UUID stepId) {
        return appEventService.updateStatus(event.getId(), AgentAppEventStatusUpdate.builder()
                .processingStatus(processingStatus)
                .currentStepId(stepId)
                .build());
    }
}
