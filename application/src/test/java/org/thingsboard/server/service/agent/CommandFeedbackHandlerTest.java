// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.gen.agent.v1.StepId;
import org.thingsboard.server.service.agent.event.AgentEventErrorHandler;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandFeedbackHandlerTest {

    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentEventProcessor agentEventProcessor;
    @Mock
    private AgentEventErrorHandler eventErrorHandler;

    @InjectMocks
    private CommandFeedbackHandler handler;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final UUID STEP_1_ID = UUID.randomUUID();
    private static final UUID STEP_2_ID = UUID.randomUUID();

    @Test
    void onCommandResult_successForCurrentStep_advances() {
        AgentAppEvent event = newEvent(AgentProcessingStatus.PROCESSING, STEP_1_ID);
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        ArgumentCaptor<AgentAppEventStatusUpdate> captor = ArgumentCaptor.forClass(AgentAppEventStatusUpdate.class);
        verify(appEventService).updateStatus(eq(EVENT_ID), captor.capture());
        assertThat(captor.getValue().getProcessingStatus()).isEqualTo(AgentProcessingStatus.PROCESSING);
        assertThat(captor.getValue().getCurrentStepId()).isNull();
        verify(agentEventProcessor).processNextStepOrFinish(TENANT_ID, AGENT_ID, event);
    }

    @Test
    void onCommandResult_lookupThrows_failsEventWithServerOrigin() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenThrow(new RuntimeException("db is down"));

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(eventErrorHandler).onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, "db is down");
        verify(agentEventProcessor, never()).processNextStepOrFinish(any(), any(), any());
    }

    @Test
    void onCommandResult_duplicateForAlreadyAdvancedStep_skips() {
        // Event has already advanced to STEP_2, a duplicate success for STEP_1 must not advance again.
        AgentAppEvent event = newEvent(AgentProcessingStatus.PENDING, STEP_2_ID);
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(appEventService, never()).updateStatus(any(), any());
        verify(agentEventProcessor, never()).processNextStepOrFinish(any(), any(), any());
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    @Test
    void onCommandResult_terminatedEvent_skips() {
        AgentAppEvent event = newEvent(AgentProcessingStatus.FINISHED, STEP_1_ID);
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(appEventService, never()).updateStatus(any(), any());
        verify(agentEventProcessor, never()).processNextStepOrFinish(any(), any(), any());
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    @Test
    void onCommandResult_eventOfAnotherAgent_isDropped() {
        AgentAppEvent event = newEvent(AgentProcessingStatus.PROCESSING, STEP_1_ID);
        event.setAgentId(new AgentId(UUID.randomUUID()));
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(appEventService, never()).updateStatus(any(), any());
        verify(agentEventProcessor, never()).processNextStepOrFinish(any(), any(), any());
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    @Test
    void onCommandResult_eventOfAnotherTenant_isDropped() {
        AgentAppEvent event = newEvent(AgentProcessingStatus.PROCESSING, STEP_1_ID);
        event.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        handler.onCommandResult(TENANT_ID, AGENT_ID, successResult(STEP_1_ID));

        verify(appEventService, never()).updateStatus(any(), any());
        verify(agentEventProcessor, never()).processNextStepOrFinish(any(), any(), any());
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    private AgentAppEvent newEvent(AgentProcessingStatus processingStatus, UUID currentStepId) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setAgentId(AGENT_ID);
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setProcessingStatus(processingStatus);
        event.setCurrentStepId(currentStepId);
        return event;
    }

    private CommandResult successResult(UUID stepId) {
        return CommandResult.newBuilder()
                .setCommandId(CommandId.newBuilder()
                        .setIdMSB(EVENT_ID.getId().getMostSignificantBits())
                        .setIdLSB(EVENT_ID.getId().getLeastSignificantBits())
                        .build())
                .setSuccess(true)
                .setStep(StepId.newBuilder()
                        .setIdMSB(stepId.getMostSignificantBits())
                        .setIdLSB(stepId.getLeastSignificantBits())
                        .build())
                .build();
    }
}
