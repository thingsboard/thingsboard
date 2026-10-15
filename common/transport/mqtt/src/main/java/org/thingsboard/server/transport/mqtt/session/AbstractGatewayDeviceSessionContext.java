// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.mqtt.session;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.mqtt.util.MqttRpcStatusUtil;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@ToString(callSuper = true)
@Slf4j
public abstract class AbstractGatewayDeviceSessionContext<T extends AbstractGatewaySessionHandler> extends MqttDeviceAwareSessionContext implements SessionMsgListener {

    @Getter
    protected final T parent;
    private final TransportService transportService;

    public AbstractGatewayDeviceSessionContext(T parent, TransportDeviceInfo deviceInfo,
                                               DeviceProfile deviceProfile, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap,
                                               TransportService transportService) {
        super(UUID.randomUUID(), mqttQoSMap);
        this.parent = parent;
        setSessionInfo(SessionInfoProto.newBuilder()
                .setNodeId(parent.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(deviceInfo.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceInfo.getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(deviceInfo.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(deviceInfo.getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(deviceInfo.getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(deviceInfo.getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(deviceInfo.getDeviceName())
                .setDeviceType(deviceInfo.getDeviceType())
                .setGwSessionIdMSB(parent.getSessionId().getMostSignificantBits())
                .setGwSessionIdLSB(parent.getSessionId().getLeastSignificantBits())
                .setGatewayIdMSB(parent.gateway.getDeviceId().getId().getMostSignificantBits())
                .setGatewayIdLSB(parent.gateway.getDeviceId().getId().getLeastSignificantBits())
                .setDeviceProfileIdMSB(deviceInfo.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(deviceInfo.getDeviceProfileId().getId().getLeastSignificantBits())
                .build());
        setDeviceInfo(deviceInfo);
        setConnected(true);
        setDeviceProfile(deviceProfile);
        this.transportService = transportService;
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public int nextMsgId() {
        return parent.nextMsgId();
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg response) {
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), response).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), notification).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        parent.deregisterSession(getDeviceInfo().getDeviceName());
    }

    public void onToDeviceRpcRequest(UUID sessionId, ToDeviceRpcRequestMsg request) {
        log.trace("[{}] Received RPC command to device", sessionId);
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), request).ifPresent(
                    payload -> {
                        MqttPublishMessage publishMsg = (MqttPublishMessage) payload;
                        boolean requireDeliveryTracking = MqttRpcStatusUtil.requireDeliveryTracking(request);
                        if (requireDeliveryTracking && isAckExpected(publishMsg)) {
                            parent.registerRpcAwaitingAck(publishMsg.variableHeader().packetId(), getSessionInfo(), request);
                        }
                        ChannelFuture channelFuture = parent.writeAndFlush(publishMsg);
                        channelFuture.addListener(result -> {
                            if (result.cause() == null) {
                                if (!isAckExpected(publishMsg)) {
                                    if (requireDeliveryTracking) {
                                        transportService.process(getSessionInfo(), request, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
                                    }
                                } else if (request.getPersisted()) {
                                    transportService.process(getSessionInfo(), request, RpcStatus.SENT, TransportServiceCallback.EMPTY);
                                }
                            }
                        });
                    }
            );
        } catch (Exception e) {
            transportService.process(getSessionInfo(),
                    ToDeviceRpcResponseMsg.newBuilder()
                            .setRequestId(request.getRequestId()).setError("Failed to convert device RPC command to MQTT msg").build(), TransportServiceCallback.EMPTY);
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
        // This feature is not supported in the TB IoT Gateway yet.
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        parent.onDeviceDeleted(this.getSessionInfo().getDeviceName());
    }

    private boolean isAckExpected(MqttMessage message) {
        return message.fixedHeader().qosLevel().value() > 0;
    }

}
