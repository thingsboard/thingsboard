// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
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
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.EdgeSessionsHolder;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
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
class EdgeGrpcServiceTest {

    private static final String THIS_NODE = "tb-core-1";
    private static final String OTHER_NODE = "tb-core-2";

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private TbClusterService clusterService;
    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    private TelemetrySubscriptionService tsSubService;
    @Mock
    private TbTransactionalCache<EdgeId, String> edgeIdServiceIdCache;
    @Mock
    private PartitionService partitionService;
    @Mock
    private EdgeContextComponent ctx;
    @Mock
    private NotificationRuleProcessor ruleProcessor;

    private EdgeSessionsHolder sessions;
    private EdgeGrpcService edgeGrpcService;
    private Edge edge;
    private EdgeId edgeId;

    @BeforeEach
    void setUp() {
        sessions = new EdgeSessionsHolder();
        edgeGrpcService = new EdgeGrpcService(sessions, applicationContext, clusterService, serviceInfoProvider,
                tsSubService, edgeIdServiceIdCache, partitionService);
        ReflectionTestUtils.setField(edgeGrpcService, "ctx", ctx);
        edgeId = new EdgeId(UUID.randomUUID());
        edge = new Edge(edgeId);
        edge.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        edge.setName("test-edge");
        lenient().when(ctx.getRuleProcessor()).thenReturn(ruleProcessor);
    }

    @Test
    void givenNoCacheEntry_whenDisconnect_thenAllDisconnectSideEffectsRun() {
        EdgeGrpcSessionManager session = registerSession();
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);

        onEdgeDisconnect(session);

        verify(tsSubService, times(1)).saveAttributes(argThat(activeStateWrite(false)));
        verify(tsSubService, times(1)).saveAttributes(argThat(lastDisconnectTimeWrite()));
        verify(clusterService, times(1)).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
        verify(ruleProcessor, times(1)).process(argThat(disconnectTrigger()));
    }

    @Test
    void givenEdgeOwnedByAnotherLiveNode_whenDisconnect_thenNoDisconnectSideEffects() {
        EdgeGrpcSessionManager session = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE, OTHER_NODE));

        onEdgeDisconnect(session);

        verifyNoDisconnectSideEffects();
    }

    @Test
    void givenOwnerNodeLeftCluster_whenDisconnect_thenInactiveStateIsPersisted() {
        // the cache has no TTL by default, so a dead owner's claim must not keep the edge active forever
        EdgeGrpcSessionManager session = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap(OTHER_NODE));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Set.of(THIS_NODE));

        onEdgeDisconnect(session);

        verify(tsSubService, times(1)).saveAttributes(argThat(activeStateWrite(false)));
    }

    @Test
    void givenSameNodeReconnectDuringOwnershipCheck_whenDisconnect_thenNoDisconnectSideEffects() {
        // the edge reconnects to this node after the stale session was removed, so the cache names this node
        EdgeGrpcSessionManager session = registerSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);
        when(edgeIdServiceIdCache.get(edgeId)).thenAnswer(invocation -> {
            sessions.put(newSession());
            return SimpleTbCacheValueWrapper.wrap(THIS_NODE);
        });

        onEdgeDisconnect(session);

        verifyNoDisconnectSideEffects();
    }

    @Test
    void givenReconnectDuringInactiveWrite_whenDisconnect_thenReconnectWaitsForTheWrite() throws Exception {
        // a same-node reconnect racing the guard must be ordered after the inactive write, so its active=true wins
        EdgeGrpcSessionManager session = registerSession();
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);
        EdgeGrpcSessionManager replacement = newSession();
        Thread reconnect = new Thread(() -> sessions.put(replacement));
        AtomicBoolean reconnectBlockedDuringWrite = new AtomicBoolean();
        lenient().doAnswer(invocation -> {
            reconnect.start();
            reconnect.join(200);
            reconnectBlockedDuringWrite.set(reconnect.isAlive());
            return null;
        }).when(tsSubService).saveAttributes(argThat(activeStateWrite(false)));

        onEdgeDisconnect(session);
        reconnect.join(5000);

        assertThat(reconnectBlockedDuringWrite).isTrue();
        assertThat(sessions.getByEdgeId(edgeId)).isSameAs(replacement);
    }

    @Test
    void givenReplacedSessionDisconnectsDuringItsTeardown_whenConnect_thenNoDisconnectSideEffects() {
        // the edge reconnects before its old stream is noticed dead; the old session's close fires while it is destroyed
        EdgeGrpcSessionManager old = registerSession();
        doAnswer(invocation -> {
            onEdgeDisconnect(old);
            return null;
        }).when(old).destroyAndMarkAsZombieIfFailed();
        EdgeGrpcSessionManager replacement = newSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);

        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeConnect", edgeId, replacement);

        verify(tsSubService, never()).saveAttributes(argThat(activeStateWrite(false)));
        verify(tsSubService, never()).saveAttributes(argThat(lastDisconnectTimeWrite()));
        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class),
                argThat(msg -> TbMsgType.DISCONNECT_EVENT.equals(msg.getInternalType())), isNull());
        verify(ruleProcessor, never()).process(argThat(disconnectTrigger()));
        verify(edgeIdServiceIdCache, never()).evict(edgeId);
        assertThat(sessions.getByEdgeId(edgeId)).isSameAs(replacement);
        assertThat(sessions.getSessionsById()).doesNotContainKey(old.getState().getSessionId());
    }

    @Test
    void givenConnect_whenClaimingOwnership_thenCachePutHappensBeforeActiveStateWrite() {
        // the previous node reads this cache entry on disconnect, so the claim must be visible before active=true
        EdgeGrpcSessionManager session = newSession();
        when(serviceInfoProvider.getServiceId()).thenReturn(THIS_NODE);

        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeConnect", edgeId, session);

        InOrder inOrder = inOrder(edgeIdServiceIdCache, tsSubService);
        inOrder.verify(edgeIdServiceIdCache).put(edgeId, THIS_NODE);
        inOrder.verify(tsSubService).saveAttributes(argThat(activeStateWrite(true)));
    }

    private void verifyNoDisconnectSideEffects() {
        verify(tsSubService, never()).saveAttributes(argThat(activeStateWrite(false)));
        verify(tsSubService, never()).saveAttributes(argThat(lastDisconnectTimeWrite()));
        verify(clusterService, never()).pushMsgToRuleEngine(any(TenantId.class), any(EntityId.class), any(TbMsg.class), isNull());
        verify(ruleProcessor, never()).process(argThat(disconnectTrigger()));
    }

    private void onEdgeDisconnect(EdgeGrpcSessionManager session) {
        ReflectionTestUtils.invokeMethod(edgeGrpcService, "onEdgeDisconnect", edge, session.getState().getSessionId());
    }

    private EdgeGrpcSessionManager registerSession() {
        EdgeGrpcSessionManager session = newSession();
        sessions.put(session);
        return session;
    }

    private EdgeGrpcSessionManager newSession() {
        EdgeSessionState state = new EdgeSessionState();
        state.setEdge(edge);
        EdgeGrpcSessionManager session = mock(EdgeGrpcSessionManager.class);
        lenient().when(session.getState()).thenReturn(state);
        return session;
    }

    private ArgumentMatcher<AttributesSaveRequest> activeStateWrite(boolean active) {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry ->
                        ACTIVITY_STATE.equals(entry.getKey())
                                && Boolean.valueOf(active).equals(entry.getBooleanValue().orElse(null)));
    }

    private ArgumentMatcher<AttributesSaveRequest> lastDisconnectTimeWrite() {
        return request -> request != null
                && edgeId.equals(request.getEntityId())
                && request.getEntries().stream().anyMatch(entry -> LAST_DISCONNECT_TIME.equals(entry.getKey()));
    }

    private ArgumentMatcher<NotificationRuleTrigger> disconnectTrigger() {
        return trigger -> trigger instanceof EdgeConnectionTrigger edgeTrigger && !edgeTrigger.isConnected();
    }

}
