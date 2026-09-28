/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EdgeGrpcServiceTest {

    @Test
    void replacingSessionDoesNotPublishDisconnectEvent() {
        EdgeGrpcService service = new EdgeGrpcService();
        TelemetrySubscriptionService telemetryService = mock(TelemetrySubscriptionService.class);
        TbClusterService clusterService = mock(TbClusterService.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        ReflectionTestUtils.setField(service, "tsSubService", telemetryService);
        ReflectionTestUtils.setField(service, "ctx", mock(EdgeContextComponent.class, RETURNS_DEEP_STUBS));
        ReflectionTestUtils.setField(service, "clusterService", clusterService);
        ReflectionTestUtils.setField(service, "edgeIdServiceIdCache", mock(TbTransactionalCache.class));
        ReflectionTestUtils.setField(service, "serviceInfoProvider", mock(TbServiceInfoProvider.class));
        ReflectionTestUtils.setField(service, "edgeEventProcessingExecutorService", scheduler);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(mock(ScheduledFuture.class));

        Edge edge = new Edge();
        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        edge.setId(edgeId);
        edge.setTenantId(new TenantId(UUID.randomUUID()));
        EdgeGrpcSession oldSession = mock(EdgeGrpcSession.class);
        EdgeGrpcSession newSession = mock(EdgeGrpcSession.class);
        UUID oldSessionId = UUID.randomUUID();
        when(oldSession.getEdge()).thenReturn(edge);
        when(oldSession.getSessionId()).thenReturn(oldSessionId);
        when(newSession.getEdge()).thenReturn(edge);
        when(newSession.getSessionId()).thenReturn(UUID.randomUUID());
        @SuppressWarnings("unchecked")
        ConcurrentMap<EdgeId, EdgeGrpcSession> sessions =
                (ConcurrentMap<EdgeId, EdgeGrpcSession>) ReflectionTestUtils.getField(service, "sessions");
        sessions.put(edgeId, oldSession);
        doAnswer(invocation -> {
            ReflectionTestUtils.invokeMethod(service, "onEdgeDisconnect", edge, oldSessionId);
            return true;
        }).when(oldSession).destroy();

        ReflectionTestUtils.invokeMethod(service, "onEdgeConnect", edgeId, newSession);

        verify(oldSession).destroy();
        verify(telemetryService, times(2)).saveAttributes(any(AttributesSaveRequest.class));
        var messageCaptor = org.mockito.ArgumentCaptor.forClass(TbMsg.class);
        verify(clusterService).pushMsgToRuleEngine(eq(edge.getTenantId()), eq(edgeId), messageCaptor.capture(), any());
        assertEquals("CONNECT_EVENT", messageCaptor.getValue().getType());

        when(newSession.destroy()).thenReturn(true);
        ReflectionTestUtils.invokeMethod(service, "onEdgeDisconnect", edge, newSession.getSessionId());

        verify(telemetryService, times(4)).saveAttributes(any(AttributesSaveRequest.class));
        verify(clusterService, times(2)).pushMsgToRuleEngine(eq(edge.getTenantId()), eq(edgeId), messageCaptor.capture(), any());
        assertEquals("DISCONNECT_EVENT", messageCaptor.getAllValues().get(2).getType());
    }
}
