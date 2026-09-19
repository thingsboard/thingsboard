// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cache.SimpleTbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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
    private EdgeContextComponent ctx;

    @Mock
    private NotificationRuleProcessor ruleProcessor;

    @Mock
    private TelemetrySubscriptionService tsSubService;

    @Mock
    private TbClusterService clusterService;

    @Mock
    private PartitionService partitionService;

    @Mock
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Mock
    private ScheduledExecutorService edgeEventProcessingExecutorService;

    @InjectMocks
    private EdgeGrpcService edgeGrpcService;

    private EdgeId edgeId;
    private Edge edge;

    @BeforeEach
    public void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        edge = new Edge(edgeId);
        edge.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        edge.setName("test-edge");
    }

    @Test
    public void givenCacheOwnedByThisNode_whenEvict_thenEntryIsEvicted() {
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));

        evictServiceIdCacheIfOwnedByThisNode();

        verify(edgeIdServiceIdCache, times(1)).evict(edgeId);
    }

    @Test
    public void givenCacheOwnedByAnotherNode_whenEvict_thenEntryIsKept() {
        // The edge already reconnected to another node within the keep-alive window: must NOT wipe the live owner.
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));

        evictServiceIdCacheIfOwnedByThisNode();

        verify(edgeIdServiceIdCache, never()).evict(edgeId);
    }

    @Test
    public void givenEmptyCache_whenEvict_thenNothingEvicted() {
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);

        evictServiceIdCacheIfOwnedByThisNode();

        verify(edgeIdServiceIdCache, never()).evict(edgeId);
    }

    // --- fireDelayedDisconnectNotification re-verify guard ---

    @Test
    public void givenEdgeReconnectedToThisNode_whenDelayFires_thenNotificationSuppressed() {
        // A live session exists again on this node - suppress the stale disconnect notification.
        sessions().put(edgeId, mock(EdgeGrpcSession.class));

        fireDelayedDisconnectNotification();

        verify(ruleProcessor, never()).process(any());
    }

    @Test
    public void givenEdgeReconnectedToAnotherNode_whenDelayFires_thenNotificationSuppressed() {
        // No local session, but the cluster cache still points at some node: the edge is connected elsewhere.
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));

        fireDelayedDisconnectNotification();

        verify(ruleProcessor, never()).process(any());
    }

    @Test
    public void givenEdgeStaysDisconnectedClusterWide_whenDelayFires_thenNotificationSent() {
        // No local session and no cache entry on any node: the edge is genuinely down - fire the notification.
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);
        when(ctx.getRuleProcessor()).thenReturn(ruleProcessor);

        fireDelayedDisconnectNotification();

        verify(ruleProcessor, times(1)).process(argThat(disconnectTrigger()));
    }

    @Test
    public void givenPendingDisconnect_whenDestroy_thenNotificationFlushed() {
        // The edge is genuinely down (no session, no cache). A graceful shutdown must flush the pending
        // notification rather than drop it, otherwise a restart within the delay window swallows the alert.
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);
        when(ctx.getRuleProcessor()).thenReturn(ruleProcessor);
        pendingDisconnects().put(edgeId, new EdgeGrpcService.PendingDisconnect(edge));

        destroy();

        verify(ruleProcessor, times(1)).process(argThat(disconnectTrigger()));
    }

    @Test
    public void givenPendingDisconnectButReconnectedElsewhere_whenDestroy_thenNotificationSuppressed() {
        // The flush still honors the re-verify guard: an edge that reconnected to another node must not alert.
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        pendingDisconnects().put(edgeId, new EdgeGrpcService.PendingDisconnect(edge));

        destroy();

        verify(ruleProcessor, never()).process(any());
    }

    // --- activity-state write guard on disconnect ---

    @Test
    public void givenEdgeReconnectedToAnotherNode_whenDisconnect_thenInactiveStateNotPersisted() {
        // A newer session on another node already set active=true. This stale session's disconnect must not
        // clobber it - otherwise the edge stays marked inactive while connected, and the server silently
        // drops every assignment event for it.
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE, OTHER_NODE));

        onEdgeDisconnect(sessionId);

        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenOwnerNodeNoLongerInCluster_whenDisconnect_thenInactiveStateIsPersisted() {
        // The recorded owner died without evicting its claim. The edge sessions cache has no TTL by default
        // and only the owner ever evicts, so honouring a dead claim would leave the edge active forever.
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE));

        onEdgeDisconnect(sessionId);

        verify(tsSubService, times(1)).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenSessionReplacedDuringDisconnect_whenStaleCallbackRuns_thenLiveSessionKept() {
        // Reproduces the get()/remove() interleaving on a single node: onEdgeConnect swaps in a new session
        // right after the stale callback's id check passes. The stale callback must not evict or tear down
        // the live session, nor clear the activity state.
        UUID staleSessionId = UUID.randomUUID();
        EdgeGrpcSession live = mock(EdgeGrpcSession.class);
        EdgeGrpcSession stale = mock(EdgeGrpcSession.class);
        when(stale.getSessionId()).thenAnswer(invocation -> {
            sessions().put(edgeId, live);
            return staleSessionId;
        });
        sessions().put(edgeId, stale);

        onEdgeDisconnect(staleSessionId);

        assertThat(sessions().get(edgeId)).isSameAs(live);
        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenSessionReplacedOnThisNodeAfterRemoval_whenStaleCallbackRuns_thenNoDisconnectSideEffects() {
        // The stale callback wins the compare-and-remove, then tearing its own session down takes a while
        // (for the Kafka session this stops a consumer). The edge reconnects to THIS node in that gap, so the
        // ownership cache legitimately names us and isOwnedByAnotherNode cannot help. None of the disconnect
        // side effects may run: not the flag, not the timestamp, not the DISCONNECT_EVENT push.
        UUID sessionId = UUID.randomUUID();
        EdgeGrpcSession stale = mock(EdgeGrpcSession.class);
        when(stale.getSessionId()).thenReturn(sessionId);
        when(stale.getEdge()).thenReturn(edge);
        when(stale.destroy()).thenAnswer(invocation -> {
            sessions().put(edgeId, mock(EdgeGrpcSession.class));
            return true;
        });
        sessions().put(edgeId, stale);

        onEdgeDisconnect(sessionId);

        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
        verify(edgeIdServiceIdCache, never()).evict(edgeId);
    }

    @Test
    public void givenEdgeReconnectedToAnotherNode_whenDisconnect_thenDisconnectEventNotPushed() {
        // The DISCONNECT_EVENT payload is {"active": false, ...} with SERVER_SCOPE, so a rule chain wiring
        // Disconnect Event -> Save Attributes would persist active=false back and re-create the bug.
        EdgeSessionStub stub = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE, OTHER_NODE));

        onEdgeDisconnect(stub.sessionId());

        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
    }

    @Test
    public void givenEdgeGenuinelyDisconnected_whenDisconnect_thenDisconnectEventPushed() {
        // Also proves the never() assertions above are not vacuous: the push does happen on this path.
        EdgeSessionStub stub = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));

        onEdgeDisconnect(stub.sessionId());

        verify(clusterService, times(1)).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
    }

    // --- onEdgeConnect ownership publish order ---

    @Test
    public void givenConnect_whenClaimingOwnership_thenCachePutHappensBeforeActiveStateWrite() {
        // Load-bearing ordering: the previous node's onEdgeDisconnect reads this cache entry to decide whether
        // to clear the flag, so the claim must be visible before active=true is written. Nothing else pins the
        // order, and a tidy-up that groups the save(...) calls together would silently restore the race.
        EdgeGrpcSession session = mock(EdgeGrpcSession.class);
        when(session.getSessionId()).thenReturn(UUID.randomUUID());
        when(session.getEdge()).thenReturn(edge);
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(ctx.getEdgeEventStorageSettings()).thenReturn(edgeEventStorageSettings);
        ReflectionTestUtils.setField(edgeGrpcService, "edgeEventProcessingExecutorService", edgeEventProcessingExecutorService);
        when(edgeEventProcessingExecutorService.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenAnswer(invocation -> mock(ScheduledFuture.class));

        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeConnect", edgeId, session);

        InOrder inOrder = inOrder(edgeIdServiceIdCache, tsSubService);
        inOrder.verify(edgeIdServiceIdCache).put(edgeId, THIS_NODE);
        inOrder.verify(tsSubService).saveAttributes(argThat(activeStateWrite()));
    }

    // --- service-id cache eviction guard ---

    @Test
    public void givenEdgeOwnedByThisNode_whenDisconnect_thenInactiveStateIsPersisted() {
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));

        onEdgeDisconnect(sessionId);

        verify(tsSubService, times(1)).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenNoCacheEntry_whenDisconnect_thenInactiveStateIsPersisted() {
        // Ownership unknown (entry absent or expired): keep the pre-guard behaviour, otherwise a genuinely
        // disconnected edge would stay marked active forever.
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);

        onEdgeDisconnect(sessionId);

        verify(tsSubService, times(1)).saveAttributes(argThat(inactiveStateWrite()));
    }

    private void onEdgeDisconnect(UUID sessionId) {
        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeDisconnect", edge, sessionId);
    }

    private EdgeSessionStub registerSession() {
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        return new EdgeSessionStub(sessionId);
    }

    private record EdgeSessionStub(UUID sessionId) {
    }

    private ArgumentMatcher<AttributesSaveRequest> activeStateWrite() {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry ->
                        ACTIVITY_STATE.equals(entry.getKey())
                                && Boolean.TRUE.equals(entry.getBooleanValue().orElse(null)));
    }

    private EdgeGrpcSession sessionFor(UUID sessionId) {
        EdgeGrpcSession session = mock(EdgeGrpcSession.class);
        when(session.getSessionId()).thenReturn(sessionId);
        when(session.getEdge()).thenReturn(edge);
        return session;
    }

    private ArgumentMatcher<AttributesSaveRequest> inactiveStateWrite() {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry ->
                        ACTIVITY_STATE.equals(entry.getKey())
                                && Boolean.FALSE.equals(entry.getBooleanValue().orElse(null)));
    }

    private void destroy() {
        ReflectionTestUtils.invokeMethod(edgeGrpcService, "destroy");
    }

    private void evictServiceIdCacheIfOwnedByThisNode() {
        ReflectionTestUtils.invokeMethod(edgeGrpcService, "evictServiceIdCacheIfOwnedByThisNode", edgeId);
    }

    private void fireDelayedDisconnectNotification() {
        EdgeGrpcService.PendingDisconnect pending = new EdgeGrpcService.PendingDisconnect(edge);
        ReflectionTestUtils.invokeMethod(edgeGrpcService, "fireDelayedDisconnectNotification", pending);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<EdgeId, EdgeGrpcSession> sessions() {
        return (ConcurrentMap<EdgeId, EdgeGrpcSession>) ReflectionTestUtils.getField(edgeGrpcService, "sessions");
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<EdgeId, EdgeGrpcService.PendingDisconnect> pendingDisconnects() {
        return (ConcurrentMap<EdgeId, EdgeGrpcService.PendingDisconnect>) ReflectionTestUtils.getField(edgeGrpcService, "pendingDisconnectNotifications");
    }

    private static ArgumentMatcher<NotificationRuleTrigger> disconnectTrigger() {
        return trigger -> trigger instanceof EdgeConnectionTrigger edgeTrigger && !edgeTrigger.isConnected();
    }

}
