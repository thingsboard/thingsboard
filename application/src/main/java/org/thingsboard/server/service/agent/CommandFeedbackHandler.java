// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.CommandAck;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandProgress;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.gen.agent.v1.StepId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventErrorHandler;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;

import java.util.Objects;
import java.util.UUID;

/**
 * Applies agent feedback on the calling thread. The inbound dispatcher invokes it from the per-agent stripe,
 * which already delivers one agent's messages in arrival order — the order an event's ack, progress and
 * result must be applied in.
 */
@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class CommandFeedbackHandler {

    private final AgentAppEventService appEventService;
    private final AgentEventProcessor agentEventProcessor;
    private final AgentEventErrorHandler eventErrorHandler;

    public void onCommandAck(TenantId tenantId, AgentId agentId, CommandAck ack) {
        AgentAppEventId eventId = toEventId(ack.getCommandId());
        log.trace("[{}][{}] Received CommandAck for event {}, status: {}", tenantId, agentId, eventId, ack.getStatus());

        doOnCommandAck(tenantId, agentId, ack, eventId);
    }

    private void doOnCommandAck(TenantId tenantId, AgentId agentId, CommandAck ack, AgentAppEventId eventId) {
        doWithFallback(tenantId, agentId, eventId, () -> {
            if (findOwnedEvent(tenantId, agentId, eventId) == null) {
                return;
            }
            if (ack.getStatus() == AckStatus.ACCEPTED) {
                appEventService.updateStatus(eventId, AgentAppEventStatusUpdate.builder()
                        .processingStatus(AgentProcessingStatus.QUEUED)
                        .currentActivity(ack.hasMessage() ? ack.getMessage() : null)
                        .build());
            } else {
                eventErrorHandler.onFailure(tenantId, agentId, eventId, ErrorOrigin.AGENT,
                        ack.hasMessage() ? ack.getMessage() : null);
            }
        });
    }

    /**
     * The command ids come straight off an untrusted gRPC stream and every event mutator below addresses the
     * event by primary key alone, so the event must be proven to belong to the authenticated session first.
     */
    private AgentAppEvent findOwnedEvent(TenantId tenantId, AgentId agentId, AgentAppEventId eventId) {
        AgentAppEvent event = appEventService.findById(tenantId, eventId);
        if (event == null) {
            log.warn("[{}][{}] Event {} not found for agent feedback", tenantId, agentId, eventId);
            return null;
        }
        if (!tenantId.equals(event.getTenantId()) || !agentId.equals(event.getAgentId())) {
            log.warn("[{}][{}] Dropping agent feedback for event {} that belongs to tenant {} / agent {}",
                    tenantId, agentId, eventId, event.getTenantId(), event.getAgentId());
            return null;
        }
        return event;
    }

    public void onCommandProgress(TenantId tenantId, AgentId agentId, CommandProgress progress) {
        AgentAppEventId eventId = toEventId(progress.getCommandId());
        log.trace("[{}][{}] Received CommandProgress for event {}, stage: {}", tenantId, agentId, eventId, progress.getStage());
        doWithFallback(tenantId, agentId, eventId, () -> {
            if (findOwnedEvent(tenantId, agentId, eventId) == null) {
                return;
            }
            appEventService.updateStatus(eventId, AgentAppEventStatusUpdate.builder()
                    .processingStatus(AgentProcessingStatus.PROCESSING)
                    .currentActivity(progress.hasMessage() ? progress.getMessage() : null)
                    .build());
        });
    }

    public void onCommandResult(TenantId tenantId, AgentId agentId, CommandResult result) {
        AgentAppEventId eventId = toEventId(result.getCommandId());
        log.trace("[{}][{}] Received CommandResult for event {}, success: {}", tenantId, agentId, eventId, result.getSuccess());

        doOnCommandResult(tenantId, agentId, result, eventId);
    }

    private void doOnCommandResult(TenantId tenantId, AgentId agentId, CommandResult result, AgentAppEventId eventId) {
        doWithFallback(tenantId, agentId, eventId, () -> {
            AgentAppEvent event = findOwnedEvent(tenantId, agentId, eventId);
            if (event == null) {
                return;
            }
            if (event.getProcessingStatus() != null && event.getProcessingStatus().isTerminated()) {
                log.info("[{}][{}] Ignoring CommandResult for already terminated event {} (status {})",
                        tenantId, agentId, eventId, event.getProcessingStatus());
                return;
            }
            if (!result.getSuccess()) {
                eventErrorHandler.onFailure(tenantId, agentId, event.getId(), ErrorOrigin.AGENT, result.getMessage());
                return;
            }
            UUID resultStepId = toStepId(result.getStep());
            if (result.hasStep() && !Objects.equals(resultStepId, event.getCurrentStepId())) {
                log.info("[{}][{}] Ignoring stale/duplicate CommandResult for event {}: result step {} does not match current step {}",
                        tenantId, agentId, eventId, resultStepId, event.getCurrentStepId());
                return;
            }
            if (event.getActionType() != null && event.getActionType().isAgentScoped() && result.getMetadataCount() > 0) {
                appEventService.mergeContextMetadata(event, result.getMetadataMap());
            }
            appEventService.updateStatus(eventId, AgentAppEventStatusUpdate.builder()
                    .processingStatus(AgentProcessingStatus.PROCESSING)
                    .currentActivity(result.hasMessage() ? result.getMessage() : null)
                    .build());
            agentEventProcessor.processNextStepOrFinish(tenantId, agentId, event);
        });
    }

    private void doWithFallback(TenantId tenantId, AgentId agentId, AgentAppEventId eventId, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process feedback for event {}", tenantId, agentId, eventId, e);
            eventErrorHandler.onFailure(tenantId, agentId, eventId, ErrorOrigin.SERVER, e.getMessage());
        }
    }

    private AgentAppEventId toEventId(CommandId commandId) {
        return new AgentAppEventId(new UUID(commandId.getIdMSB(), commandId.getIdLSB()));
    }

    private UUID toStepId(StepId stepId) {
        return new UUID(stepId.getIdMSB(), stepId.getIdLSB());
    }
}
