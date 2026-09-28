// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.state.RollBackStepState;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentEventErrorHandler {

    private final AgentAppEventService appEventService;
    private final AgentApplicationService appService;
    private final AgentAppEventStepsResolver stepsResolver;
    private final AgentEventWatchdog eventWatchdog;
    @Lazy
    private final AgentEventProcessor agentEventProcessor;
    private final TransactionTemplate transactionTemplate;

    public void onFailure(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId, ErrorOrigin errorOrigin, String errorMsg) {
        boolean shouldDispatchNext = false;
        try {
            if (!transitionToError(failedEventId, errorMsg)) {
                log.info("[{}][{}] Event {} already in terminal state, skipping error side effects", tenantId, agentId, failedEventId);
                return;
            }
            shouldDispatchNext = applyErrorSideEffects(tenantId, agentId, failedEventId, errorOrigin);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to apply error side effects for event {}", tenantId, agentId, failedEventId, e);
            shouldDispatchNext = true; // The event is already ERROR, so the application queue would stall if nothing was dispatched.
        } finally {
            eventWatchdog.cancel(agentId, failedEventId);
        }
        if (shouldDispatchNext) {
            dispatchNext(tenantId, agentId, failedEventId);
        }
    }

    private boolean transitionToError(AgentAppEventId failedEventId, String errorMsg) {
        return appEventService.updateStatus(failedEventId, AgentAppEventStatusUpdate.builder()
                .processingStatus(AgentProcessingStatus.ERROR)
                .errorMessage(errorMsg)
                .build());
    }

    private void dispatchNext(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId) {
        try {
            AgentAppEvent failed = appEventService.findById(tenantId, failedEventId);
            if (isAgentScoped(failed)) {
                agentEventProcessor.dispatchAllQueuedAppEvents(tenantId, agentId);
            } else {
                dispatchNextIfAppExists(tenantId, agentId, failedEventId);
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to dispatch next event after error on event {}", tenantId, agentId, failedEventId, e);
        }
    }

    private boolean isAgentScoped(AgentAppEvent failed) {
        return failed != null && failed.getActionType() != null && failed.getActionType().isAgentScoped();
    }

    private boolean applyErrorSideEffects(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId, ErrorOrigin errorOrigin) {
        return Boolean.TRUE.equals(transactionTemplate.execute(_ -> {
            log.trace("[{}][{}] Processing after error for event {}", tenantId, agentId, failedEventId);
            boolean dispatchNextEvent = true;

            AgentAppEvent event = appEventService.findById(tenantId, failedEventId);
            if (isDelete(event)) {
                rollbackPendingDeletion(tenantId, event.getApplicationId());
            }
            if (shouldEnqueueRollbackEvent(event, errorOrigin)) {
                boolean rolledBack = trySaveRollbackEvent(tenantId, event);
                dispatchNextEvent = !rolledBack;
            }
            if (shouldClearDesiredTemplateId(event, errorOrigin)) {
                clearDesiredTemplateId(tenantId, event.getApplicationId());
            }

            return dispatchNextEvent;
        }));
    }

    private boolean trySaveRollbackEvent(TenantId tenantId, AgentAppEvent event) {
        Optional<AgentAppEvent> rollback;
        try {
            rollback = buildRollbackEvent(tenantId, event);
        } catch (Exception e) {
            log.info("[{}] Could not build a rollback event for failed event {}: {}", tenantId, event.getId(), e.getMessage());
            return false;
        }
        rollback.ifPresent(e -> appEventService.save(tenantId, e));
        return rollback.isPresent();
    }

    private boolean isDelete(AgentAppEvent event) {
        return event != null && event.hasActionType(AgentAppEventActionType.DELETE);
    }

    private boolean shouldEnqueueRollbackEvent(AgentAppEvent event, ErrorOrigin errorOrigin) {
        return event != null && event.getStartStatus() == ProcessingStartStatus.DELIVERED &&
                errorOrigin == ErrorOrigin.SERVER && event.getApplicationId() != null
                && (event.hasActionType(AgentAppEventActionType.UPDATE) || event.hasActionType(AgentAppEventActionType.UPGRADE));
    }

    private boolean shouldClearDesiredTemplateId(AgentAppEvent event, ErrorOrigin errorOrigin) {
        if (event == null || event.getApplicationId() == null) {
            return false;
        }
        // A terminal ROLLBACK leaves nothing to retry, so the pending-upgrade marker must go whoever reported the error.
        if (event.hasActionType(AgentAppEventActionType.ROLLBACK)) {
            return true;
        }
        return errorOrigin == ErrorOrigin.AGENT && event.hasActionType(AgentAppEventActionType.UPGRADE);
    }

    private void clearDesiredTemplateId(TenantId tenantId, AgentApplicationId applicationId) {
        AgentApplication app = appService.findById(tenantId, applicationId);
        if (app != null && app.getDesiredTemplateVersion() != null) {
            log.info("[{}] Clearing desiredTemplateVersion for application {} after agent error", tenantId, applicationId);
            app.setDesiredTemplateVersion(null);
            appService.save(tenantId, app);
        }
    }

    private void dispatchNextIfAppExists(TenantId tenantId, AgentId agentId, AgentAppEventId failedEventId) {
        AgentApplication application = appService.findByEventId(tenantId, failedEventId);
        if (application == null) {
            log.warn("[{}][{}] Application not found for failed event {}, skipping next event dispatch", tenantId, agentId, failedEventId);
            return;
        }
        agentEventProcessor.processNextEventForApp(tenantId, agentId, application);
    }

    private void rollbackPendingDeletion(TenantId tenantId, AgentApplicationId applicationId) {
        if (applicationId == null) {
            return;
        }
        try {
            AgentApplication app = appService.findById(tenantId, applicationId);
            if (app != null && app.isPendingDeletion()) {
                app.setPendingDeletion(false);
                appService.save(tenantId, app);
                log.info("[{}] Rolled back pendingDeletion for application {}", tenantId, applicationId);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to rollback pendingDeletion for application {}", tenantId, applicationId, e);
        }
    }

    private Optional<AgentAppEvent> buildRollbackEvent(TenantId tenantId, AgentAppEvent failedEvent) {
        AgentAppEvent rollbackEvent = new AgentAppEvent();
        rollbackEvent.setTenantId(tenantId);
        rollbackEvent.setApplicationId(failedEvent.getApplicationId());
        rollbackEvent.setAgentId(failedEvent.getAgentId());
        rollbackEvent.setApplicationName(failedEvent.getApplicationName());
        rollbackEvent.setActionType(AgentAppEventActionType.ROLLBACK);
        rollbackEvent.setStartStatus(ProcessingStartStatus.DELIVERED);
        rollbackEvent.setProcessingStatus(AgentProcessingStatus.PENDING);
        rollbackEvent.setUpdatedTime(System.currentTimeMillis());

        AgentApplication app = appService.findById(tenantId, failedEvent.getApplicationId());
        if (app == null) {
            return Optional.empty();
        }
        List<AgentAppStep> rollbackSteps = stepsResolver.resolveSteps(app, AgentAppEventActionType.ROLLBACK);
        Optional<UUID> rollbackStepId = rollbackSteps.stream()
                .filter(s -> s.getType() == AgentAppStepType.ROLLBACK)
                .findFirst()
                .map(AgentAppStep::getId);
        if (rollbackStepId.isEmpty()) {
            log.info("[{}] Template of application {} declares no ROLLBACK step, nothing to roll back", tenantId, app.getId());
            return Optional.empty();
        }

        rollbackEvent.setStepStates(Map.of(rollbackStepId.get(), new RollBackStepState(failedEvent.getId())));
        return Optional.of(rollbackEvent);
    }
}
