// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.device;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.LinkedHashMapRemoveEldest;
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
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.TbRpcService;
import org.thingsboard.server.service.session.DeviceSessionCacheService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DeviceActorMessageProcessorTest {

    public static final int MAX_CONCURRENT_SESSIONS_PER_DEVICE = 10;
    private static final String NODE_ID = "tb-core-0";
    ActorSystemContext systemContext;
    DeviceService deviceService;
    TbCoreToTransportService transportService;
    TbClusterService clusterService;
    EdgeService edgeService;
    TbCoreDeviceRpcService deviceRpcService;
    TbRpcService tbRpcService;
    DeviceSessionCacheService sessionCacheService;
    TbActorCtx actorCtx;
    TenantId tenantId = TenantId.SYS_TENANT_ID;
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");
    EdgeId edgeId = new EdgeId(UUID.randomUUID());

    DeviceActorMessageProcessor processor;

    @Before
    public void setUp() {
        systemContext = mock(ActorSystemContext.class);
        deviceService = mock(DeviceService.class);
        transportService = mock(TbCoreToTransportService.class);
        clusterService = mock(TbClusterService.class);
        edgeService = mock(EdgeService.class);
        deviceRpcService = mock(TbCoreDeviceRpcService.class);
        tbRpcService = mock(TbRpcService.class);
        sessionCacheService = mock(DeviceSessionCacheService.class);
        actorCtx = mock(TbActorCtx.class);
        willReturn(MAX_CONCURRENT_SESSIONS_PER_DEVICE).given(systemContext).getMaxConcurrentSessionsPerDevice();
        willReturn(deviceService).given(systemContext).getDeviceService();
        willReturn(true).given(systemContext).isEdgesEnabled();
        willReturn(clusterService).given(systemContext).getClusterService();
        willReturn(edgeService).given(systemContext).getEdgeService();
        willReturn(deviceRpcService).given(systemContext).getTbCoreDeviceRpcService();
        willReturn(tbRpcService).given(systemContext).getTbRpcService();
        willReturn(sessionCacheService).given(systemContext).getDeviceSessionCacheService();
        willReturn(mock(DeviceStateService.class)).given(systemContext).getDeviceStateService();
        processor = newStandaloneProcessor();
        willReturn(transportService).given(systemContext).getTbCoreToTransportService();
    }

    @Test
    public void givenSystemContext_whenNewInstance_thenVerifySessionMapMaxSize() {
        assertThat(processor.sessions, instanceOf(LinkedHashMapRemoveEldest.class));
        assertThat(processor.sessions.getMaxEntries(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.sessions.getRemovalConsumer(), notNullValue());
    }

    @Test
    public void givenFullSessionMap_whenSessionOverflow_thenShouldDeleteAttributeAndRPCSubscriptions() {
        //givenFullSessionMap
        for (int i = 0; i < MAX_CONCURRENT_SESSIONS_PER_DEVICE; i++) {
            UUID sessionID = UUID.randomUUID();
            processor.sessions.put(sessionID, Mockito.mock(SessionInfoMetaData.class, RETURNS_DEEP_STUBS));
            processor.attributeSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
            processor.rpcSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
        }
        assertThat(processor.sessions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.attributeSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.rpcSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));

        //add one more
        processor.sessions.put(UUID.randomUUID(), Mockito.mock(SessionInfoMetaData.class));

        assertThat(processor.sessions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.attributeSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE-1));
        assertThat(processor.rpcSubscriptions.size(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE-1));

    }

    @Test
    public void givenEdgeAssignedDevice_whenSessionSubscribedToRpc_thenRpcIsSentToSessionNotEdgeQueue() {
        givenEdgeIsActive();
        processor = newEdgeAssignedProcessor();
        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));

        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "approveOta")));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    @Test
    public void givenEdgeAssignedDevice_whenSessionSubscribedToRpc_thenPersistedRpcIsSentToSessionNotEdgeQueue() {
        givenEdgeIsActive();
        processor = newEdgeAssignedProcessor();
        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        processor.processRpcRequest(actorCtx, persistedRpc("rebootForUpdate"));

        verify(tbRpcService).save(eq(tenantId), argThat(rpc -> rpc.getStatus() == RpcStatus.QUEUED));
        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "rebootForUpdate")));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    @Test
    public void givenEdgeAssignedDeviceAndInactiveEdge_whenSessionSubscribedToRpc_thenRpcIsSentToSession() {
        givenEdgeIsInactive();
        processor = newEdgeAssignedProcessor();
        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));

        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "approveOta")));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    @Test
    public void givenEdgeAssignedDevice_whenOneWayRpcSentOverSession_thenRpcCompletesImmediately() {
        givenEdgeIsActive();
        processor = newEdgeAssignedProcessor();
        UUID sessionId = openSessionAndSubscribeToRpc(processor);
        ToDeviceRpcRequestActorMsg rpc = oneWayRpc("setLed");

        processor.processRpcRequest(actorCtx, rpc);

        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "setLed")));
        verify(deviceRpcService).processRpcResponseFromDeviceActor(argThat(response ->
                response.getId().equals(rpc.getMsg().getId()) && response.getError().isEmpty()));
        verify(systemContext, never()).scheduleMsgWithDelay(any(), any(), anyLong());
    }

    @Test
    public void givenEdgeAssignedDevice_whenNoSession_thenRpcIsSavedToEdgeQueue() {
        givenEdgeIsActive();
        processor = newEdgeAssignedProcessor();

        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));

        verify(clusterService).onEdgeHighPriorityMsg(argThat(this::isRpcCallToEdge));
        verify(transportService, never()).process(anyString(), argThat(ToTransportMsg::hasToDeviceRequest));
    }

    @Test
    public void givenEdgeAssignedDeviceAndInactiveEdge_whenNoSession_thenRpcIsNotQueuedToEdge() {
        givenEdgeIsInactive();
        processor = newEdgeAssignedProcessor();

        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));

        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
        verify(transportService, never()).process(anyString(), argThat(ToTransportMsg::hasToDeviceRequest));
        verify(systemContext).scheduleMsgWithDelay(eq(actorCtx), any(DeviceActorServerSideRpcTimeoutMsg.class), anyLong());
    }

    @Test
    public void givenEdgeAssignedDevice_whenSessionClosed_thenRpcIsSavedToEdgeQueueAgain() {
        givenEdgeIsActive();
        processor = newEdgeAssignedProcessor();
        closeSession(processor, openSessionAndSubscribeToRpc(processor));

        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));

        verify(clusterService).onEdgeHighPriorityMsg(argThat(this::isRpcCallToEdge));
        verify(transportService, never()).process(anyString(), argThat(ToTransportMsg::hasToDeviceRequest));
    }

    @Test
    public void givenEdgeAssignedDevice_whenRpcQueuedToEdgeAndThenSessionSubscribes_thenPendingRpcIsSentToSession() {
        givenEdgeIsActive();
        processor = newEdgeAssignedProcessor();
        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));
        verify(clusterService).onEdgeHighPriorityMsg(argThat(this::isRpcCallToEdge));

        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "approveOta")));
    }

    @Test
    public void givenEdgesDisabledAndDeviceWithEdgeRelation_whenNoSession_thenRpcIsNotQueuedToEdge() {
        givenEdgeIsActive();
        willReturn(false).given(systemContext).isEdgesEnabled();
        processor = newProcessorForDeviceWithEdgeRelation();

        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));

        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
        verify(systemContext).scheduleMsgWithDelay(eq(actorCtx), any(DeviceActorServerSideRpcTimeoutMsg.class), anyLong());
    }

    @Test
    public void givenStandaloneDevice_whenSessionSubscribedToRpc_thenRpcIsSentToSession() {
        UUID sessionId = openSessionAndSubscribeToRpc(processor);

        processor.processRpcRequest(actorCtx, twoWayRpc("approveOta"));

        verify(transportService).process(eq(NODE_ID), argThat(msg -> isRpcTo(msg, sessionId, "approveOta")));
        verify(clusterService, never()).onEdgeHighPriorityMsg(any());
    }

    private DeviceActorMessageProcessor newStandaloneProcessor() {
        return new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
    }

    private DeviceActorMessageProcessor newEdgeAssignedProcessor() {
        DeviceActorMessageProcessor edgeAssigned = newStandaloneProcessor();
        edgeAssigned.processEdgeUpdate(new DeviceEdgeUpdateMsg(tenantId, deviceId, edgeId));
        return edgeAssigned;
    }

    private DeviceActorMessageProcessor newProcessorForDeviceWithEdgeRelation() {
        RelationService relationService = mock(RelationService.class);
        willReturn(List.of(new EntityRelation(edgeId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE)))
                .given(relationService).findByToAndType(tenantId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        willReturn(relationService).given(systemContext).getRelationService();
        willReturn(new Device(deviceId)).given(deviceService).findDeviceById(tenantId, deviceId);
        willReturn(DeviceSessionsCacheEntry.getDefaultInstance()).given(sessionCacheService).get(deviceId);
        return newStandaloneProcessor();
    }

    private void givenEdgeIsActive() {
        givenEdgeActiveState(true);
    }

    private void givenEdgeIsInactive() {
        givenEdgeActiveState(false);
    }

    private void givenEdgeActiveState(boolean active) {
        willReturn(Futures.immediateFuture(active)).given(edgeService).isEdgeActiveAsync(eq(tenantId), eq(edgeId), anyString());
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

    private void closeSession(DeviceActorMessageProcessor processor, UUID sessionId) {
        processor.process(wrap(TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo(sessionId))
                .setSessionEvent(SessionEventMsg.newBuilder().setEvent(SessionEvent.CLOSED))
                .build()));
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

    private ToDeviceRpcRequestActorMsg twoWayRpc(String method) {
        return rpc(method, false, false);
    }

    private ToDeviceRpcRequestActorMsg oneWayRpc(String method) {
        return rpc(method, true, false);
    }

    private ToDeviceRpcRequestActorMsg persistedRpc(String method) {
        return rpc(method, false, true);
    }

    private ToDeviceRpcRequestActorMsg rpc(String method, boolean oneway, boolean persisted) {
        ToDeviceRpcRequest request = new ToDeviceRpcRequest(UUID.randomUUID(), tenantId, deviceId, oneway,
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
