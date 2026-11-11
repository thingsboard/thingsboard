// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.BackupVolumesStep;
import org.thingsboard.server.common.data.agent.step.state.BackupVolumesStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeDownStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.agent.AgentEventRateLimiter;
import org.thingsboard.server.service.agent.action.DeleteAppActionHandler;
import org.thingsboard.server.service.agent.action.UpdateActionHandler;
import org.thingsboard.server.service.agent.action.UpgradeActionHandler;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTbAgentApplicationServiceTest {

    @Mock
    private AgentAppConfigMergeOrchestrator templateMergeOrchestrator;
    @Mock
    private AgentApplicationService agentApplicationService;
    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private TbClusterService tbClusterService;
    @Mock
    private TbLogEntityActionService logEntityActionService;
    @Mock
    private UpdateActionHandler updateActionHandler;
    @Mock
    private UpgradeActionHandler upgradeActionHandler;
    @Mock
    private DeleteAppActionHandler deleteAppActionHandler;
    @Mock
    private AgentEventRateLimiter agentEventRateLimiter;
    @Mock
    private AgentAppEventStepsResolver eventStepsResolver;

    private DefaultTbAgentApplicationService service;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final User USER;

    static {
        USER = new User();
        USER.setTenantId(TENANT_ID);
    }

    @BeforeEach
    void setUp() {
        when(updateActionHandler.getActionType()).thenReturn(AgentAppEventActionType.UPDATE);
        when(upgradeActionHandler.getActionType()).thenReturn(AgentAppEventActionType.UPGRADE);
        when(deleteAppActionHandler.getActionType()).thenReturn(AgentAppEventActionType.DELETE);

        service = new DefaultTbAgentApplicationService(
                templateMergeOrchestrator, agentApplicationService, agentAppEventService, tbClusterService,
                agentEventRateLimiter, eventStepsResolver);
        service.setActionHandlers(List.of(updateActionHandler, upgradeActionHandler, deleteAppActionHandler));
        ReflectionTestUtils.setField(service, "logEntityActionService", logEntityActionService);
    }

    // ==================== update() ====================

    @Test
    void update_newApplication_throws() {
        AgentApplication app = newApplication(null);

        assertThatThrownBy(() -> service.update(app, USER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_existingApplication_savesAndLogsUpdate() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.save(eq(TENANT_ID), eq(app))).thenReturn(app);

        AgentApplication result = service.update(app, USER);

        assertThat(result.getId()).isEqualTo(APP_ID);
        verify(logEntityActionService).logEntityAction(TENANT_ID, APP_ID, app, ActionType.UPDATED, USER);
        verify(agentAppEventService, never()).save(any(), any());
    }

    @Test
    void update_activeEventForApplication_throws() {
        AgentApplication app = newApplication(APP_ID);
        when(agentAppEventService.hasActiveEventForApplication(APP_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(app, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("while an event is being processed");
        verify(agentApplicationService, never()).save(any(), any());
    }

    @Test
    void update_applicationPendingDeletion_isStillUpdatableWhileItsEventRuns() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        app.setPendingDeletion(true);
        when(agentApplicationService.save(eq(TENANT_ID), eq(app))).thenReturn(app);

        service.update(app, USER);

        verify(agentAppEventService, never()).hasActiveEventForApplication(any());
        verify(agentApplicationService).save(TENANT_ID, app);
    }

    @Test
    void update_agentUpgradeInProgress_throws() {
        AgentApplication app = newApplication(APP_ID);
        app.setAgentId(AGENT_ID);
        when(agentAppEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(app, USER))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("while an agent upgrade is in progress");
        verify(agentApplicationService, never()).save(any(), any());
    }

    // ==================== install() ====================

    @Test
    void install() throws Exception {
        AgentApplication app = newApplication(null);
        AgentApplication savedApp = newApplication(APP_ID);
        when(agentApplicationService.saveWithRelatedEntity(eq(TENANT_ID), eq(app), isNull())).thenReturn(savedApp);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        AgentAppInstallResponse result = service.install(TENANT_ID, request, USER);

        assertThat(result.getApplication().getId()).isEqualTo(APP_ID);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        AgentAppEvent event = captor.getValue();
        assertThat(event.getActionType()).isEqualTo(AgentAppEventActionType.INSTALL);
        assertThat(event.getApplicationId()).isEqualTo(APP_ID);
        assertThat(event.getStartStatus()).isEqualTo(ProcessingStartStatus.PENDING);
    }

    @Test
    void install_setsOriginToInstalled() throws Exception {
        AgentApplication app = newApplication(null);
        AgentApplication savedApp = newApplication(APP_ID);
        when(agentApplicationService.saveWithRelatedEntity(eq(TENANT_ID), any(AgentApplication.class), isNull())).thenReturn(savedApp);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        service.install(TENANT_ID, request, USER);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(agentApplicationService).saveWithRelatedEntity(eq(TENANT_ID), appCaptor.capture(), isNull());
        assertThat(appCaptor.getValue().getOrigin()).isEqualTo(AgentApplicationOrigin.INSTALLED);
    }

    @Test
    void install_noApplication_throws() {
        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);

        assertThatThrownBy(() -> service.install(TENANT_ID, request, USER))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("must include an application");
        verify(agentApplicationService, never()).saveWithRelatedEntity(any(), any(), any());
    }

    @Test
    void install_agentUpgradeInProgress_throws() {
        AgentApplication app = newApplication(null);
        app.setAgentId(AGENT_ID);
        when(agentAppEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.INSTALL);
        request.setApplication(app);

        assertThatThrownBy(() -> service.install(TENANT_ID, request, USER))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining(TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG);
        verify(agentApplicationService, never()).saveWithRelatedEntity(any(), any(), any());
    }

    // ==================== execActionEvent() ====================

    @Test
    void execActionEvent_createsEvent() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        service.execActionEvent(TENANT_ID, APP_ID, request);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);
    }

    @Test
    void execActionEvent_delegatesToActionHandler() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);

        service.execActionEvent(TENANT_ID, APP_ID, request);

        verify(deleteAppActionHandler).handle(any(), eq(request), any());
    }

    @Test
    void execActionEvent_withStepInputs_setsStepStates() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        UUID stepId = UUID.randomUUID();
        ComposeDownStepState stepState = new ComposeDownStepState();
        stepState.setRemoveVolumes(new StepField<>(true, true));

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);
        request.setStepInputs(Map.of(stepId, stepState));

        service.execActionEvent(TENANT_ID, APP_ID, request);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStepStates()).containsKey(stepId);
    }

    @Test
    void execActionEvent_stepInputsSettingAServerControlledField_areRejected() {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        UUID stepId = UUID.randomUUID();
        BackupVolumesStep templateStep = new BackupVolumesStep();
        templateStep.setId(stepId);
        BackupVolumesStepState templateState = new BackupVolumesStepState();
        templateState.setBackupVolumes(new StepField<>(List.of("data"), false));
        templateStep.setState(templateState);
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.DELETE)).thenReturn(List.of(templateStep));

        BackupVolumesStepState submitted = new BackupVolumesStepState();
        submitted.setBackupVolumes(new StepField<>(List.of("everything"), false));

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);
        request.setStepInputs(Map.of(stepId, submitted));

        assertThatThrownBy(() -> service.execActionEvent(TENANT_ID, APP_ID, request))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("server-controlled fields");
        verify(agentAppEventService, never()).save(any(), any());
    }

    @Test
    void execActionEvent_stepInputsSettingAUserChoiceField_areAccepted() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        UUID stepId = UUID.randomUUID();
        BackupVolumesStep templateStep = new BackupVolumesStep();
        templateStep.setId(stepId);
        BackupVolumesStepState templateState = new BackupVolumesStepState();
        templateState.setBackupVolumes(new StepField<>(List.of("data"), true));
        templateStep.setState(templateState);
        when(eventStepsResolver.resolveSteps(app, AgentAppEventActionType.DELETE)).thenReturn(List.of(templateStep));

        BackupVolumesStepState submitted = new BackupVolumesStepState();
        submitted.setBackupVolumes(new StepField<>(List.of("everything"), true));

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.DELETE);
        request.setStepInputs(Map.of(stepId, submitted));

        service.execActionEvent(TENANT_ID, APP_ID, request);

        ArgumentCaptor<AgentAppEvent> captor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(agentAppEventService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStepStates()).containsKey(stepId);
    }

    @Test
    void execActionEvent_activeOrPendingEventExists_throws() {
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(newApplication(APP_ID));
        when(agentAppEventService.hasActiveOrPendingEventForApplication(APP_ID)).thenReturn(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        assertThatThrownBy(() -> service.execActionEvent(TENANT_ID, APP_ID, request))
                .isInstanceOf(ThingsboardException.class);
        verify(agentAppEventService, never()).save(eq(TENANT_ID), any(AgentAppEvent.class));
    }

    @Test
    void execActionEvent_skipActiveEventCheck_bypassesGuard() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        service.execActionEvent(TENANT_ID, APP_ID, request, true);

        verify(agentAppEventService, never()).hasActiveOrPendingEventForApplication(any());
        verify(agentAppEventService).save(eq(TENANT_ID), any(AgentAppEvent.class));
    }

    @Test
    void execActionEvent_duplicateBulkEvent_returnsNullWithoutSaving() throws Exception {
        AgentApplication app = newApplication(APP_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);
        UUID bulkActionId = UUID.randomUUID();
        when(agentAppEventService.existsByApplicationIdAndBulkActionId(APP_ID, bulkActionId)).thenReturn(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);
        request.setBulkActionId(bulkActionId);

        assertThat(service.execActionEvent(TENANT_ID, APP_ID, request, true)).isNull();

        verify(agentAppEventService, never()).save(any(), any());
        verify(agentApplicationService, never()).save(any(), any());
        verify(updateActionHandler, never()).handle(any(), any(), any());
    }

    @Test
    void execActionEvent_rowLockContention_mapsToEventInProgress() {
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID))
                .thenThrow(new PessimisticLockingFailureException("could not obtain lock"));

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        assertThatThrownBy(() -> service.execActionEvent(TENANT_ID, APP_ID, request))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining(TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG);
        verify(agentAppEventService, never()).save(any(), any());
    }

    @Test
    void execActionEvent_agentEventGuardIsNotBypassedBySkipActiveEventCheck() {
        AgentApplication app = newApplication(APP_ID);
        app.setAgentId(AGENT_ID);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);
        when(agentAppEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        assertThatThrownBy(() -> service.execActionEvent(TENANT_ID, APP_ID, request, true))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining(TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG);
        verify(agentAppEventService, never()).hasActiveOrPendingEventForApplication(any());
        verify(agentAppEventService, never()).save(any(), any());
    }

    @Test
    void execActionEvent_clearsDesiredTemplateId() throws Exception {
        String desiredTemplateVersion = "1.0.0";
        AgentApplication app = newApplication(APP_ID);
        app.setDesiredTemplateVersion(desiredTemplateVersion);
        when(agentApplicationService.findByIdForUpdate(TENANT_ID, APP_ID)).thenReturn(app);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setActionType(AgentAppEventActionType.RESTART);

        service.execActionEvent(TENANT_ID, APP_ID, request);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(agentApplicationService).save(eq(TENANT_ID), appCaptor.capture());
        assertThat(appCaptor.getValue().getDesiredTemplateVersion()).isNull();
    }

    // ==================== cancelEvent() ====================

    @Test
    void cancelEvent_success() throws Exception {
        AgentAppEvent event = new AgentAppEvent(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setApplicationId(APP_ID);
        event.setProcessingStatus(AgentProcessingStatus.PROCESSING);
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        AgentApplication app = newApplication(APP_ID);
        app.setAgentId(AGENT_ID);
        when(agentApplicationService.findById(TENANT_ID, APP_ID)).thenReturn(app);

        service.cancelEvent(TENANT_ID, EVENT_ID);

        verify(tbClusterService).onAgentAppEventCancelled(TENANT_ID, AGENT_ID, event);
    }

    @Test
    void cancelEvent_agentScopedEvent_routesOnTheEventsAgentWithoutLoadingAnApplication() throws Exception {
        AgentAppEvent event = new AgentAppEvent(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setAgentId(AGENT_ID);
        event.setActionType(AgentAppEventActionType.AGENT_UPGRADE);
        event.setProcessingStatus(AgentProcessingStatus.PROCESSING);
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        service.cancelEvent(TENANT_ID, EVENT_ID);

        verify(tbClusterService).onAgentAppEventCancelled(TENANT_ID, AGENT_ID, event);
        verify(agentApplicationService, never()).findById(any(), any());
    }

    @Test
    void cancelEvent_alreadyFinished_throws() {
        AgentAppEvent event = new AgentAppEvent(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setProcessingStatus(AgentProcessingStatus.FINISHED);
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        assertThatThrownBy(() -> service.cancelEvent(TENANT_ID, EVENT_ID))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    void cancelEvent_alreadyError_throws() {
        AgentAppEvent event = new AgentAppEvent(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setProcessingStatus(AgentProcessingStatus.ERROR);
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(event);

        assertThatThrownBy(() -> service.cancelEvent(TENANT_ID, EVENT_ID))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    void cancelEvent_eventNotFound_throws() {
        when(agentAppEventService.findById(TENANT_ID, EVENT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelEvent(TENANT_ID, EVENT_ID))
                .isInstanceOf(ThingsboardException.class);
    }

    // ==================== upgradeAgent() ====================

    @Test
    void upgradeAgent_createsPendingAgentUpgradeEventCarryingTheImageRef() throws Exception {
        when(agentAppEventService.save(eq(TENANT_ID), any(AgentAppEvent.class))).thenAnswer(inv -> inv.getArgument(1));

        AgentAppEvent event = service.upgradeAgent(TENANT_ID, AGENT_ID, "thingsboard/tb-remote-agent:1.2.3");

        assertThat(event.getAgentId()).isEqualTo(AGENT_ID);
        assertThat(event.getActionType()).isEqualTo(AgentAppEventActionType.AGENT_UPGRADE);
        assertThat(event.getStartStatus()).isEqualTo(ProcessingStartStatus.PENDING);
        assertThat(event.getContextMetadata()).containsEntry(AgentUpgradeKeys.IMAGE_REF, "thingsboard/tb-remote-agent:1.2.3");
        verify(agentEventRateLimiter).checkOrThrow(TENANT_ID, AGENT_ID);
    }

    @Test
    void upgradeAgent_emptyImageRef_throwsBeforeTouchingAnything() {
        assertThatThrownBy(() -> service.upgradeAgent(TENANT_ID, AGENT_ID, ""))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("imageRef must not be empty");
        verify(agentEventRateLimiter, never()).checkOrThrow(any(), any());
        verify(agentAppEventService, never()).save(any(), any());
    }

    @Test
    void upgradeAgent_agentEventAlreadyInFlight_throws() {
        when(agentAppEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.upgradeAgent(TENANT_ID, AGENT_ID, "thingsboard/tb-remote-agent:1.2.3"))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining(TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG);
        verify(agentEventRateLimiter, never()).checkOrThrow(any(), any());
        verify(agentAppEventService, never()).save(any(), any());
    }

    @Test
    void upgradeAgent_appEventAlreadyInFlight_throws() {
        when(agentAppEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(false);
        when(agentAppEventService.hasActiveOrPendingAppEventForAgent(AGENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.upgradeAgent(TENANT_ID, AGENT_ID, "thingsboard/tb-remote-agent:1.2.3"))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining(TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG);
        verify(agentEventRateLimiter, never()).checkOrThrow(any(), any());
    }

    @Test
    void upgradeAgent_rateLimited_propagates() {
        doThrow(new TbRateLimitsException(EntityType.AGENT)).when(agentEventRateLimiter).checkOrThrow(TENANT_ID, AGENT_ID);

        assertThatThrownBy(() -> service.upgradeAgent(TENANT_ID, AGENT_ID, "thingsboard/tb-remote-agent:1.2.3"))
                .isInstanceOf(TbRateLimitsException.class);
        verify(agentAppEventService, never()).save(any(), any());
    }

    @Test
    void upgradeAgent_lostRaceOnTheUniqueIndex_mapsToEventInProgress() {
        when(agentAppEventService.save(eq(TENANT_ID), any(AgentAppEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> service.upgradeAgent(TENANT_ID, AGENT_ID, "thingsboard/tb-remote-agent:1.2.3"))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining(TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG);
    }

    // ==================== Helpers ====================

    private AgentApplication newApplication(AgentApplicationId id) {
        AgentApplication app = new AgentApplication();
        if (id != null) {
            app.setId(id);
        }
        app.setTenantId(TENANT_ID);
        return app;
    }

    private DockerComposeConfig createDockerComposeConfig(String composeContent) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(new ObjectMapper().valueToTree(composeContent));
        return config;
    }
}
