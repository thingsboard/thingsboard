// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.ErrorOrigin;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.transport.TransportProtos.AgentAppEventNotificationProto;
import org.thingsboard.server.service.agent.AgentAppArgumentResolver;
import org.thingsboard.server.service.agent.AgentRpcService;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultAgentEventProcessorTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());

    @Mock
    private AgentRpcService agentRpcService;
    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentApplicationService appService;
    @Mock
    private AgentEventWatchdog eventWatchdog;
    @Mock
    private AgentAppEventStepsResolver eventStepsResolver;
    @Mock
    private AgentEventErrorHandler eventErrorHandler;
    @Mock
    private AgentSessionRegistry sessions;
    @Mock
    private AgentAppArgumentResolver argumentResolver;
    @Mock
    private AgentScopedEventProcessor agentScopedProcessor;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private DefaultAgentEventProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    void anAgentScopedNotificationNeverLooksUpAnApplication() {
        when(sessions.hasSession(AGENT_ID)).thenReturn(true);

        processor.onEventNotification(notification(AgentAppEventActionType.AGENT_UPGRADE, false));

        verify(agentScopedProcessor).resumeOrDispatch(TENANT_ID, AGENT_ID);
        verify(appService, never()).findById(any(), any());
    }

    @Test
    void anAppNotificationIsSkippedWhileAnAgentEventIsActive() {
        when(sessions.hasSession(AGENT_ID)).thenReturn(true);
        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        processor.onEventNotification(notification(AgentAppEventActionType.RESTART, false));

        verify(appService, never()).findById(any(), any());
        verify(agentScopedProcessor, never()).resumeOrDispatch(any(), any());
    }

    /** The delivered branch bypasses dispatchNextEventIfPossible, so it needs its own gate. */
    @Test
    void aDeliveredAppNotificationIsAlsoSkippedWhileAnAgentEventIsActive() {
        when(sessions.hasSession(AGENT_ID)).thenReturn(true);
        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        processor.onEventNotification(notification(AgentAppEventActionType.RESTART, true));

        verify(appService, never()).findById(any(), any());
    }

    @Test
    void anUnknownActionTypeIsTreatedAsAppScoped() {
        when(sessions.hasSession(AGENT_ID)).thenReturn(true);
        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        processor.onEventNotification(notificationOfType("SOMETHING_NEW", false));

        verify(agentScopedProcessor, never()).resumeOrDispatch(any(), any());
    }

    @Test
    void reconnectResumesTheAgentEventInsteadOfFanningOutApps() {
        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        processor.resumeEventsOnReconnect(TENANT_ID, AGENT_ID);

        verify(agentScopedProcessor).resumeOrDispatch(TENANT_ID, AGENT_ID);
        verify(appEventService, never()).findApplicationIdsWithOutstandingEvents(any());
        verify(appService, never()).findByAgentId(any(), any(), any());
    }

    @Test
    void reconnectFansOutAppsWhenNoAgentEventIsActive() {
        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(false);
        when(appEventService.findApplicationIdsWithOutstandingEvents(AGENT_ID)).thenReturn(List.of());

        processor.resumeEventsOnReconnect(TENANT_ID, AGENT_ID);

        verify(appEventService).findApplicationIdsWithOutstandingEvents(AGENT_ID);
        verify(agentScopedProcessor, never()).resumeOrDispatch(any(), any());
    }

    /** Reconnect must not page through every application the agent owns - only the ones with work queued. */
    @Test
    void reconnectOnlyTouchesApplicationsWithOutstandingEvents() {
        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(false);
        AgentApplicationId appId = new AgentApplicationId(UUID.randomUUID());
        when(appEventService.findApplicationIdsWithOutstandingEvents(AGENT_ID)).thenReturn(List.of(appId));
        when(appService.findById(TENANT_ID, appId)).thenReturn(null);

        processor.resumeEventsOnReconnect(TENANT_ID, AGENT_ID);

        verify(appService).findById(TENANT_ID, appId);
        verify(appService, never()).findByAgentId(any(), any(), any());
    }

    @Test
    void aCancelNotificationIsLetThroughEvenDuringAnUpgrade() {
        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(true);

        processor.onEventNotification(notification(AgentAppEventActionType.RESTART, false)
                .toBuilder().setCancelled(true).build());

        verify(eventErrorHandler).onFailure(eq(TENANT_ID), eq(AGENT_ID), any(), any(), any());
    }

    // ==================== Custom argument resolution before the first step ====================

    @Test
    void customArgumentsAreResolvedAndPersistedBeforeTheFirstStepIsSent() {
        AgentApplication application = applicationWithPendingEvent();
        when(argumentResolver.resolve(TENANT_ID, application)).thenReturn(Futures.immediateFuture(Map.of("PORT", "8080")));

        processor.processNextEventForApp(TENANT_ID, AGENT_ID, application);

        verify(appEventService).updateResolvedArguments(EVENT_ID, Map.of("PORT", "8080"));
        verify(eventErrorHandler, never()).onFailure(any(), any(), any(), any(), any());
    }

    @Test
    void aFailedArgumentResolutionFailsTheEventInsteadOfDroppingIt() {
        AgentApplication application = applicationWithPendingEvent();
        when(argumentResolver.resolve(TENANT_ID, application))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("device has no attribute PORT")));

        processor.processNextEventForApp(TENANT_ID, AGENT_ID, application);

        verify(eventErrorHandler).onFailure(TENANT_ID, AGENT_ID, EVENT_ID, ErrorOrigin.SERVER, "device has no attribute PORT");
        verify(appEventService, never()).updateResolvedArguments(any(), any());
    }

    @Test
    void theResolverIsNotConsultedWhenNoStepDeclaresCustomArguments() {
        AgentApplication application = applicationWithPendingEvent(new ComposeStartStep());

        processor.processNextEventForApp(TENANT_ID, AGENT_ID, application);

        verify(argumentResolver, never()).resolve(any(), any());
        verify(appEventService, never()).updateResolvedArguments(any(), any());
    }

    private AgentApplication applicationWithPendingEvent() {
        ComposeStep composeStep = new ComposeStep();
        composeStep.setId(UUID.randomUUID());
        return applicationWithPendingEvent(composeStep);
    }

    private AgentApplication applicationWithPendingEvent(AgentAppStep firstStep) {
        if (firstStep.getId() == null) {
            firstStep.setId(UUID.randomUUID());
        }
        AgentApplication application = new AgentApplication();
        application.setId(APP_ID);
        application.setTenantId(TENANT_ID);
        application.setAgentId(AGENT_ID);
        application.setConfig(new DockerComposeConfig());

        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setApplicationId(APP_ID);
        event.setAgentId(AGENT_ID);
        event.setActionType(AgentAppEventActionType.INSTALL);

        when(appEventService.hasActiveOrPendingAgentEvent(AGENT_ID)).thenReturn(false);
        when(appEventService.hasActiveEventForApplication(APP_ID)).thenReturn(false);
        when(appEventService.findOldestPendingByApplicationId(APP_ID)).thenReturn(Optional.of(event));
        when(appEventService.markDelivered(EVENT_ID)).thenReturn(true);
        when(eventStepsResolver.resolveSteps(application, AgentAppEventActionType.INSTALL)).thenReturn(List.of(firstStep));
        return application;
    }

    private AgentAppEventNotificationProto notification(AgentAppEventActionType actionType, boolean delivered) {
        return notificationOfType(actionType.name(), delivered);
    }

    private AgentAppEventNotificationProto notificationOfType(String actionType, boolean delivered) {
        UUID eventId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        return AgentAppEventNotificationProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setAgentIdMSB(AGENT_ID.getId().getMostSignificantBits())
                .setAgentIdLSB(AGENT_ID.getId().getLeastSignificantBits())
                .setApplicationIdMSB(applicationId.getMostSignificantBits())
                .setApplicationIdLSB(applicationId.getLeastSignificantBits())
                .setEventIdMSB(eventId.getMostSignificantBits())
                .setEventIdLSB(eventId.getLeastSignificantBits())
                .setActionType(actionType)
                .setDelivered(delivered)
                .build();
    }
}
