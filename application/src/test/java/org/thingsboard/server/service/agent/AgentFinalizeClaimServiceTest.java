// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.FinalizeClaim;
import org.thingsboard.server.gen.agent.v1.FinalizeClaimResult;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.service.agent.event.AgentEventWatchdog;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The claim is the only thing that authorizes one instance to destroy the other, so these
 * cover the whole decision table: who may win before and after the deadline, that the
 * winner is single-shot, and that re-granting to the current winner is unconditional.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AgentFinalizeClaimServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final String OLD_CONTAINER = "aaaa0000000000000000000000000000000000000000000000000000000000a1";
    private static final String NEW_CONTAINER = "bbbb0000000000000000000000000000000000000000000000000000000000b2";

    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentRpcService agentRpcService;
    @Mock
    private AgentEventWatchdog eventWatchdog;
    @Mock
    private ScheduledExecutorService scheduler;

    @InjectMocks
    private AgentFinalizeClaimService claimService;

    @BeforeEach
    void setUp() throws Exception {
        when(appEventService.claimFinalizeWinner(any(), any())).thenReturn(true);
        when(agentRpcService.push(any(), any())).thenReturn(true);
        when(eventWatchdog.getScheduler()).thenReturn(scheduler);
    }

    /** The verdict is already committed, so a dropped result must not simply be forgotten. */
    @Test
    void aDroppedVerdictIsRetried() throws Exception {
        givenEvent(null, inTheFuture(), AgentProcessingStatus.PROCESSING);
        when(agentRpcService.push(any(), any())).thenReturn(false);

        claim(NEW_CONTAINER);

        verify(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void beforeTheDeadlineOnlyTheNewInstanceMayWin() {
        givenEvent(null, inTheFuture(), AgentProcessingStatus.PROCESSING);

        assertThat(claim(NEW_CONTAINER)).isTrue();
        verify(appEventService).claimFinalizeWinner(EVENT_ID, NEW_CONTAINER);
    }

    @Test
    void beforeTheDeadlineThePreviousInstanceIsDenied() {
        givenEvent(null, inTheFuture(), AgentProcessingStatus.PROCESSING);

        assertThat(claim(OLD_CONTAINER)).isFalse();
        verify(appEventService, never()).claimFinalizeWinner(any(), any());
    }

    @Test
    void afterTheDeadlineOnlyThePreviousInstanceMayWin() {
        givenEvent(null, inThePast(), AgentProcessingStatus.PROCESSING);

        assertThat(claim(OLD_CONTAINER)).isTrue();
        verify(appEventService).claimFinalizeWinner(EVENT_ID, OLD_CONTAINER);
    }

    @Test
    void afterTheDeadlineTheNewInstanceIsDenied() {
        givenEvent(null, inThePast(), AgentProcessingStatus.PROCESSING);

        assertThat(claim(NEW_CONTAINER)).isFalse();
    }

    /**
     * A restarted winner must always be able to finish, so the deadline gates only the
     * first assignment. Denying it would leave the event unfinishable.
     */
    @Test
    void theCurrentWinnerIsAlwaysReGranted() {
        givenEvent(NEW_CONTAINER, inThePast(), AgentProcessingStatus.ERROR);

        assertThat(claim(NEW_CONTAINER)).isTrue();
        verify(appEventService).claimFinalizeWinner(EVENT_ID, NEW_CONTAINER);
    }

    /**
     * A lease flip would let two instances each hold a grant once, so the winner is never
     * re-assigned — not even after the deadline moves eligibility to the other side.
     */
    @Test
    void theWinnerIsNeverReassigned() {
        givenEvent(NEW_CONTAINER, inThePast(), AgentProcessingStatus.PROCESSING);

        assertThat(claim(OLD_CONTAINER)).isFalse();
        verify(appEventService, never()).claimFinalizeWinner(any(), any());
    }

    @Test
    void aTerminalEventGrantsNobodyNew() {
        givenEvent(null, inTheFuture(), AgentProcessingStatus.FINISHED);

        assertThat(claim(NEW_CONTAINER)).isFalse();
    }

    @Test
    void anUnknownEventIsDenied() {
        when(appEventService.findById(eq(TENANT_ID), eq(EVENT_ID))).thenReturn(null);

        assertThat(claim(NEW_CONTAINER)).isFalse();
    }

    @Test
    void anAppScopedEventIsDenied() {
        AgentAppEvent event = event(null, inTheFuture(), AgentProcessingStatus.PROCESSING);
        event.setActionType(AgentAppEventActionType.UPDATE);
        when(appEventService.findById(eq(TENANT_ID), eq(EVENT_ID))).thenReturn(event);

        assertThat(claim(NEW_CONTAINER)).isFalse();
    }

    @Test
    void anEventOfAnotherAgentIsDenied() {
        AgentAppEvent event = event(null, inTheFuture(), AgentProcessingStatus.PROCESSING);
        event.setAgentId(new AgentId(UUID.randomUUID()));
        when(appEventService.findById(eq(TENANT_ID), eq(EVENT_ID))).thenReturn(event);

        assertThat(claim(NEW_CONTAINER)).isFalse();
    }

    @Test
    void anEmptyContainerIdIsDenied() {
        givenEvent(null, inTheFuture(), AgentProcessingStatus.PROCESSING);

        assertThat(claim("")).isFalse();
    }

    /** The verdict is echoed with the container it answers, so a claimant can tell them apart. */
    @Test
    void theVerdictEchoesTheClaimingContainer() {
        givenEvent(null, inTheFuture(), AgentProcessingStatus.PROCESSING);

        claim(NEW_CONTAINER);

        assertThat(pushedResult().getContainerId()).isEqualTo(NEW_CONTAINER);
        assertThat(pushedResult().getCommandId().getIdMSB()).isEqualTo(EVENT_ID.getId().getMostSignificantBits());
    }

    private void givenEvent(String winner, Long deadline, AgentProcessingStatus status) {
        when(appEventService.findById(eq(TENANT_ID), eq(EVENT_ID))).thenReturn(event(winner, deadline, status));
    }

    private AgentAppEvent event(String winner, Long deadline, AgentProcessingStatus status) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setAgentId(AGENT_ID);
        event.setTenantId(TENANT_ID);
        event.setActionType(AgentAppEventActionType.AGENT_UPGRADE);
        event.setWinnerContainerId(winner);
        event.setFinalizeDeadlineTs(deadline);
        event.setProcessingStatus(status);
        event.setContextMetadata(Map.of(
                AgentUpgradeKeys.OLD_CONTAINER_ID, OLD_CONTAINER,
                AgentUpgradeKeys.NEW_CONTAINER_ID, NEW_CONTAINER));
        return event;
    }

    private boolean claim(String containerId) {
        claimService.onFinalizeClaim(TENANT_ID, AGENT_ID, FinalizeClaim.newBuilder()
                .setCommandId(CommandId.newBuilder()
                        .setIdMSB(EVENT_ID.getId().getMostSignificantBits())
                        .setIdLSB(EVENT_ID.getId().getLeastSignificantBits())
                        .build())
                .setContainerId(containerId)
                .build());
        return pushedResult().getGranted();
    }

    private FinalizeClaimResult pushedResult() {
        ArgumentCaptor<ServerToAgent> captor = ArgumentCaptor.forClass(ServerToAgent.class);
        try {
            verify(agentRpcService, atLeastOnce()).push(eq(AGENT_ID), captor.capture());
        } catch (AgentSessionNotFoundException e) {
            throw new AssertionError("push must not throw in a verify", e);
        }
        ServerToAgent msg = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(msg.hasFinalizeClaimResult()).isTrue();
        return msg.getFinalizeClaimResult();
    }

    private static long inTheFuture() {
        return System.currentTimeMillis() + 60_000;
    }

    private static long inThePast() {
        return System.currentTimeMillis() - 60_000;
    }
}
