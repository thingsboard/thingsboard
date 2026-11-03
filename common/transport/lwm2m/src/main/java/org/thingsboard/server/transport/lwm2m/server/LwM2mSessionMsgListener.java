// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.rpc.LwM2MRpcRequestHandler;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class LwM2mSessionMsgListener implements GenericFutureListener<Future<? super Void>>, SessionMsgListener {
    private final LwM2mUplinkMsgHandler handler;
    private final LwM2MAttributesService attributesService;
    private final LwM2MRpcRequestHandler rpcHandler;
    private final TransportProtos.SessionInfoProto sessionInfo;
    private final TransportService transportService;

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse) {
        this.attributesService.onGetAttributesResponse(getAttributesResponse, this.sessionInfo);
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, AttributeUpdateNotificationMsg attributeUpdateNotification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        this.attributesService.onAttributesUpdate(attributeUpdateNotification, this.sessionInfo);
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
    }

    @Override
    public void onToTransportUpdateCredentials(ToTransportUpdateCredentialsProto updateCredentials) {
        this.handler.onToTransportUpdateCredentials(sessionInfo, updateCredentials);
    }

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        this.handler.onDeviceProfileUpdate(sessionInfo, deviceProfile);
    }

    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        this.handler.onDeviceUpdate(sessionInfo, device, deviceProfileOpt);
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, ToDeviceRpcRequestMsg toDeviceRequest) {
        log.trace("[{}] Received RPC command to device", sessionId);
        this.rpcHandler.onToDeviceRpcRequest(toDeviceRequest, this.sessionInfo);
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
        this.rpcHandler.onToServerRpcResponse(toServerResponse);
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        log.info("[{}]  operationComplete", future);
    }

    @Override
    public void onResourceUpdate(TransportProtos.ResourceUpdateMsg resourceUpdateMsgOpt) {
        if (ResourceType.LWM2M_MODEL.name().equals(resourceUpdateMsgOpt.getResourceType())) {
            this.handler.onResourceUpdate(resourceUpdateMsgOpt);
        }
    }

    @Override
    public void onResourceDelete(TransportProtos.ResourceDeleteMsg resourceDeleteMsgOpt) {
        if (ResourceType.LWM2M_MODEL.name().equals(resourceDeleteMsgOpt.getResourceType())) {
            this.handler.onResourceDelete(resourceDeleteMsgOpt);
        }
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        log.trace("[{}] Device on delete", deviceId);
        this.handler.onDeviceDelete(deviceId);
    }
}
