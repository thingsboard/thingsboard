// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.agent.step.state.BackupVolumesStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.agent.bulk.DefaultAgentBulkActionProcessingService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAgentBulkActionProcessingServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentProfileId AGENT_PROFILE_ID = new AgentProfileId(UUID.randomUUID());
    private static final AgentAppProfileId APPLICATION_PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final String TEMPLATE_VERSION = "1.0.0";
    private static final User USER = new User();

    @Mock
    private AgentAppProfileService profileService;
    @Mock
    private AgentApplicationDao applicationDao;
    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private AgentBulkActionService agentBulkActionService;
    @Mock
    private TbClusterService clusterService;
    @Mock
    private TbAgentApplicationService tbAgentApplicationService;
    @Mock
    private PartitionService partitionService;

    private DefaultAgentBulkActionProcessingService service;

    @BeforeAll
    static void beforeAll() {
        USER.setId(new UserId(UUID.randomUUID()));
    }

    @BeforeEach
    void setUp() {
        service = new DefaultAgentBulkActionProcessingService(tbAgentApplicationService, profileService, applicationDao, agentAppEventService,
                agentBulkActionService, clusterService, partitionService);
        ReflectionTestUtils.setField(service, "stuckActionThresholdMs", 600_000L);

        lenient().when(agentBulkActionService.save(any(), any())).thenAnswer(invocation -> {
            AgentBulkAction action = invocation.getArgument(1);
            if (action.getId() == null) {
                action.setId(new AgentBulkActionId(UUID.randomUUID()));
            }
            return action;
        });
    }

    // ==================== enqueueBulkOperation ====================

    @Test
    void enqueueBulkOperation_rejectsNullActionType() {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(null);

        assertThatThrownBy(() -> service.enqueueBulkOperation(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(clusterService);
    }

    @ParameterizedTest
    @EnumSource(value = AgentAppEventActionType.class,
            names = {"UPDATE", "DELETE", "RESTART", "ROLLBACK", "UPGRADE"}, mode = EnumSource.Mode.EXCLUDE)
    void enqueueBulkOperation_restrictedActionTypes(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);

        assertThatThrownBy(() -> service.enqueueBulkOperation(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(clusterService);
    }

    @ParameterizedTest
    @EnumSource(value = AgentAppEventActionType.class, names = {"UPDATE", "DELETE", "RESTART", "ROLLBACK", "UPGRADE"})
    void enqueueBulkOperation_allowedActionTypes(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);

        AgentBulkAction result = service.enqueueBulkOperation(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request);

        assertThat(result.getStatus()).isEqualTo(AgentBulkActionStatus.QUEUED);
        verify(clusterService).pushMsgToAgentBulkOps(any(), eq(request));
    }

    @Test
    void enqueueBulkOperation_savesWithQueuedStatusAndPublishes() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);

        AgentBulkAction result = service.enqueueBulkOperation(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request);

        assertThat(result.getStatus()).isEqualTo(AgentBulkActionStatus.QUEUED);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getAgentProfileId()).isEqualTo(AGENT_PROFILE_ID.getId());
        assertThat(result.getApplicationProfileId()).isEqualTo(APPLICATION_PROFILE_ID.getId());
        assertThat(result.getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService).save(eq(TENANT_ID), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentBulkActionStatus.QUEUED);

        verify(clusterService).pushMsgToAgentBulkOps(any(), eq(request));
    }

    // ==================== processBulkOperation ====================

    @Test
    void processBulkOperation_skipsWhenBulkActionNotFound() {
        AgentBulkOperationMsg msg = buildProtoMsg(AgentAppEventActionType.UPDATE);

        service.processBulkOperation(msg);

        verify(agentBulkActionService).findById(any(), any());
        verify(agentBulkActionService, never()).save(any(), any());
    }

    @Test
    void processBulkOperation_setsInProgressAndExecutes() {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentAppProfileInfo profile = createProfile();
        AgentApplicationInfo eligibleApp = createApplication();

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(profile);
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(eligibleApp), 1, 1, false));

        service.processBulkOperation(msg);

        // save is called at least twice: first for IN_PROGRESS, then for COMPLETED.
        // Since the same object is mutated, we verify processingStartedTime was set (during IN_PROGRESS)
        // and final status is COMPLETED.
        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        AgentBulkAction lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getProcessingStartedTime()).isNotNull();
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.STARTED);
    }

    @Test
    void processBulkOperation_failsToStartWhenEveryAppIsSkipped() {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentApplicationInfo skipped = createApplication();
        skipped.setTemplateVersion("0.0.0-mismatch");

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        stubApps(skipped);

        service.processBulkOperation(msg);

        AgentBulkAction lastSaved = lastSavedBulkAction();
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.START_FAILED);
        assertThat(lastSaved.getErrorMsg()).isEqualTo("Couldn't find any eligible applications for execution");
        assertThat(lastSaved.getTotal()).isEqualTo(1);
        assertThat(lastSaved.getSkipCounts()).containsEntry(SkipReason.VERSION_MISMATCH, 1);
        verifyNoInteractions(tbAgentApplicationService);
    }

    @Test
    void processBulkOperation_failsToStartWhenTheProfileMatchesNoApplication() {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        service.processBulkOperation(buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE));

        AgentBulkAction lastSaved = lastSavedBulkAction();
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.START_FAILED);
        assertThat(lastSaved.getTotal()).isZero();
        assertThat(lastSaved.getSkipCounts()).isEmpty();
    }

    @Test
    void processBulkOperation_executesForEligibleApps() throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentAppProfileInfo profile = createProfile();

        AgentApplicationInfo app = createApplication();

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(profile);
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(app), 1, 1, false));
        when(tbAgentApplicationService.execActionEvent(any(), eq(app.getId()), any()))
                .thenReturn(new AgentAppEvent());

        service.processBulkOperation(msg);

        verify(tbAgentApplicationService).execActionEvent(any(), eq(app.getId()), any());

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        List<AgentBulkAction> savedActions = captor.getAllValues();
        AgentBulkAction lastSaved = savedActions.get(savedActions.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.STARTED);
        assertThat(lastSaved.getSubmitted()).isEqualTo(1);
    }

    // ==================== failStuckBulkActions ====================

    @Test
    void failStuckBulkActions_skipsWhenNotMyPartition() {
        TopicPartitionInfo tpi = new TopicPartitionInfo("topic", null, 0, false);
        when(partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)).thenReturn(tpi);

        service.failStuckBulkActions();

        verifyNoInteractions(agentBulkActionService);
    }

    @Test
    void failStuckBulkActions_failsStuckInProgressAction() {
        TopicPartitionInfo tpi = new TopicPartitionInfo("topic", null, 0, true);
        when(partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)).thenReturn(tpi);

        AgentBulkAction stuckAction = createBulkAction(new AgentBulkActionId(UUID.randomUUID()), AgentBulkActionStatus.IN_PROGRESS);
        when(agentBulkActionService.findStuckBulkActions(anyLong(), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(stuckAction), 1, 1, false))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        service.failStuckBulkActions();

        ArgumentCaptor<String> errorMsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(agentBulkActionService).failIfStillStuck(eq(stuckAction.getId()),
                eq(AgentBulkActionStatus.START_FAILED), errorMsgCaptor.capture());
        assertThat(errorMsgCaptor.getValue()).contains("Stuck in IN_PROGRESS");
        verify(agentBulkActionService, never()).save(any(), any());
    }

    @Test
    void failStuckBulkActions_failsStuckQueuedAction() {
        TopicPartitionInfo tpi = new TopicPartitionInfo("topic", null, 0, true);
        when(partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)).thenReturn(tpi);

        AgentBulkAction queuedAction = createBulkAction(new AgentBulkActionId(UUID.randomUUID()), AgentBulkActionStatus.QUEUED);
        when(agentBulkActionService.findStuckBulkActions(anyLong(), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(queuedAction), 1, 1, false))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        service.failStuckBulkActions();

        ArgumentCaptor<String> errorMsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(agentBulkActionService).failIfStillStuck(eq(queuedAction.getId()),
                eq(AgentBulkActionStatus.START_FAILED), errorMsgCaptor.capture());
        assertThat(errorMsgCaptor.getValue()).contains("Stuck in QUEUED");
    }

    // ==================== processBulkOperation error handling ====================

    @Test
    void processBulkOperation_unexpectedException_savesFailedStatus() {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE);

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenThrow(new RuntimeException("DB connection lost"));

        service.processBulkOperation(msg);

        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        List<AgentBulkAction> saved = captor.getAllValues();
        AgentBulkAction lastSaved = saved.get(saved.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.START_FAILED);
        assertThat(lastSaved.getErrorMsg()).contains("DB connection lost");
    }

    // ==================== preview / eligibility skip reasons ====================

    @Test
    void preview_rejectsInvalidActionType() {
        BulkOperationRequest request = createRequest(AgentAppEventActionType.INSTALL);

        assertThatThrownBy(() -> service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(profileService);
    }

    @Test
    void preview_skipsVersionMismatch_whenTemplateDiffersForUpdate() {
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        AgentApplicationInfo app = createApplication();
        app.setTemplateVersion("0.0.0-mismatch"); // differs from profile template
        stubApps(app);

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.UPDATE));

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isZero();
        assertThat(preview.getSkippedCountsByReason()).containsEntry(SkipReason.VERSION_MISMATCH, 1);
    }

    @Test
    void preview_skipsActiveEvent_whenEventPending() {
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        AgentApplicationInfo app = createApplication(); // template matches profile
        stubApps(app);
        when(agentAppEventService.findApplicationIdsWithActiveOrPendingEvents(anyCollection())).thenReturn(Set.of(app.getId()));

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.UPDATE));

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isZero();
        assertThat(preview.getSkippedCountsByReason()).containsEntry(SkipReason.ACTIVE_EVENT, 1);
    }

    @Test
    void preview_skipsActiveEvent_whenTheOwningAgentHasAnAgentLevelEventInFlight() {
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        AgentApplicationInfo app = createApplication(); // no app-level event, only an agent-level one
        stubApps(app);
        when(agentAppEventService.findAgentIdsWithActiveOrPendingAgentEvents(anyCollection()))
                .thenReturn(Set.of(app.getAgentId()));

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.UPDATE));

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isZero();
        assertThat(preview.getSkippedCountsByReason()).containsEntry(SkipReason.ACTIVE_EVENT, 1);
    }

    @Test
    void preview_restartIsExemptFromTheTemplateVersionEqualityCheck() {
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        AgentApplicationInfo app = createApplication();
        app.setTemplateVersion("0.0.0-mismatch"); // would be VERSION_MISMATCH for UPDATE
        stubApps(app);

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.RESTART));

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isEqualTo(1);
        assertThat(preview.getSkippedCountsByReason()).isEmpty();
    }

    @Test
    void preview_skipsVersionMismatch_forUpgradeWhenNextVersionDiffers() {
        AgentAppProfileInfo profile = createProfile();
        profile.setTemplateCurrentVersion("2.0.0");
        when(profileService.findProfileInfoById(any(), any())).thenReturn(profile);
        AgentApplicationInfo app = createApplication(); // template matches; UPGRADE ignores template equality
        app.setNextVersion("1.0.0"); // != profile current version -> not yet on target
        stubApps(app);

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.UPGRADE));

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isZero();
        assertThat(preview.getSkippedCountsByReason()).containsEntry(SkipReason.VERSION_MISMATCH, 1);
    }

    @Test
    void preview_eligibleForUpgrade_whenNextVersionMatches() {
        AgentAppProfileInfo profile = createProfile();
        profile.setTemplateCurrentVersion("2.0.0");
        when(profileService.findProfileInfoById(any(), any())).thenReturn(profile);
        AgentApplicationInfo app = createApplication();
        app.setNextVersion("2.0.0"); // already targeting the profile's current version
        stubApps(app);

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.UPGRADE));

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isEqualTo(1);
        assertThat(preview.getSkippedCountsByReason()).isEmpty();
    }

    @Test
    void preview_eligibleWhenNoSkipReason() {
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        stubApps(createApplication());

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.UPDATE));

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isEqualTo(1);
        assertThat(preview.getSkippedSample()).isEmpty();
    }

    @Test
    void preview_capsSampleAt20PerReason() {
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        List<AgentApplicationInfo> apps = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            apps.add(createApplication());
        }
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(apps, 1, 25, false));
        when(agentAppEventService.findApplicationIdsWithActiveOrPendingEvents(anyCollection()))
                .thenAnswer(inv -> new HashSet<>(inv.getArgument(0, Collection.class)));

        BulkOperationPreview preview = service.preview(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, createRequest(AgentAppEventActionType.UPDATE));

        assertThat(preview.getTotal()).isEqualTo(25);
        assertThat(preview.getEligible()).isZero();
        assertThat(preview.getSkippedCountsByReason()).containsEntry(SkipReason.ACTIVE_EVENT, 25);
        assertThat(preview.getSkippedSample()).hasSize(20);
    }

    // ==================== per-app execution-failure mapping ====================

    @Test
    void processBulkOperation_activeEventInProgress_mapsToActiveEvent() throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentApplicationInfo app = createApplication();

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        stubApps(app);
        when(tbAgentApplicationService.execActionEvent(any(), eq(app.getId()), any()))
                .thenThrow(new ThingsboardException(TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS));

        service.processBulkOperation(buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE));

        AgentBulkAction lastSaved = lastSavedBulkAction();
        assertThat(lastSaved.getSkipCounts()).containsEntry(SkipReason.ACTIVE_EVENT, 1);
        assertThat(lastSaved.getSubmitted()).isZero();
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.STARTED);
    }

    @Test
    void processBulkOperation_rateLimitExceeded_mapsToRateLimit() throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentApplicationInfo app = createApplication();

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        stubApps(app);
        when(tbAgentApplicationService.execActionEvent(any(), eq(app.getId()), any()))
                .thenThrow(new TbRateLimitsException(EntityType.TENANT));

        service.processBulkOperation(buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE));

        AgentBulkAction lastSaved = lastSavedBulkAction();
        assertThat(lastSaved.getSkipCounts()).containsEntry(SkipReason.RATE_LIMIT_EXCEEDED, 1);
        assertThat(lastSaved.getSubmitted()).isZero();
        assertThat(lastSaved.getStatus()).isEqualTo(AgentBulkActionStatus.STARTED);
    }

    @Test
    void processBulkOperation_genericException_mapsToError() throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentApplicationInfo app = createApplication();

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        stubApps(app);
        when(tbAgentApplicationService.execActionEvent(any(), eq(app.getId()), any()))
                .thenThrow(new RuntimeException("docker daemon down"));

        service.processBulkOperation(buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE));

        AgentBulkAction lastSaved = lastSavedBulkAction();
        assertThat(lastSaved.getSkipCounts()).containsEntry(SkipReason.ERROR, 1);
        assertThat(lastSaved.getSubmitted()).isZero();
    }

    // ==================== synthetic error event for skipped apps ====================

    @Test
    void processBulkOperation_savesSyntheticErrorEventForSkippedApps() throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);

        AgentApplicationInfo eligible = createApplication();
        AgentApplicationInfo skipped = createApplication();
        skipped.setTemplateVersion("0.0.0-mismatch"); // VERSION_MISMATCH

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(eligible, skipped), 1, 2, false));
        when(tbAgentApplicationService.execActionEvent(any(), eq(eligible.getId()), any()))
                .thenReturn(new AgentAppEvent());

        service.processBulkOperation(buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE));

        ArgumentCaptor<List<AgentAppEvent>> eventCaptor = ArgumentCaptor.captor();
        verify(agentAppEventService).saveAll(eq(TENANT_ID), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).hasSize(1);
        AgentAppEvent errEvent = eventCaptor.getValue().get(0);
        assertThat(errEvent.getApplicationId()).isEqualTo(skipped.getId());
        assertThat(errEvent.getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);
        assertThat(errEvent.getStartStatus()).isEqualTo(ProcessingStartStatus.DELIVERY_FAIL);
        assertThat(errEvent.getProcessingStatus()).isEqualTo(AgentProcessingStatus.START_FAILED);
        assertThat(errEvent.getErrorMessage()).contains(SkipReason.VERSION_MISMATCH.name());
        assertThat(errEvent.getBulkActionId()).isEqualTo(bulkActionId.getId());
    }

    // ==================== previewAsync ====================

    @Test
    void previewAsync_runsOffTheCallingThreadAndReturnsTheSamePreview() throws Exception {
        ReflectionTestUtils.setField(service, "previewThreads", 1);
        ReflectionTestUtils.setField(service, "previewQueueSize", 1);
        service.init();

        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        stubApps(createApplication());

        Thread callingThread = Thread.currentThread();
        AtomicReference<Thread> scanThread = new AtomicReference<>();
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    scanThread.set(Thread.currentThread());
                    return new PageData<>(List.of(createApplication()), 1, 1, false);
                });

        BulkOperationPreview preview = service.previewAsync(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID,
                createRequest(AgentAppEventActionType.UPDATE)).get(10, TimeUnit.SECONDS);

        assertThat(preview.getTotal()).isEqualTo(1);
        assertThat(preview.getEligible()).isEqualTo(1);
        assertThat(scanThread.get()).isNotSameAs(callingThread);
        service.shutdown();
    }

    @Test
    void previewAsync_rejectsInvalidActionTypeEagerlyWithoutTouchingTheExecutor() {
        assertThatThrownBy(() -> service.previewAsync(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID,
                createRequest(AgentAppEventActionType.INSTALL)))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(profileService, applicationDao);
    }

    @Test
    void previewAsync_rejectsWithBadRequestWhenTheQueueIsFull() throws Exception {
        ReflectionTestUtils.setField(service, "previewThreads", 1);
        ReflectionTestUtils.setField(service, "previewQueueSize", 1);
        service.init();

        CountDownLatch release = new CountDownLatch(1);
        when(profileService.findProfileInfoById(any(), any())).thenAnswer(inv -> {
            release.await(10, TimeUnit.SECONDS);
            return createProfile();
        });

        BulkOperationRequest request = createRequest(AgentAppEventActionType.UPDATE);
        // one occupies the single thread, one fills the single queue slot
        service.previewAsync(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request);
        awaitScanStarted();
        service.previewAsync(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request);

        try {
            assertThatThrownBy(() -> service.previewAsync(TENANT_ID, AGENT_PROFILE_ID, APPLICATION_PROFILE_ID, request))
                    .isInstanceOf(ThingsboardException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } finally {
            release.countDown();
            service.shutdown();
        }
    }

    private void awaitScanStarted() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (org.mockito.Mockito.mockingDetails(profileService).getInvocations().stream()
                    .anyMatch(inv -> "findProfileInfoById".equals(inv.getMethod().getName()))) {
                return;
            }
            Thread.sleep(20);
        }
    }

    // ==================== step inputs round trip ====================

    @Test
    void processBulkOperation_decodedStepInputsReachBothTheEventRequestAndTheSyntheticErrorEvent() throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);

        AgentApplicationInfo eligible = createApplication();
        AgentApplicationInfo skipped = createApplication();
        skipped.setTemplateVersion("0.0.0-mismatch");

        UUID stepId = UUID.randomUUID();
        BackupVolumesStepState stepState = new BackupVolumesStepState();
        stepState.setBackupVolumes(new StepField<>(List.of("data", "logs")));

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(eligible, skipped), 1, 2, false));
        when(tbAgentApplicationService.execActionEvent(any(), eq(eligible.getId()), any()))
                .thenReturn(new AgentAppEvent());

        AgentBulkOperationMsg msg = buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE)
                .toBuilder()
                .setStepInputs(JacksonUtil.toString(Map.of(stepId, stepState)))
                .build();

        service.processBulkOperation(msg);

        ArgumentCaptor<AgentAppEventRequest> requestCaptor = ArgumentCaptor.forClass(AgentAppEventRequest.class);
        verify(tbAgentApplicationService).execActionEvent(eq(TENANT_ID), eq(eligible.getId()), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStepInputs()).containsOnlyKeys(stepId);
        assertThat(((BackupVolumesStepState) requestCaptor.getValue().getStepInputs().get(stepId)).getBackupVolumes().getValue())
                .containsExactly("data", "logs");

        ArgumentCaptor<List<AgentAppEvent>> eventCaptor = ArgumentCaptor.captor();
        verify(agentAppEventService).saveAll(eq(TENANT_ID), eventCaptor.capture());
        assertThat(eventCaptor.getValue().get(0).getStepStates()).containsOnlyKeys(stepId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "null"})
    void processBulkOperation_blankStepInputsDecodeToNoStepStates(String rawStepInputs) throws Exception {
        AgentBulkActionId bulkActionId = new AgentBulkActionId(UUID.randomUUID());
        AgentBulkAction bulkAction = createBulkAction(bulkActionId, AgentBulkActionStatus.QUEUED);
        AgentApplicationInfo app = createApplication();

        when(agentBulkActionService.findById(any(), eq(bulkActionId))).thenReturn(bulkAction);
        when(profileService.findProfileInfoById(any(), any())).thenReturn(createProfile());
        stubApps(app);
        when(tbAgentApplicationService.execActionEvent(any(), eq(app.getId()), any())).thenReturn(new AgentAppEvent());

        service.processBulkOperation(buildProtoMsg(bulkActionId, AgentAppEventActionType.UPDATE)
                .toBuilder().setStepInputs(rawStepInputs).build());

        ArgumentCaptor<AgentAppEventRequest> requestCaptor = ArgumentCaptor.forClass(AgentAppEventRequest.class);
        verify(tbAgentApplicationService).execActionEvent(eq(TENANT_ID), eq(app.getId()), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStepInputs()).isNull();
    }

    // ==================== Helpers ====================

    private void stubApps(AgentApplicationInfo... apps) {
        when(applicationDao.findByApplicationProfileIdAndAgentProfileId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(apps), 1, apps.length, false));
    }

    private AgentBulkAction lastSavedBulkAction() {
        ArgumentCaptor<AgentBulkAction> captor = ArgumentCaptor.forClass(AgentBulkAction.class);
        verify(agentBulkActionService, atLeastOnce()).save(any(), captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    private BulkOperationRequest createRequest(AgentAppEventActionType actionType) {
        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(actionType);
        return request;
    }

    private AgentAppProfileInfo createProfile() {
        AgentAppProfileInfo profile = new AgentAppProfileInfo();
        profile.setId(APPLICATION_PROFILE_ID);
        profile.setTemplateVersion(TEMPLATE_VERSION);
        return profile;
    }

    private AgentBulkAction createBulkAction(AgentBulkActionId id, AgentBulkActionStatus status) {
        AgentBulkAction action = new AgentBulkAction(id);
        action.setTenantId(TENANT_ID);
        action.setAgentProfileId(AGENT_PROFILE_ID.getId());
        action.setApplicationProfileId(APPLICATION_PROFILE_ID.getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(status);
        return action;
    }

    private AgentApplicationInfo createApplication() {
        AgentApplicationInfo app = new AgentApplicationInfo();
        app.setId(new AgentApplicationId(UUID.randomUUID()));
        app.setTenantId(TENANT_ID);
        app.setAgentId(new AgentId(UUID.randomUUID()));
        app.setName("test-app");
        app.setTemplateVersion(TEMPLATE_VERSION);
        return app;
    }

    private AgentBulkOperationMsg buildProtoMsg(AgentAppEventActionType actionType) {
        return buildProtoMsg(new AgentBulkActionId(UUID.randomUUID()), actionType);
    }

    private AgentBulkOperationMsg buildProtoMsg(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType) {
        UUID tenantUuid = TENANT_ID.getId();
        UUID bulkUuid = bulkActionId.getId();
        UUID agentProfileUuid = AGENT_PROFILE_ID.getId();
        UUID applicationProfileUuid = APPLICATION_PROFILE_ID.getId();

        return AgentBulkOperationMsg.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setBulkActionIdMSB(bulkUuid.getMostSignificantBits())
                .setBulkActionIdLSB(bulkUuid.getLeastSignificantBits())
                .setAgentProfileIdMSB(agentProfileUuid.getMostSignificantBits())
                .setAgentProfileIdLSB(agentProfileUuid.getLeastSignificantBits())
                .setApplicationProfileIdMSB(applicationProfileUuid.getMostSignificantBits())
                .setApplicationProfileIdLSB(applicationProfileUuid.getLeastSignificantBits())
                .setActionType(actionType.name())
                .build();
    }
}
