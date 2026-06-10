/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.SimpleTbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for the cluster-aware predicates that guard the delayed disconnect notification:
 * {@code evictServiceIdCacheIfOwnedByThisNode} and the re-verify check in
 * {@code fireDelayedDisconnectNotification}. These exercise the cross-node ownership logic that the
 * single-node integration tests ({@code EdgeConnectionNotificationTest}) cannot reach.
 */
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

    @InjectMocks
    private EdgeGrpcService edgeGrpcService;

    private TenantId tenantId;
    private EdgeId edgeId;
    private Edge edge;

    @BeforeEach
    public void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
        edgeId = new EdgeId(UUID.randomUUID());
        edge = new Edge(edgeId);
        edge.setTenantId(tenantId);
        edge.setName("test-edge");
    }

    // --- evictServiceIdCacheIfOwnedByThisNode ---

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
