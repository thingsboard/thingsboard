// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAgentEventWatchdogTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());

    @Mock
    private AgentAppEventService agentAppEventService;
    @Mock
    private AgentSessionRegistry agentSessionRegistry;
    @Mock
    private AgentAppEventStepsResolver eventStepsResolver;
    @Mock
    private AgentSession session;
    @Mock
    private AgentEventResender resender;

    private DefaultAgentEventWatchdog watchdog;

    @BeforeEach
    void setUp() {
        watchdog = new DefaultAgentEventWatchdog(agentAppEventService, agentSessionRegistry, eventStepsResolver);
        ReflectionTestUtils.setField(watchdog, "watchdogInitialDelayMs", 1000L);
    }

    @Test
    void schedule_noActiveSession_doesNotScheduleWatchdog() {
        when(agentSessionRegistry.getByAgentId(AGENT_ID)).thenReturn(null);

        watchdog.schedule(application(), event(AgentProcessingStatus.QUEUED, 0L), resender);

        verify(agentSessionRegistry).getByAgentId(AGENT_ID);
        verifyNoInteractions(agentAppEventService, eventStepsResolver, resender);
    }

    @Test
    void staleEvent_nullCurrentStep_doesNotResend() {
        AgentApplication application = application();
        AgentAppEvent event = event(AgentProcessingStatus.QUEUED, 0L); // updatedTime 0 -> stale vs now
        when(agentSessionRegistry.getByAgentId(AGENT_ID)).thenReturn(session);
        List<Runnable> tasks = captureScheduledTasks();
        // empty step list -> findByStepId returns null for any currentStepId
        when(eventStepsResolver.resolveSteps(eq(application), any())).thenReturn(Collections.<AgentAppStep>emptyList());
        when(agentAppEventService.findById(eq(TENANT_ID), eq(EVENT_ID))).thenReturn(event);

        watchdog.schedule(application, event, resender);
        tasks.get(0).run();

        verify(resender, never()).resendCurrentStep(any(), any(), any());
        verify(resender, never()).onError(any(), any());
        // checkStaleness completed normally -> only the initial scheduling, no reschedule
        verify(session, times(1)).scheduleEventWatchdog(any(), any(), any(), anyLong(), any());
    }

    @Test
    void watchdogCheckKeepsFailing_marksErrorAfterMaxRetries() {
        AgentApplication application = application();
        AgentAppEvent event = event(AgentProcessingStatus.QUEUED, 0L);
        when(agentSessionRegistry.getByAgentId(AGENT_ID)).thenReturn(session);
        List<Runnable> tasks = captureScheduledTasks();
        when(agentAppEventService.findById(any(), any())).thenThrow(new RuntimeException("db down"));

        watchdog.schedule(application, event, resender);
        int i = 0;
        while (i < tasks.size() && i < 10) {
            tasks.get(i).run();
            i++;
        }

        // initial attempt (count 0) + 3 reschedules (counts 1,2,3) = 4 schedule calls
        verify(session, times(4)).scheduleEventWatchdog(any(), any(), any(), anyLong(), any());
        verify(resender, times(1)).onError(EVENT_ID, application);
    }

    private List<Runnable> captureScheduledTasks() {
        List<Runnable> tasks = new ArrayList<>();
        doAnswer(inv -> {
            tasks.add(inv.getArgument(2));
            return null;
        }).when(session).scheduleEventWatchdog(any(), any(), any(), anyLong(), any());
        return tasks;
    }

    private AgentApplication application() {
        AgentApplication application = new AgentApplication();
        application.setTenantId(TENANT_ID);
        application.setAgentId(AGENT_ID);
        return application;
    }

    private AgentAppEvent event(AgentProcessingStatus processingStatus, long updatedTime) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setProcessingStatus(processingStatus);
        event.setUpdatedTime(updatedTime);
        event.setCurrentStepId(UUID.randomUUID());
        return event;
    }
}
