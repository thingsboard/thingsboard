// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cache.SimpleTbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.EdgeSessionsHolder;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;

@ExtendWith(MockitoExtension.class)
public class EdgeGrpcServiceTest {

    private static final String THIS_NODE = "tb-core-1";
    private static final String OTHER_NODE = "tb-core-2";

    @Mock
    private TbTransactionalCache<EdgeId, String> edgeIdServiceIdCache;

    @Mock
    private TbServiceInfoProvider serviceInfoProvider;

    @Mock
    private PartitionService partitionService;

    @Mock
    private TelemetrySubscriptionService tsSubService;

    @Mock
    private TbClusterService clusterService;

    @Mock
    private ApplicationContext applicationContext;

    private EdgeSessionsHolder sessions;
    private EdgeGrpcService edgeGrpcService;

    private EdgeId edgeId;
    private Edge edge;

    @BeforeEach
    public void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        edge = new Edge(edgeId);
        edge.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        edge.setName("test-edge");
        // A real holder, so the claim-once removal is exercised rather than stubbed.
        sessions = new EdgeSessionsHolder();
        edgeGrpcService = new EdgeGrpcService(sessions, applicationContext, clusterService,
                serviceInfoProvider, tsSubService, edgeIdServiceIdCache, partitionService);
    }

    // --- activity-state write guard on disconnect ---

    @Test
    public void givenEdgeReconnectedToAnotherNode_whenDisconnect_thenInactiveStateNotPersisted() {
        // A newer session on another node already set active=true. This stale session's disconnect must not
        // clobber it - otherwise the edge stays marked inactive while connected, and the server silently
        // drops every assignment event for it.
        EdgeSessionState state = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE, OTHER_NODE));

        onEdgeDisconnect(state.getSessionId());

        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenEdgeOwnedByThisNode_whenDisconnect_thenInactiveStateIsPersisted() {
        EdgeSessionState state = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));

        onEdgeDisconnect(state.getSessionId());

        verify(tsSubService, times(1)).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenNoCacheEntry_whenDisconnect_thenInactiveStateIsPersisted() {
        // Ownership unknown (entry absent or expired): keep the pre-guard behaviour, otherwise a genuinely
        // disconnected edge would stay marked active forever.
        EdgeSessionState state = registerSession();
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);

        onEdgeDisconnect(state.getSessionId());

        verify(tsSubService, times(1)).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenOwnerNodeNoLongerInCluster_whenDisconnect_thenInactiveStateIsPersisted() {
        // The recorded owner died without evicting its claim. The edge sessions cache has no TTL by default
        // and only the owner ever evicts, so honouring a dead claim would leave the edge active forever.
        EdgeSessionState state = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE));

        onEdgeDisconnect(state.getSessionId());

        verify(tsSubService, times(1)).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenSessionReplacedDuringDisconnect_whenStaleCallbackRuns_thenLiveSessionKept() {
        // Reproduces the lookup/removal interleaving on a single node: onEdgeConnect swaps in a new session
        // right after the stale callback's id check passes. The stale callback must not evict or tear down
        // the live session, nor clear the activity state.
        EdgeSessionState staleState = stateFor(edge);
        EdgeSessionState liveState = stateFor(edge);
        EdgeGrpcSessionManager live = mock(EdgeGrpcSessionManager.class);
        when(live.getState()).thenReturn(liveState);
        EdgeGrpcSessionManager stale = mock(EdgeGrpcSessionManager.class);
        when(stale.getState()).thenAnswer(invocation -> {
            sessions.put(live);
            return staleState;
        });
        sessions.put(stale);

        onEdgeDisconnect(staleState.getSessionId());

        assertThat(sessions.getByEdgeId(edgeId)).isSameAs(live);
        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenSessionReplacedOnThisNodeAfterRemoval_whenStaleCallbackRuns_thenNoDisconnectSideEffects() {
        // The stale callback wins the compare-and-remove, then tearing its own session down takes a while
        // (for the Kafka session manager this stops a consumer). The edge reconnects to THIS node in that gap,
        // so the ownership cache legitimately names us and isOwnedByAnotherNode cannot help. None of the
        // disconnect side effects may run: not the flag, not the timestamp, not the DISCONNECT_EVENT push.
        EdgeSessionState staleState = stateFor(edge);
        EdgeGrpcSessionManager stale = mock(EdgeGrpcSessionManager.class);
        when(stale.getState()).thenReturn(staleState);
        EdgeGrpcSessionManager live = mock(EdgeGrpcSessionManager.class);
        when(live.getState()).thenReturn(stateFor(edge));
        doAnswer(invocation -> {
            sessions.put(live);
            return null;
        }).when(stale).destroyAndMarkAsZombieIfFailed();
        sessions.put(stale);

        onEdgeDisconnect(staleState.getSessionId());

        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
    }

    @Test
    public void givenEdgeReconnectedToAnotherNode_whenDisconnect_thenDisconnectEventNotPushed() {
        // The DISCONNECT_EVENT payload is {"active": false, ...} with SERVER_SCOPE, so a rule chain wiring
        // Disconnect Event -> Save Attributes would persist active=false back and re-create the bug.
        EdgeSessionState state = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE, OTHER_NODE));

        onEdgeDisconnect(state.getSessionId());

        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
    }

    @Test
    public void givenEdgeGenuinelyDisconnected_whenDisconnect_thenDisconnectEventPushed() {
        // Also proves the never() assertions above are not vacuous: the push does happen on this path.
        EdgeSessionState state = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));

        onEdgeDisconnect(state.getSessionId());

        verify(clusterService, times(1)).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
    }

    // --- onEdgeConnect ownership publish order ---

    @Test
    public void givenConnect_whenClaimingOwnership_thenCachePutHappensBeforeActiveStateWrite() {
        // Load-bearing ordering: the previous node's onEdgeDisconnect reads this cache entry to decide whether
        // to clear the flag, so the claim must be visible before active=true is written. Nothing else pins the
        // order, and a tidy-up that groups the save(...) calls together would silently restore the race.
        EdgeGrpcSessionManager session = mock(EdgeGrpcSessionManager.class);
        when(session.getState()).thenReturn(stateFor(edge));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);

        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeConnect", edgeId, session);

        InOrder inOrder = inOrder(edgeIdServiceIdCache, tsSubService);
        inOrder.verify(edgeIdServiceIdCache).put(edgeId, THIS_NODE);
        inOrder.verify(tsSubService).saveAttributes(argThat(activeStateWrite()));
    }

    private ArgumentMatcher<AttributesSaveRequest> activeStateWrite() {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry ->
                        ACTIVITY_STATE.equals(entry.getKey())
                                && Boolean.TRUE.equals(entry.getBooleanValue().orElse(null)));
    }

    private EdgeSessionState registerSession() {
        EdgeSessionState state = stateFor(edge);
        EdgeGrpcSessionManager session = mock(EdgeGrpcSessionManager.class);
        when(session.getState()).thenReturn(state);
        sessions.put(session);
        return state;
    }

    private EdgeSessionState stateFor(Edge edge) {
        EdgeSessionState state = new EdgeSessionState();
        state.setEdge(edge);
        return state;
    }

    private void onEdgeDisconnect(UUID sessionId) {
        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeDisconnect", edge, sessionId);
    }

    private ArgumentMatcher<AttributesSaveRequest> inactiveStateWrite() {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry ->
                        ACTIVITY_STATE.equals(entry.getKey())
                                && Boolean.FALSE.equals(entry.getBooleanValue().orElse(null)));
    }

}
