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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

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
    private EdgeContextComponent ctx;

    @Mock
    private NotificationRuleProcessor ruleProcessor;

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
        verify(tsSubService, never()).saveAttributes(argThat(lastDisconnectTimeWrite()));
    }

    @Test
    public void givenEdgeOwnedByThisNode_whenDisconnect_thenInactiveStateIsPersisted() {
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));

        onEdgeDisconnect(sessionId);

        verify(tsSubService, times(1)).saveAttributes(argThat(inactiveStateWrite()));
        verify(tsSubService, times(1)).saveAttributes(argThat(lastDisconnectTimeWrite()));
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
        // onEdgeConnect has already swapped in a new session on this node when the stale callback runs.
        // The check-and-remove is a single atomic step against the same map that onEdgeConnect writes
        // through, so the swap can only land before or after it. The stale callback must not evict or
        // tear down the live session, nor clear the activity state.
        UUID staleSessionId = UUID.randomUUID();
        EdgeGrpcSession live = mock(EdgeGrpcSession.class);
        when(live.getSessionId()).thenReturn(UUID.randomUUID());
        sessions().put(edgeId, live);

        onEdgeDisconnect(staleSessionId);

        assertThat(sessions().get(edgeId)).isSameAs(live);
        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
    }

    @Test
    public void givenSessionReplacedOnThisNodeDuringDisconnect_whenStaleCallbackRuns_thenNoDisconnectSideEffects() {
        // the edge reconnects to this node after the stale session was detached, so the cache names this node
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        reconnectToThisNodeOnOwnershipCheck();

        onEdgeDisconnect(sessionId);

        verify(tsSubService, never()).saveAttributes(argThat(inactiveStateWrite()));
        verify(tsSubService, never()).saveAttributes(argThat(lastDisconnectTimeWrite()));
        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
        verify(edgeIdServiceIdCache, never()).evict(edgeId);
    }

    @Test
    public void givenReconnectDuringInactiveWrite_whenDisconnect_thenAttachWaitsForTheWrite() throws Exception {
        // a same-node reconnect racing the guard must be ordered after the inactive write, so its active=true wins
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);
        EdgeGrpcSession replacement = mock(EdgeGrpcSession.class);
        Thread attach = new Thread(() -> ReflectionTestUtils.invokeMethod(edgeGrpcService, "attachSession", edgeId, replacement));
        AtomicBoolean attachBlockedDuringWrite = new AtomicBoolean();
        lenient().doAnswer(invocation -> {
            attach.start();
            attach.join(200);
            attachBlockedDuringWrite.set(attach.isAlive());
            return null;
        }).when(tsSubService).saveAttributes(argThat(inactiveStateWrite()));

        onEdgeDisconnect(sessionId);
        attach.join(5000);

        assertThat(attachBlockedDuringWrite).isTrue();
        assertThat(sessions()).containsEntry(edgeId, replacement);
        verify(tsSubService, times(1)).saveAttributes(argThat(lastDisconnectTimeWrite()));
    }

    @Test
    public void givenEdgeReconnectedToAnotherNode_whenDisconnect_thenDisconnectEventNotPushed() {
        // DISCONNECT_EVENT carries active=false, a Save Attributes node would write it back
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE, OTHER_NODE));

        onEdgeDisconnect(sessionId);

        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
    }

    @Test
    public void givenEdgeGenuinelyDisconnected_whenDisconnect_thenDisconnectEventPushed() {
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));
        when(ctx.getRuleProcessor()).thenReturn(ruleProcessor);

        onEdgeDisconnect(sessionId);

        verify(clusterService, times(1)).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
    }

    @Test
    public void givenConnect_whenClaimingOwnership_thenCachePutHappensBeforeActiveStateWrite() {
        // the previous node reads this cache entry on disconnect, so the claim must be visible before active=true
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
    public void givenCacheOwnedByThisNode_whenDisconnect_thenEntryIsEvicted() {
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(THIS_NODE));

        onEdgeDisconnect(sessionId);

        verify(edgeIdServiceIdCache, times(1)).evict(edgeId);
    }

    @Test
    public void givenCacheOwnedByAnotherNode_whenDisconnect_thenEntryIsKept() {
        // The edge already reconnected elsewhere: wiping the live owner's claim would defeat the
        // activity-state guard for any later disconnect callback.
        UUID sessionId = UUID.randomUUID();
        sessions().put(edgeId, sessionFor(sessionId));
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE, OTHER_NODE));

        onEdgeDisconnect(sessionId);

        verify(edgeIdServiceIdCache, never()).evict(edgeId);
    }

    private void onEdgeDisconnect(UUID sessionId) {
        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeDisconnect", edge, sessionId);
    }

    // simulates a reconnect to this node landing after the stale session was detached, before the guard runs
    private void reconnectToThisNodeOnOwnershipCheck() {
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenAnswer(invocation -> {
            sessions().put(edgeId, mock(EdgeGrpcSession.class));
            return SimpleTbCacheValueWrapper.wrap(THIS_NODE);
        });
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

    @SuppressWarnings("unchecked")
    private ConcurrentMap<EdgeId, EdgeGrpcSession> sessions() {
        return (ConcurrentMap<EdgeId, EdgeGrpcSession>) ReflectionTestUtils.getField(edgeGrpcService, "sessions");
    }

    private ArgumentMatcher<AttributesSaveRequest> inactiveStateWrite() {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry ->
                        ACTIVITY_STATE.equals(entry.getKey())
                                && Boolean.FALSE.equals(entry.getBooleanValue().orElse(null)));
    }

    private ArgumentMatcher<AttributesSaveRequest> lastDisconnectTimeWrite() {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry -> LAST_DISCONNECT_TIME.equals(entry.getKey()));
    }

}
