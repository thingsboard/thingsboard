// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultAgentEventWatchdog implements AgentEventWatchdog {

    private static final int MAX_ERROR_RETRIES = 3;

    @Value("${agents.event.watchdog_initial_delay_ms:30000}")
    private long watchdogInitialDelayMs;
    @Value("${agents.event.scheduler_pool_size:4}")
    private int schedulerPoolSize;

    private final AgentAppEventService agentAppEventService;
    private final AgentSessionRegistry agentSessionRegistry;
    private final AgentAppEventStepsResolver eventStepsResolver;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    private void init() {
        this.scheduler = ThingsBoardExecutors.newScheduledThreadPool(schedulerPoolSize, "agent-event-scheduler");
    }

    @PreDestroy
    private void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void schedule(AgentApplication application, AgentAppEvent event, AgentEventResender resender) {
        scheduleInternal(application, event.getId(), resender, 0);
    }

    private void scheduleInternal(AgentApplication application, AgentAppEventId eventId,
                                  AgentEventResender resender, int errorRetryCount) {
        AgentSession session = agentSessionRegistry.getByAgentId(application.getAgentId());
        if (session == null) {
            log.trace("[{}][{}] No active session found, skipping watchdog scheduling for event {}",
                    application.getTenantId(), application.getAgentId(), eventId);
            return;
        }
        long scheduledAt = System.currentTimeMillis();
        log.trace("[{}][{}] Scheduling watchdog for event {} with delay {}ms",
                application.getTenantId(), application.getAgentId(), eventId, watchdogInitialDelayMs);
        session.scheduleEventWatchdog(
                eventId, scheduler,
                () -> checkStalenessWithRetry(application, eventId, scheduledAt, resender, errorRetryCount),
                watchdogInitialDelayMs,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void cancel(AgentId agentId, AgentAppEventId eventId) {
        log.trace("[{}] Cancelling watchdog for event {}", agentId, eventId);
        AgentSession session = agentSessionRegistry.getByAgentId(agentId);
        if (session != null) {
            session.cancelEventWatchdog(eventId);
            log.trace("[{}] Watchdog cancelled for event {}", agentId, eventId);
        } else {
            log.trace("[{}] No active session found, nothing to cancel for event {}", agentId, eventId);
        }
    }

    private void checkStalenessWithRetry(AgentApplication application, AgentAppEventId eventId,
                                         long scheduledAt, AgentEventResender resender, int errorRetryCount) {
        try {
            checkStaleness(application, eventId, scheduledAt, resender);
        } catch (Exception e) {
            if (errorRetryCount < MAX_ERROR_RETRIES) {
                log.warn("[{}][{}] Watchdog check failed for event {}, rescheduling (attempt {}/{})",
                        application.getTenantId(), application.getAgentId(), eventId, errorRetryCount + 1, MAX_ERROR_RETRIES, e);
                scheduleInternal(application, eventId, resender, errorRetryCount + 1);
            } else {
                log.error("[{}][{}] Watchdog check failed for event {} after {} retries, marking as ERROR",
                        application.getTenantId(), application.getAgentId(), eventId, MAX_ERROR_RETRIES, e);
                resender.onError(eventId, application);
            }
        }
    }

    private void checkStaleness(AgentApplication application, AgentAppEventId eventId, long scheduledAt, AgentEventResender resender) {
        log.trace("[{}][{}] Checking staleness for event {}, scheduledAt: {}",
                application.getTenantId(), application.getAgentId(), eventId, scheduledAt);
        AgentAppEvent current = agentAppEventService.findById(application.getTenantId(), eventId);
        if (current == null) {
            log.trace("[{}][{}] Event {} not found, skipping staleness check", application.getTenantId(), application.getAgentId(), eventId);
            return;
        }
        AgentProcessingStatus processingStatus = current.getProcessingStatus();
        if (processingStatus == AgentProcessingStatus.FINISHED || processingStatus == AgentProcessingStatus.ERROR) {
            log.trace("[{}][{}] Event {} already in terminal status {}, skipping",
                    application.getTenantId(), application.getAgentId(), eventId, processingStatus);
            return;
        }
        if (current.getUpdatedTime() > scheduledAt) {
            log.trace("[{}][{}] Event {} has been updated since scheduling (updatedTime: {} > scheduledAt: {}), rescheduling watchdog",
                    application.getTenantId(), application.getAgentId(), eventId, current.getUpdatedTime(), scheduledAt);
            schedule(application, current, resender);
            return;
        }
        log.warn("[{}][{}] Event {} appears stale (no progress since {}), resending",
                application.getTenantId(), application.getAgentId(), eventId, scheduledAt);
        AgentAppStep currentStep = resolveCurrentStep(application, current);
        if (currentStep != null) {
            resender.resendCurrentStep(current, application, currentStep);
        } else {
            log.warn("[{}][{}] Couldn't find current stepId for Event {}", application.getTenantId(), application.getAgentId(), eventId);
        }
    }

    private AgentAppStep resolveCurrentStep(AgentApplication application, AgentAppEvent event) {
        List<AgentAppStep> steps = resolveSteps(application, event.getActionType());
        return StepLinkedListUtils.findByStepId(steps, event.getCurrentStepId());
    }

    private List<AgentAppStep> resolveSteps(AgentApplication application, AgentAppEventActionType actionType) {
        return eventStepsResolver.resolveSteps(application, actionType);
    }
}
