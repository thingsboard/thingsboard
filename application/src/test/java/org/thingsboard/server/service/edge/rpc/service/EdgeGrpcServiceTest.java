// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
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
import org.thingsboard.server.service.edge.rpc.EdgeSessionState;
import org.thingsboard.server.service.edge.rpc.session.EdgeSessionsHolder;
import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EdgeGrpcServiceTest {

    @Test
    void replacingSessionIgnoresOldDisconnectButProcessesCurrentDisconnect() {
        EdgeSessionsHolder sessions = new EdgeSessionsHolder();
        TbClusterService clusterService = mock(TbClusterService.class);
        TelemetrySubscriptionService telemetryService = mock(TelemetrySubscriptionService.class);
        @SuppressWarnings("unchecked")
        TbTransactionalCache<EdgeId, String> serviceIdCache = mock(TbTransactionalCache.class);
        EdgeGrpcService service = new EdgeGrpcService(sessions, mock(ApplicationContext.class), clusterService,
                mock(TbServiceInfoProvider.class), telemetryService, serviceIdCache);
        ReflectionTestUtils.setField(service, "ctx", mock(EdgeContextComponent.class, RETURNS_DEEP_STUBS));

        Edge edge = new Edge();
        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        edge.setId(edgeId);
        edge.setTenantId(new TenantId(UUID.randomUUID()));

        EdgeSessionState oldState = new EdgeSessionState();
        oldState.setEdge(edge);
        EdgeGrpcSessionManager oldSession = mock(EdgeGrpcSessionManager.class);
        when(oldSession.getState()).thenReturn(oldState);
        sessions.put(oldSession);

        EdgeSessionState newState = new EdgeSessionState();
        newState.setEdge(edge);
        EdgeGrpcSessionManager newSession = mock(EdgeGrpcSessionManager.class);
        when(newSession.getState()).thenReturn(newState);
        doAnswer(invocation -> {
            ReflectionTestUtils.invokeMethod(service, "onEdgeDisconnect", edge, oldState.getSessionId());
            return null;
        }).when(oldSession).destroyAndMarkAsZombieIfFailed();

        ReflectionTestUtils.invokeMethod(service, "onEdgeConnect", edgeId, newSession);

        assertSame(newSession, sessions.getByEdgeId(edgeId));
        verify(telemetryService, times(2)).saveAttributes(any(AttributesSaveRequest.class));
        var messages = org.mockito.ArgumentCaptor.forClass(TbMsg.class);
        verify(clusterService).pushMsgToRuleEngine(eq(edge.getTenantId()), eq(edgeId), messages.capture(), any());
        assertEquals("CONNECT_EVENT", messages.getValue().getType());

        ReflectionTestUtils.invokeMethod(service, "onEdgeDisconnect", edge, newState.getSessionId());

        verify(telemetryService, times(4)).saveAttributes(any(AttributesSaveRequest.class));
        verify(clusterService, times(2)).pushMsgToRuleEngine(eq(edge.getTenantId()), eq(edgeId), messages.capture(), any());
        assertEquals("DISCONNECT_EVENT", messages.getAllValues().get(2).getType());
    }
}
