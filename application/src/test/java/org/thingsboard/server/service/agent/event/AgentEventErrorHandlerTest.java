// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.step.RollBackStep;
import org.thingsboard.server.common.data.agent.step.state.RollBackStepState;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentEventErrorHandlerTest {

    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentApplicationService appService;
    @Mock
    private AgentAppEventStepsResolver stepsResolver;
    @Mock
    private AgentEventWatchdog eventWatchdog;
    @Mock
    private AgentEventProcessor agentEventProcessor;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AgentEventErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<org.springframework.transaction.support.TransactionCallback<?>>getArgument(0)
                        .doInTransaction(null));
        lenient().when(appEventService.updateStatus(any(), any())).thenReturn(true);
    }

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());
    private static final UUID ROLLBACK_STEP_ID = UUID.randomUUID();

    // ==================== Common behavior ====================

    @Test
    void onFailure_alwaysMarksErrorAndCancelsWatchdog() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(newApplication());

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(appEventService).updateStatus(EVENT_ID, AgentAppEventStatusUpdate.builder()
                .processingStatus(AgentProcessingStatus.ERROR).build());
        verify(eventWatchdog).cancel(AGENT_ID, EVENT_ID);
    }

    @Test
    void onFailure_eventAlreadyTerminal_skipsRollbackSideEffectsAndDispatch() {
        // A success result already finished the event; updateStatus(ERROR) no-ops (returns false).
        // The late failure must not roll back / enqueue a spurious ROLLBACK or dispatch the next event.
        when(appEventService.updateStatus(any(), any())).thenReturn(false);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(eventWatchdog).cancel(AGENT_ID, EVENT_ID);
        verify(appEventService, never()).findById(any(), any());
        verify(appEventService, never()).save(any(), any());
        verify(agentEventProcessor, never()).processNextEventForApp(any(), any(), any());
    }

    // ==================== Dispatch next (queue must never stall) ====================

    @Test
    void onFailure_install_serverError_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_install_agentError_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT, null);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_restart_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.RESTART));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT, null);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_eventNotFound_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(null);
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_applicationNotFound_doesNotThrow() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.INSTALL));
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(null);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(agentEventProcessor, never()).processNextEventForApp(any(), any(), any());
    }

    @Test
    void onFailure_agentScopedAction_dispatchesEveryQueuedAppEventOfTheAgent() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(agentScopedEvent());

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT, null);

        verify(agentEventProcessor).dispatchAllQueuedAppEvents(TENANT_ID, AGENT_ID);
        verify(agentEventProcessor, never()).processNextEventForApp(any(), any(), any());
        verify(appService, never()).findByEventId(any(), any());
    }

    // ==================== DELETE → rollback pending deletion ====================

    @Test
    void onFailure_delete_rollbacksPendingDeletion() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.DELETE));
        AgentApplication app = newApplication();
        app.setPendingDeletion(true);
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT, null);

        assertThat(app.isPendingDeletion()).isFalse();
        verify(appService).save(TENANT_ID, app);
    }

    @Test
    void onFailure_delete_dispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.DELETE));
        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_delete_doesNotCreateRollbackEvent() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.DELETE));
        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(appEventService, never()).save(any(), any());
    }

    // ==================== SERVER + UPDATE → enqueue rollback event ====================

    @Test
    void onFailure_serverUpdate_createsRollbackEvent() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(deliveredEvent(AgentAppEventActionType.UPDATE));
        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(stepsResolver.resolveSteps(app, AgentAppEventActionType.ROLLBACK)).thenReturn(List.of(newRollbackStep()));

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(appEventService).save(eq(TENANT_ID), captor.capture());

        AgentAppEvent rollback = captor.getValue();
        assertThat(rollback.getApplicationId()).isEqualTo(APP_ID);
        assertThat(rollback.getActionType()).isEqualTo(AgentAppEventActionType.ROLLBACK);
        assertThat(rollback.getStartStatus()).isEqualTo(ProcessingStartStatus.DELIVERED);
        assertThat(rollback.getProcessingStatus()).isEqualTo(AgentProcessingStatus.PENDING);
        assertThat(rollback.getStepStates()).containsKey(ROLLBACK_STEP_ID);
        RollBackStepState stepState = (RollBackStepState) rollback.getStepStates().get(ROLLBACK_STEP_ID);
        assertThat(stepState.getFailedEventId().getValue()).isEqualTo(EVENT_ID);
    }

    @Test
    void onFailure_serverUpdate_doesNotDispatchNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(deliveredEvent(AgentAppEventActionType.UPDATE));
        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(stepsResolver.resolveSteps(app, AgentAppEventActionType.ROLLBACK)).thenReturn(List.of(newRollbackStep()));

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(agentEventProcessor, never()).processNextEventForApp(any(), any(), any());
    }

    @Test
    void onFailure_serverUpdate_applicationGone_savesNoRollbackAndDispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(deliveredEvent(AgentAppEventActionType.UPDATE));
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(null);
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(appEventService, never()).save(any(), any());
        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    @Test
    void onFailure_serverUpdate_templateWithoutRollbackStep_savesNoRollbackAndDispatchesNext() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(deliveredEvent(AgentAppEventActionType.UPDATE));
        AgentApplication app = newApplication();
        when(appService.findById(TENANT_ID, APP_ID)).thenReturn(app);
        when(stepsResolver.resolveSteps(app, AgentAppEventActionType.ROLLBACK)).thenReturn(List.of());
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(appEventService, never()).save(any(), any());
        verify(eventWatchdog).cancel(AGENT_ID, EVENT_ID);
        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    // ==================== NULL STATUS (cancelled pre-delivery) + SERVER + UPDATE → no rollback ====================

    @Test
    void onFailure_nullStatusServerUpdate_doesNotCreateRollbackEvent() {
        // A cancelled, never-delivered UPDATE (null status, startStatus != DELIVERED):
        // nothing reached the agent, so there is nothing to roll back.
        AgentAppEvent event = newEvent(AgentAppEventActionType.UPDATE);
        assertThat(event.getProcessingStatus()).isNull();
        assertThat(event.getStartStatus()).isNotEqualTo(ProcessingStartStatus.DELIVERED);
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(appEventService, never()).save(any(), any());
        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    // ==================== AGENT + UPDATE → no rollback, just dispatch ====================

    @Test
    void onFailure_agentUpdate_doesNotCreateRollbackEvent() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.UPDATE));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.AGENT, null);

        verify(appEventService, never()).save(any(), any());
        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    // ==================== ROLLBACK failure → no recursive rollback ====================

    @Test
    void onFailure_rollbackEvent_serverError_doesNotCreateAnotherRollback() {
        when(appEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(newEvent(AgentAppEventActionType.ROLLBACK));
        AgentApplication app = newApplication();
        when(appService.findByEventId(TENANT_ID, EVENT_ID)).thenReturn(app);

        errorHandler.onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, null);

        verify(appEventService, never()).save(any(), any());
        verify(agentEventProcessor).processNextEventForApp(TENANT_ID, AGENT_ID, app);
    }

    // ==================== Helpers ====================

    private AgentAppEvent newEvent(AgentAppEventActionType actionType) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setApplicationId(APP_ID);
        event.setActionType(actionType);
        return event;
    }

    private AgentAppEvent agentScopedEvent() {
        AgentAppEvent event = newEvent(AgentAppEventActionType.AGENT_UPGRADE);
        event.setApplicationId(null);
        event.setAgentId(AGENT_ID);
        return event;
    }

    private AgentAppEvent deliveredEvent(AgentAppEventActionType actionType) {
        AgentAppEvent event = newEvent(actionType);
        event.setStartStatus(ProcessingStartStatus.DELIVERED);
        return event;
    }

    private AgentApplication newApplication() {
        AgentApplication app = new AgentApplication();
        app.setId(APP_ID);
        app.setTenantId(TENANT_ID);
        app.setAgentId(AGENT_ID);
        return app;
    }

    private RollBackStep newRollbackStep() {
        RollBackStep step = new RollBackStep();
        step.setId(ROLLBACK_STEP_ID);
        return step;
    }
}
