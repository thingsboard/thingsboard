// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.transport.TransportProtos.AgentAppEventNotificationProto;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.service.agent.AgentAppArgumentResolver;
import org.thingsboard.server.service.agent.AgentMsgConstructorUtils;
import org.thingsboard.server.service.agent.AgentRpcService;
import org.thingsboard.server.service.agent.AgentSessionNotFoundException;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultAgentEventProcessor implements AgentEventProcessor {

    private static final int TRY_SEND_MAX_ATTEMPTS = 3;

    @Value("${agents.event.retry_send_delay_ms:1000}")
    private long retrySendDelayMs;

    @Lazy
    private final AgentRpcService agentRpcService;
    private final AgentAppEventService appEventService;
    private final AgentApplicationService appService;
    private final AgentEventWatchdog eventWatchdog;
    private final AgentAppEventStepsResolver eventStepsResolver;
    private final AgentEventErrorHandler eventErrorHandler;
    private final AgentSessionRegistry sessions;
    private final AgentAppArgumentResolver argumentResolver;
    private final AgentScopedEventProcessor agentScopedProcessor;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void onEventNotification(AgentAppEventNotificationProto notification) {
        AgentId agentId = AgentId.fromMsbAndLsb(notification.getAgentIdMSB(), notification.getAgentIdLSB());
        TenantId tenantId = TenantId.fromUUID(new UUID(notification.getTenantIdMSB(), notification.getTenantIdLSB()));
        AgentApplicationId applicationId = AgentApplicationId.fromMsbAndLsb(notification.getApplicationIdMSB(), notification.getApplicationIdLSB());

        if (notification.getCancelled()) {
            AgentAppEventId eventId = new AgentAppEventId(new UUID(notification.getEventIdMSB(), notification.getEventIdLSB()));
            log.trace("[{}][{}] Processing cancel notification for event {}", tenantId, agentId, eventId);
            eventErrorHandler.onFailure(tenantId, agentId, eventId, ErrorOrigin.SERVER, "Processing cancel notification");
            return;
        }
        if (!sessions.hasSession(agentId)) {
            log.trace("[{}] No local session for agent, skipping notification", agentId);
            return;
        }
        if (isAgentScoped(notification.getActionType())) {
            agentScopedProcessor.resumeOrDispatch(tenantId, agentId);
            return;
        }
        if (appEventService.hasActiveOrPendingAgentEvent(agentId)) {
            log.trace("[{}] Agent-scoped event in progress, skipping app event notification", agentId);
            return;
        }
        AgentApplication application = appService.findById(tenantId, applicationId);
        if (application == null) {
            log.warn("[{}] Application not found for event {}", tenantId, applicationId);
            return;
        }

        log.trace("[{}][{}] Processing agent app event notification for application {}", tenantId, agentId, application);
        if (notification.getDelivered()) {
            resumeEventOrProcessNext(tenantId, agentId, application);
            return;
        }
        processNextEventForApp(tenantId, agentId, application);
    }

    @Override
    public void onError(TenantId tenantId, AgentId agentId, AgentAppEventId eventId, String errorMsg) {
        eventErrorHandler.onFailure(tenantId, agentId, eventId, ErrorOrigin.SERVER, errorMsg);
    }

    @Override
    public void resumeEventsOnReconnect(TenantId tenantId, AgentId agentId) {
        log.trace("[{}] Resuming events on reconnect for agent", agentId);
        if (appEventService.hasActiveOrPendingAgentEvent(agentId)) {
            agentScopedProcessor.resumeOrDispatch(tenantId, agentId);
            return;
        }
        dispatchAllQueuedAppEvents(tenantId, agentId);
    }

    @Override
    public void dispatchAllQueuedAppEvents(TenantId tenantId, AgentId agentId) {
        if (appEventService.hasActiveOrPendingAgentEvent(agentId)) {
            log.trace("[{}][{}] Agent-scoped event in progress, skipping app event dispatch", tenantId, agentId);
            return;
        }
        for (AgentApplicationId applicationId : appEventService.findApplicationIdsWithOutstandingEvents(agentId)) {
            try {
                AgentApplication app = appService.findById(tenantId, applicationId);
                if (app == null) {
                    log.trace("[{}][{}] Application {} with outstanding events no longer exists", tenantId, agentId, applicationId);
                    continue;
                }
                resumeEventOrProcessNext(tenantId, agentId, app);
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to resume events on agent reconnection for application {}", tenantId, agentId, applicationId, e);
            }
        }
    }

    /**
     * Callers must already have established that the agent has no active or pending agent-scoped event.
     */
    private void resumeEventOrProcessNext(TenantId tenantId, AgentId agentId, AgentApplication app) {
        log.trace("[{}][{}] Checking in-flight events for application {}", tenantId, agentId, app.getId());
        boolean resumed = resumeInFlightEvent(tenantId, agentId, app);
        if (!resumed) {
            // resumeInFlightEvent proved there is no active DELIVERED event for this application,
            // which is exactly what hasActiveEventForApplication would re-evaluate.
            log.trace("[{}][{}] No in-flight event found, dispatching next event for application {}", tenantId, agentId, app.getId());
            dispatchOldestPendingEvent(tenantId, agentId, app);
        }
    }

    @Override
    public void processNextEventForApp(TenantId tenantId, AgentId agentId, AgentApplication application) {
        log.trace("[{}][{}] Processing next agent app event notification for application {}", tenantId, agentId, application.getId());
        dispatchNextEventIfPossible(tenantId, agentId, application);
    }

    @Override
    public void processNextStepOrFinish(TenantId tenantId, AgentId agentId, AgentAppEvent event) {
        log.trace("[{}][{}] Processing next step or finish for event {}, currentStepId: {}", tenantId, agentId, event.getId(), event.getCurrentStepId());
        if (event.getActionType() != null && event.getActionType().isAgentScoped()) {
            agentScopedProcessor.processNextStepOrFinish(tenantId, agentId, event);
            return;
        }
        try {
            if (event.getApplicationId() == null) {
                log.warn("[{}] Orphaned event {}, application already removed", tenantId, event.getId());
                updateWithStatus(event, AgentProcessingStatus.ERROR, event.getCurrentStepId());
                return;
            }
            AgentApplication application = appService.findById(tenantId, event.getApplicationId());
            if (application == null) {
                log.warn("[{}] Application not found for event {}", tenantId, event.getApplicationId());
                updateWithStatus(event, AgentProcessingStatus.ERROR, event.getCurrentStepId());
                return;
            }
            List<AgentAppStep> steps = resolveSteps(application, event.getActionType());
            Optional<AgentAppStep> nextStep = StepLinkedListUtils.getNextStep(event.getCurrentStepId(), steps);
            nextStep.ifPresentOrElse(
                    step -> sendStep(event, application, step, steps.size()),
                    () -> finishEvent(tenantId, agentId, event, application)
            );
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process next step for event {}, marking as ERROR",
                    tenantId, agentId, event.getId(), e);
            eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private void finishEvent(TenantId tenantId, AgentId agentId, AgentAppEvent event, AgentApplication application) {
        log.trace("[{}][{}] Finishing event {} with action type {}", tenantId, agentId, event.getId(), event.getActionType());
        boolean transitionedToFinished;
        try {
            // The FINISHED transition and its side effect share one transaction
            transitionedToFinished = Boolean.TRUE.equals(transactionTemplate.execute(
                    _ -> finishEventInTransaction(tenantId, agentId, event, application)));
        } finally {
            eventWatchdog.cancel(agentId, event.getId());
        }
        if (transitionedToFinished) {
            processNextEventForApp(tenantId, agentId, application);
        }
    }

    private boolean finishEventInTransaction(TenantId tenantId, AgentId agentId, AgentAppEvent event, AgentApplication application) {
        if (!updateWithStatus(event, AgentProcessingStatus.FINISHED, event.getCurrentStepId())) {
            log.info("[{}][{}] Event {} already in terminal state, skipping finish side effects", tenantId, agentId, event.getId());
            return false;
        }
        if (event.getActionType() == AgentAppEventActionType.DELETE) {
            log.trace("[{}][{}] Deleting application {} after DELETE event", tenantId, agentId, application.getId());
            appService.delete(tenantId, event.getApplicationId());
        } else if (event.getActionType() == AgentAppEventActionType.UPGRADE && application.getDesiredTemplateVersion() != null) {
            log.trace("[{}][{}] Promoting desiredTemplateId to templateId for application {}", tenantId, agentId, application.getId());
            appService.promoteDesiredTemplate(tenantId, application.getId());
        } else if (event.getActionType() == AgentAppEventActionType.ROLLBACK && application.getDesiredTemplateVersion() != null) {
            log.trace("[{}][{}] Clearing desiredTemplateId after rollback for application {}", tenantId, agentId, application.getId());
            application.setDesiredTemplateVersion(null);
            appService.save(tenantId, application);
        }
        return true;
    }

    private boolean resumeInFlightEvent(TenantId tenantId, AgentId agentId, AgentApplication application) {
        var inFlightEvent = appEventService.findActiveDeliveredByApplicationId(application.getId());
        inFlightEvent.ifPresent(event -> {
            log.info("[{}][{}] Resuming in-flight event {} for application {}", tenantId, agentId, event.getId(), application.getId());
            try {
                List<AgentAppStep> steps = resolveSteps(application, event.getActionType());
                AgentAppStep currentStep = resolveCurrentStep(steps, event);
                sendStep(event, application, currentStep, steps.size());
            } catch (Exception e) {
                log.error("[{}][{}] Failed to resume in-flight event {} for application {}, marking as ERROR",
                        tenantId, agentId, event.getId(), application.getId(), e);
                eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
            }
        });
        return inFlightEvent.isPresent();
    }

    private AgentAppStep resolveCurrentStep(List<AgentAppStep> steps, AgentAppEvent event) {
        return Optional.ofNullable(StepLinkedListUtils.findByStepId(steps, event.getCurrentStepId()))
                .orElseGet(() -> StepLinkedListUtils.findFirstStep(steps));
    }

    private void dispatchNextEventIfPossible(TenantId tenantId, AgentId agentId, AgentApplication application) {
        AgentApplicationId applicationId = application.getId();
        if (appEventService.hasActiveOrPendingAgentEvent(agentId)) {
            log.trace("[{}][{}] Agent-scoped event in progress, skipping app event dispatch", tenantId, agentId);
            return;
        }
        if (appEventService.hasActiveEventForApplication(applicationId)) {
            log.trace("[{}][{}] Active event exists for application, skipping", tenantId, applicationId);
            return;
        }
        dispatchOldestPendingEvent(tenantId, agentId, application);
    }

    private void dispatchOldestPendingEvent(TenantId tenantId, AgentId agentId, AgentApplication application) {
        AgentApplicationId applicationId = application.getId();
        appEventService.findOldestPendingByApplicationId(applicationId).ifPresentOrElse(
                pendingEvent -> dispatchNextEvent(tenantId, agentId, application, pendingEvent),
                () -> log.trace("[{}][{}] No pending events for application", tenantId, applicationId)
        );
    }

    private void dispatchNextEvent(TenantId tenantId, AgentId agentId,
                                   AgentApplication application, AgentAppEvent event) {
        log.trace("[{}][{}] Dispatching event {} with action type {} for application {}", tenantId, agentId, event.getId(), event.getActionType(), application.getId());
        if (!appEventService.markDelivered(event.getId())) {
            log.trace("[{}][{}] Failed to claim event {} (already claimed by another node or thread)", tenantId, agentId, event.getId());
            return;
        }
        try {
            resolveArgsAndDispatchFirstStep(tenantId, agentId, application, event);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to dispatch event {}, marking as ERROR", tenantId, agentId, event.getId(), e);
            eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private void resolveArgsAndDispatchFirstStep(TenantId tenantId, AgentId agentId, AgentApplication application, AgentAppEvent event) {
        List<AgentAppStep> steps = resolveSteps(application, event.getActionType());
        DonAsynchron.withCallback(
                findCustomArgumentsIfRequired(tenantId, application, steps),
                args -> dispatchFirstStep(tenantId, agentId, application, steps, args, event),
                t -> eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, t.getMessage()),
                MoreExecutors.directExecutor());
    }

    private void dispatchFirstStep(TenantId tenantId, AgentId agentId, AgentApplication application, List<AgentAppStep> steps,
                                   Map<String, String> resolvedArguments, AgentAppEvent event) {
        try {
            if (resolvedArguments != null && !resolvedArguments.isEmpty()) {
                appEventService.updateResolvedArguments(event.getId(), resolvedArguments);
                event.setResolvedArguments(resolvedArguments);
            }
            AgentAppStep firstStep = StepLinkedListUtils.findFirstStep(steps);
            log.trace("[{}][{}] Resolved {} steps for event {}, first step: {}", tenantId, agentId, steps.size(), event.getId(), firstStep.getId());
            sendStep(event, application, firstStep, steps.size());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to dispatch event {}, marking as ERROR", tenantId, agentId, event.getId(), e);
            eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private ListenableFuture<Map<String, String>> findCustomArgumentsIfRequired(
            TenantId tenantId, AgentApplication application, List<AgentAppStep> steps) {

        boolean findCustomArguments = steps.stream().anyMatch(s -> s.getType().containsCustomArguments());
        if (!findCustomArguments) {
            return Futures.immediateFuture(Collections.emptyMap());
        }
        return argumentResolver.resolve(tenantId, application);
    }

    private void sendStep(AgentAppEvent event, AgentApplication application, AgentAppStep step, int totalSteps) {
        try {
            log.trace("[{}][{}] Sending step {} for event {}", application.getTenantId(), application.getAgentId(), step.getId(), event.getId());
            ServerToAgent msg = AgentMsgConstructorUtils.buildAppCommand(event, application, step, totalSteps);
            if (!updateWithStatus(event, AgentProcessingStatus.PENDING, step.getId())) {
                log.info("[{}][{}] Event {} reached a terminal state concurrently, not sending step {}",
                        application.getTenantId(), application.getAgentId(), event.getId(), step.getId());
                eventWatchdog.cancel(application.getAgentId(), event.getId());
                return;
            }
            eventWatchdog.schedule(application, event, new DefaultAgentEventResender(totalSteps, event));
            trySend(application, event, msg);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to send step {} for event {}, marking as ERROR",
                    application.getTenantId(), application.getAgentId(), step.getId(), event.getId(), e);
            eventErrorHandler.onFailure(application.getTenantId(), application.getAgentId(), event.getId(), ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private void trySend(AgentApplication application, AgentAppEvent event, ServerToAgent msg) {
        int firstAttempt = 0;
        trySend(application, event, msg, firstAttempt);
    }

    private void trySend(AgentApplication application, AgentAppEvent event, ServerToAgent msg, int attempt) {
        try {
            if (attempt++ >= TRY_SEND_MAX_ATTEMPTS) {
                log.warn("[{}][{}] Couldn't send msg to agent; retry limit reached: {}",
                        application.getTenantId(), application.getAgentId(), TRY_SEND_MAX_ATTEMPTS);
                return;
            }
            log.trace("[{}][{}] Trying to send event {} to agent, attempt {}", application.getTenantId(), application.getAgentId(), event.getId(), attempt);
            final int nextAttempt = attempt;
            boolean pushed = agentRpcService.push(application.getAgentId(), msg);
            if (!pushed) {
                log.warn("[{}][{}] Failed to push to agent (backpressure), scheduling retry",
                        application.getTenantId(), application.getAgentId());
                eventWatchdog.getScheduler().schedule(
                        () -> trySend(application, event, msg, nextAttempt),
                        retrySendDelayMs, TimeUnit.MILLISECONDS);
            }
        } catch (AgentSessionNotFoundException e) {
            log.trace("[{}] No active session for agent. Will resend on reconnect", application.getAgentId());
            eventWatchdog.cancel(application.getAgentId(), event.getId());
        }
    }

    private static boolean isAgentScoped(String actionType) {
        try {
            return AgentAppEventActionType.valueOf(actionType).isAgentScoped();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private List<AgentAppStep> resolveSteps(AgentApplication application, AgentAppEventActionType actionType) {
        return eventStepsResolver.resolveSteps(application, actionType);
    }

    private boolean updateWithStatus(AgentAppEvent event, AgentProcessingStatus processingStatus, UUID stepId) {
        return appEventService.updateStatus(event.getId(), AgentAppEventStatusUpdate.builder()
                .processingStatus(processingStatus)
                .currentStepId(stepId)
                .build());
    }

    private class DefaultAgentEventResender implements AgentEventResender {
        private final int totalSteps;
        private final AgentAppEvent event;

        public DefaultAgentEventResender(int totalSteps, AgentAppEvent event) {
            this.totalSteps = totalSteps;
            this.event = event;
        }

        @Override
        public void resendCurrentStep(AgentAppEvent event, AgentApplication application, AgentAppStep step) {
            sendStep(event, application, step, totalSteps);
        }

        @Override
        public void onError(AgentAppEventId eventId, AgentApplication application) {
            eventErrorHandler.onFailure(application.getTenantId(), application.getAgentId(), eventId, ErrorOrigin.SERVER,
                    "Agent progress could not be verified: the watchdog staleness check kept failing on the server");
        }
    }
}
