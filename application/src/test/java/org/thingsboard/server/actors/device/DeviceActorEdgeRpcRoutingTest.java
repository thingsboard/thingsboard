// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.device;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.edge.EdgeHighPriorityMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.TbRpcService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Where a server-side RPC goes for a device that is assigned to an edge.
 * <p>
 * A device assigned to an edge normally talks to the edge, and the cloud forwards its RPCs
 * to the edge's event queue. The same device can also hold its own transport session on the
 * cloud (for example, a device that falls back to the cloud when it cannot reach its edge).
 * While that session is subscribed to RPC, the cloud can deliver directly, and must: the edge
 * cannot reach the device at that moment.
 */
public class DeviceActorEdgeRpcRoutingTest {

    private static final String NODE_ID = "tb-core-0";

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("5e4ab3e0-8a3b-11ef-9a1c-3d6f8b0e2a10"));
    private final DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");
    private final EdgeId edgeId = new EdgeId(UUID.fromString("0b1c2d3e-4f50-11ef-8a6b-9c7d5e3f1a20"));

    private ActorSystemContext systemContext;
    private RelationService relationService;
    private EdgeService edgeService;
    private TbClusterService clusterService;
    private TbCoreToTransportService transportService;
    private TbRpcService tbRpcService;
    private TbActorCtx actorCtx;

    @Before
    public void setUp() {
        systemContext = mock(ActorSystemContext.class);
        relationService = mock(RelationService.class);
        edgeService = mock(EdgeService.class);
        clusterService = mock(TbClusterService.class);
        transportService = mock(TbCoreToTransportService.class);
        tbRpcService = mock(TbRpcService.class);
        actorCtx = mock(TbActorCtx.class);

        DeviceService deviceService = mock(DeviceService.class);
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        device.setName("fallback-device");
        device.setType("default");
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);

        when(systemContext.getMaxConcurrentSessionsPerDevice()).thenReturn(10);
        when(systemContext.isEdgesEnabled()).thenReturn(true);
        when(systemContext.isLocalCacheType()).thenReturn(true);
        when(systemContext.getDeviceService()).thenReturn(deviceService);
        when(systemContext.getRelationService()).thenReturn(relationService);
        when(systemContext.getEdgeService()).thenReturn(edgeService);
        when(systemContext.getClusterService()).thenReturn(clusterService);
        when(systemContext.getTbCoreToTransportService()).thenReturn(transportService);
        when(systemContext.getTbRpcService()).thenReturn(tbRpcService);
        when(systemContext.getTbCoreDeviceRpcService()).thenReturn(mock(TbCoreDeviceRpcService.class));
        when(systemContext.getDeviceStateService()).thenReturn(mock(DeviceStateService.class));

        when(edgeService.isEdgeActiveAsync(eq(tenantId), eq(edgeId), anyString())).thenReturn(Futures.immediateFuture(true));
    }

    @Test
    public void givenDeviceAssignedToEdge_whenCloudSessionSubscribedToRpc_thenRpcIsSentToSessionNotEdgeQueue() {
        DeviceActorMessageProcessor processor = newProcessor(true);
        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        processor.processRpcRequest(actorCtx, rpcRequest("approveOta", false));

        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "approveOta")));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    @Test
    public void givenDeviceAssignedToEdge_whenCloudSessionSubscribedToRpc_thenPersistentRpcIsSentToSessionNotEdgeQueue() {
        DeviceActorMessageProcessor processor = newProcessor(true);
        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        processor.processRpcRequest(actorCtx, rpcRequest("rebootForUpdate", true));

        verify(tbRpcService).save(eq(tenantId), argThat(rpc -> rpc.getStatus() == RpcStatus.QUEUED));
        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "rebootForUpdate")));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    @Test
    public void givenDeviceAssignedToEdge_whenNoCloudSession_thenRpcIsSavedToEdgeQueue() {
        DeviceActorMessageProcessor processor = newProcessor(true);

        processor.processRpcRequest(actorCtx, rpcRequest("approveOta", false));

        verify(clusterService).onEdgeHighPriorityMsg(argThat(this::isRpcCallToEdge));
        verify(transportService, never()).process(anyString(), argThat(ToTransportMsg::hasToDeviceRequest));
    }

    @Test
    public void givenDeviceAssignedToEdge_whenCloudSessionClosed_thenRpcIsSavedToEdgeQueueAgain() {
        DeviceActorMessageProcessor processor = newProcessor(true);
        UUID sessionId = openSessionAndSubscribeToRpc(processor);
        processor.process(wrap(TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo(sessionId))
                .setSessionEvent(SessionEventMsg.newBuilder().setEvent(SessionEvent.CLOSED))
                .build()));

        processor.processRpcRequest(actorCtx, rpcRequest("approveOta", false));

        verify(clusterService).onEdgeHighPriorityMsg(argThat(this::isRpcCallToEdge));
        verify(transportService, never()).process(anyString(), argThat(ToTransportMsg::hasToDeviceRequest));
    }

    @Test
    public void givenDeviceNotAssignedToEdge_whenCloudSessionSubscribedToRpc_thenRpcIsSentToSession() {
        DeviceActorMessageProcessor processor = newProcessor(false);
        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        processor.processRpcRequest(actorCtx, rpcRequest("approveOta", false));

        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "approveOta")));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    @Test
    public void givenDeviceNotAssignedToEdge_whenNoCloudSession_thenRpcIsNeitherSentNorQueuedToEdge() {
        DeviceActorMessageProcessor processor = newProcessor(false);

        processor.processRpcRequest(actorCtx, rpcRequest("approveOta", false));

        verify(transportService, never()).process(anyString(), argThat(ToTransportMsg::hasToDeviceRequest));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    private DeviceActorMessageProcessor newProcessor(boolean assignedToEdge) {
        List<EntityRelation> relations = assignedToEdge
                ? List.of(new EntityRelation(edgeId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                : Collections.emptyList();
        when(relationService.findByToAndType(tenantId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .thenReturn(relations);
        return new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
    }

    private UUID openSessionAndSubscribeToRpc(DeviceActorMessageProcessor processor) {
        UUID sessionId = UUID.randomUUID();
        processor.process(wrap(TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo(sessionId))
                .setSessionEvent(SessionEventMsg.newBuilder().setEvent(SessionEvent.OPEN))
                .setSubscribeToRPC(SubscribeToRPCMsg.newBuilder().setSessionType(SessionType.ASYNC))
                .build()));
        return sessionId;
    }

    private SessionInfoProto sessionInfo(UUID sessionId) {
        return SessionInfoProto.newBuilder()
                .setNodeId(NODE_ID)
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .build();
    }

    private static TransportToDeviceActorMsgWrapper wrap(TransportToDeviceActorMsg msg) {
        return new TransportToDeviceActorMsgWrapper(msg, TbCallback.EMPTY);
    }

    private ToDeviceRpcRequestActorMsg rpcRequest(String method, boolean persisted) {
        ToDeviceRpcRequest request = new ToDeviceRpcRequest(UUID.randomUUID(), tenantId, deviceId, false,
                System.currentTimeMillis() + 60_000, new ToDeviceRpcRequestBody(method, "{}"), persisted, null, null);
        return new ToDeviceRpcRequestActorMsg(NODE_ID, request);
    }

    private static boolean isRpcTo(ToTransportMsg msg, UUID sessionId, String method) {
        return msg.hasToDeviceRequest()
                && msg.getSessionIdMSB() == sessionId.getMostSignificantBits()
                && msg.getSessionIdLSB() == sessionId.getLeastSignificantBits()
                && method.equals(msg.getToDeviceRequest().getMethodName());
    }

    private boolean isRpcCallToEdge(EdgeHighPriorityMsg msg) {
        return edgeId.equals(msg.getEdgeEvent().getEdgeId())
                && msg.getEdgeEvent().getAction() == EdgeEventActionType.RPC_CALL;
    }

}
