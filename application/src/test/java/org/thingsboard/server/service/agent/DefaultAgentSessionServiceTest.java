// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.gen.agent.v1.Hello;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;
import org.thingsboard.server.service.agent.session.AgentSessionState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Hello-time triage decides whether a container that reports the id of an upgrade's new
 * instance is the agent, a mid-flight takeover, or a copy of a dead upgrade that must never
 * become a routing target. Disconnect bookkeeping has to stay ownership-aware, because a
 * self-upgrade routinely leaves a stale session behind on another node.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultAgentSessionServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentAppEventId EVENT_ID = new AgentAppEventId(UUID.randomUUID());
    private static final String OLD_CONTAINER = "aaaa0000000000000000000000000000000000000000000000000000000000a1";
    private static final String NEW_CONTAINER = "bbbb0000000000000000000000000000000000000000000000000000000000b2";
    private static final String ROUTING_KEY = "routing-key";
    private static final String ROUTING_SECRET = "routing-secret";
    private static final String MY_SERVICE_ID = "core-1";
    private static final String OTHER_SERVICE_ID = "core-2";

    @Mock
    private AgentService agentService;
    @Mock
    private AgentSessionRegistry sessions;
    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    private TbTransactionalCache<AgentId, String> agentIdServiceIdCache;
    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private AgentStateService agentStateService;

    @InjectMocks
    private DefaultAgentSessionService sessionService;

    // --- hello-time triage ---

    @Test
    void anOrdinaryAgentIsAccepted() {
        givenAgent();
        when(appEventService.findLatestAgentEventByAgentId(AGENT_ID)).thenReturn(Optional.empty());

        assertThat(connect(OLD_CONTAINER)).isEmpty();
        verify(sessions).registerOrReplace(eq(AGENT_ID), eq(TENANT_ID), any());
        verify(agentStateService).onAgentConnect(any(), anyLong(), anyString(), eq(OLD_CONTAINER));
    }

    @Test
    void aTakeoverInstanceIsAcceptedWhileItsUpgradeIsStillRunning() {
        givenAgent();
        givenLatestEvent(NEW_CONTAINER, null, AgentProcessingStatus.PROCESSING, inThePast());

        assertThat(connect(NEW_CONTAINER)).isEmpty();
        verify(appEventService, never()).updateActivityUnguarded(any(), anyString());
    }

    @Test
    void theWinnerOfASweptUpgradeIsAcceptedAndTheEventIsReconciled() {
        givenAgent();
        givenLatestEvent(NEW_CONTAINER, NEW_CONTAINER, AgentProcessingStatus.ERROR, inThePast());

        assertThat(connect(NEW_CONTAINER)).isEmpty();
        verify(appEventService).updateActivityUnguarded(EVENT_ID, AgentUpgradeKeys.RECONCILED_ACTIVITY);
    }

    /**
     * The survivor keeps reporting the new container id forever, so every later restart lands
     * here. A cleanly finished upgrade must not be relabelled as a timeout recovery.
     */
    @Test
    void aFinishedUpgradeIsNeverRelabelled() {
        givenAgent();
        givenLatestEvent(NEW_CONTAINER, NEW_CONTAINER, AgentProcessingStatus.FINISHED, inThePast());

        assertThat(connect(NEW_CONTAINER)).isEmpty();
        verify(appEventService, never()).updateActivityUnguarded(any(), anyString());
    }

    @Test
    void aFailureBeforeTheDeadlineIsNotCalledATimeout() {
        givenAgent();
        givenLatestEvent(NEW_CONTAINER, NEW_CONTAINER, AgentProcessingStatus.ERROR, inTheFuture());

        assertThat(connect(NEW_CONTAINER)).isEmpty();
        verify(appEventService, never()).updateActivityUnguarded(any(), anyString());
    }

    @Test
    void anAlreadyReconciledEventIsNotAnnotatedTwice() {
        givenAgent();
        AgentAppEvent event = event(NEW_CONTAINER, NEW_CONTAINER, AgentProcessingStatus.ERROR, inThePast());
        event.setCurrentActivity(AgentUpgradeKeys.RECONCILED_ACTIVITY);
        when(appEventService.findLatestAgentEventByAgentId(AGENT_ID)).thenReturn(Optional.of(event));

        assertThat(connect(NEW_CONTAINER)).isEmpty();
        verify(appEventService, never()).updateActivityUnguarded(any(), anyString());
    }

    /**
     * A copy that never won its claim would otherwise become a routing target the moment the
     * gate lifts, since routing is last-writer-wins.
     */
    @Test
    void aNeverGrantedCopyOfADeadUpgradeIsDenied() {
        givenAgent();
        givenLatestEvent(NEW_CONTAINER, OLD_CONTAINER, AgentProcessingStatus.ERROR, inThePast());

        Optional<Status> denial = connect(NEW_CONTAINER);

        assertThat(denial).isPresent();
        assertThat(denial.get().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(denial.get().getDescription()).isEqualTo(DefaultAgentSessionService.TAKEOVER_DEMOTED);
        verify(sessions, never()).registerOrReplace(any(), any(), any());
    }

    @Test
    void anAgentWithoutAContainerIdIsNotTriaged() {
        givenAgent();

        assertThat(connect("")).isEmpty();
        verify(appEventService, never()).findLatestAgentEventByAgentId(any());
    }

    // --- disconnect bookkeeping ---

    @Test
    void aStaleDisconnectLeavesAnotherNodesSessionAlone() {
        AgentSession session = session();
        when(sessions.removeIfSame(session)).thenReturn(true);
        when(serviceInfoProvider.getServiceId()).thenReturn(MY_SERVICE_ID);
        when(agentIdServiceIdCache.get(AGENT_ID)).thenReturn(wrapper(OTHER_SERVICE_ID));

        sessionService.onCompleted(session);

        verify(agentIdServiceIdCache, never()).evict(any(AgentId.class));
        verify(agentStateService, never()).onAgentDisconnect(any(), anyLong());
    }

    @Test
    void theOwningNodeClearsTheSessionState() {
        AgentSession session = session();
        when(sessions.removeIfSame(session)).thenReturn(true);
        when(serviceInfoProvider.getServiceId()).thenReturn(MY_SERVICE_ID);
        when(agentIdServiceIdCache.get(AGENT_ID)).thenReturn(wrapper(MY_SERVICE_ID));

        sessionService.onCompleted(session);

        verify(agentIdServiceIdCache).evict(AGENT_ID);
        verify(agentStateService).onAgentDisconnect(any(), anyLong());
    }

    @Test
    void aTakeoverBetweenTheOwnerCheckAndTheEvictSkipsTheDisconnectBookkeeping() {
        AgentSession session = session();
        when(sessions.removeIfSame(session)).thenReturn(true);
        when(serviceInfoProvider.getServiceId()).thenReturn(MY_SERVICE_ID);
        when(agentIdServiceIdCache.get(AGENT_ID))
                .thenReturn(wrapper(MY_SERVICE_ID))
                .thenReturn(wrapper(OTHER_SERVICE_ID));

        sessionService.onCompleted(session);

        verify(agentStateService, never()).onAgentDisconnect(any(), anyLong());
    }

    @Test
    void aSupersededSessionSkipsTheDisconnectBookkeepingEntirely() {
        AgentSession session = session();
        when(sessions.removeIfSame(session)).thenReturn(false);

        sessionService.onCompleted(session);

        verify(agentIdServiceIdCache, never()).get(any(AgentId.class));
        verify(agentIdServiceIdCache, never()).evict(any(AgentId.class));
        verify(agentStateService, never()).onAgentDisconnect(any(), anyLong());
    }

    @Test
    void aCacheMissIsTreatedAsOwnedAndClearsTheSessionState() {
        AgentSession session = session();
        when(sessions.removeIfSame(session)).thenReturn(true);
        when(agentIdServiceIdCache.get(AGENT_ID)).thenReturn(null);

        sessionService.onCompleted(session);

        verify(agentIdServiceIdCache).evict(AGENT_ID);
        verify(agentStateService).onAgentDisconnect(any(), anyLong());
    }

    // --- fixtures ---

    private Optional<Status> connect(String containerId) {
        return sessionService.onConnected(emptySession(), Hello.newBuilder()
                .setRoutingKey(ROUTING_KEY)
                .setRoutingSecret(ROUTING_SECRET)
                .setAgentVersion("thingsboard/tb-remote-agent:test")
                .setContainerId(containerId)
                .build());
    }

    private void givenAgent() {
        when(agentService.findAgentByRoutingKey(eq(TenantId.SYS_TENANT_ID), eq(ROUTING_KEY))).thenReturn(agent());
        when(serviceInfoProvider.getServiceId()).thenReturn(MY_SERVICE_ID);
    }

    private void givenLatestEvent(String newContainerId, String winner, AgentProcessingStatus status, long deadline) {
        when(appEventService.findLatestAgentEventByAgentId(AGENT_ID))
                .thenReturn(Optional.of(event(newContainerId, winner, status, deadline)));
    }

    private AgentAppEvent event(String newContainerId, String winner, AgentProcessingStatus status, long deadline) {
        AgentAppEvent event = new AgentAppEvent();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setAgentId(AGENT_ID);
        event.setActionType(AgentAppEventActionType.AGENT_UPGRADE);
        event.setProcessingStatus(status);
        event.setWinnerContainerId(winner);
        event.setFinalizeDeadlineTs(deadline);
        event.setContextMetadata(Map.of(
                AgentUpgradeKeys.OLD_CONTAINER_ID, OLD_CONTAINER,
                AgentUpgradeKeys.NEW_CONTAINER_ID, newContainerId));
        return event;
    }

    private Agent agent() {
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setRoutingKey(ROUTING_KEY);
        agent.setSecret(ROUTING_SECRET);
        return agent;
    }

    /** A session whose state the service under test fills in, as the gRPC layer would. */
    private AgentSession emptySession() {
        AgentSession session = mock(AgentSession.class);
        when(session.getState()).thenReturn(new AgentSessionState());
        return session;
    }

    private AgentSession session() {
        AgentSession session = emptySession();
        session.getState().setAgent(agent());
        return session;
    }

    private TbCacheValueWrapper<String> wrapper(String value) {
        return () -> value;
    }

    private static long inTheFuture() {
        return System.currentTimeMillis() + 60_000;
    }

    private static long inThePast() {
        return System.currentTimeMillis() - 60_000;
    }
}
