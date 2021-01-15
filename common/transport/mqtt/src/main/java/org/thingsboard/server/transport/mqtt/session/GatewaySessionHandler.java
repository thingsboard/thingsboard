/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.session;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.adaptor.ProtoConverter;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewaySessionHandler {

    private static final String DEFAULT_DEVICE_TYPE = "default";
    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";
    private static final String DEVICE_PROPERTY = "device";

    private final MqttTransportContext context;
    private final TransportService transportService;
    private final TransportDeviceInfo gateway;
    private final UUID sessionId;
    private final ConcurrentMap<String, Lock> deviceCreationLockMap;
    private final ConcurrentMap<String, GatewayDeviceSessionCtx> devices;
    private final ConcurrentMap<String, SettableFuture<GatewayDeviceSessionCtx>> deviceFutures;
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;
    private final ChannelHandlerContext channel;
    private final DeviceSessionCtx deviceSessionCtx;

    public GatewaySessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId) {
        this.context = deviceSessionCtx.getContext();
        this.transportService = context.getTransportService();
        this.deviceSessionCtx = deviceSessionCtx;
        this.gateway = deviceSessionCtx.getDeviceInfo();
        this.sessionId = sessionId;
        this.devices = new ConcurrentHashMap<>();
        this.deviceFutures = new ConcurrentHashMap<>();
        this.deviceCreationLockMap = new ConcurrentHashMap<>();
        this.mqttQoSMap = deviceSessionCtx.getMqttQoSMap();
        this.channel = deviceSessionCtx.getChannel();
    }

    public void onDeviceConnect(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceConnectJson(mqttMsg);
        } else {
            onDeviceConnectProto(mqttMsg);
        }
    }

    public void onDeviceDisconnect(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceDisconnectJson(mqttMsg);
        } else {
            onDeviceDisconnectProto(mqttMsg);
        }
    }

    public void onDeviceTelemetry(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceTelemetryJson(msgId, payload);
        } else {
            onDeviceTelemetryProto(msgId, payload);
        }
    }

    public void onDeviceClaim(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceClaimJson(msgId, payload);
        } else {
            onDeviceClaimProto(msgId, payload);
        }
    }

    public void onDeviceAttributes(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceAttributesJson(msgId, payload);
        } else {
            onDeviceAttributesProto(msgId, payload);
        }
    }

    public void onDeviceAttributesRequest(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceAttributesRequestJson(mqttMsg);
        } else {
            onDeviceAttributesRequestProto(mqttMsg);
        }
    }

    public void onDeviceRpcResponse(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceRpcResponseJson(msgId, payload);
        } else {
            onDeviceRpcResponseProto(msgId, payload);
        }
    }

    public void onGatewayDisconnect() {
        devices.forEach(this::deregisterSession);
    }

    public String getNodeId() {
        return context.getNodeId();
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public MqttTransportAdaptor getPayloadAdaptor() {
        return deviceSessionCtx.getPayloadAdaptor();
    }

    public TransportDeviceInfo getDeviceInfo() { return deviceSessionCtx.getDeviceInfo(); }

    void deregisterSession(String deviceName) {
        GatewayDeviceSessionCtx deviceSessionCtx = devices.remove(deviceName);
        if (deviceSessionCtx != null) {
            deregisterSession(deviceName, deviceSessionCtx);
        } else {
            log.debug("[{}] Device [{}] was already removed from the gateway session", sessionId, deviceName);
        }
    }

    void writeAndFlush(MqttMessage mqttMessage) {
        channel.writeAndFlush(mqttMessage);
    }

    int nextMsgId() {
        return deviceSessionCtx.nextMsgId();
    }

    private boolean isJsonPayloadType() {
        return deviceSessionCtx.isJsonPayloadType();
    }

    private void processOnConnect(MqttPublishMessage msg, String deviceName, String deviceType) {
        log.trace("[{}] onDeviceConnect: {}", sessionId, deviceName);
        Futures.addCallback(onDeviceConnect(deviceName, deviceType), new FutureCallback<GatewayDeviceSessionCtx>() {
            @Override
            public void onSuccess(@Nullable GatewayDeviceSessionCtx result) {
                ack(msg);
                log.trace("[{}] onDeviceConnectOk: {}", sessionId, deviceName);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to process device connect command: {}", sessionId, deviceName, t);

            }
        }, context.getExecutor());
    }

    private ListenableFuture<GatewayDeviceSessionCtx> onDeviceConnect(String deviceName, String deviceType) {
        GatewayDeviceSessionCtx result = devices.get(deviceName);
        if (result == null) {
            Lock deviceCreationLock = deviceCreationLockMap.computeIfAbsent(deviceName, s -> new ReentrantLock());
            deviceCreationLock.lock();
            try {
                result = devices.get(deviceName);
                if (result == null) {
                    return getDeviceCreationFuture(deviceName, deviceType);
                } else {
                    return toCompletedFuture(result);
                }
            } finally {
                deviceCreationLock.unlock();
            }
        } else {
            return toCompletedFuture(result);
        }
    }

    private ListenableFuture<GatewayDeviceSessionCtx> getDeviceCreationFuture(String deviceName, String deviceType) {
        SettableFuture<GatewayDeviceSessionCtx> future = deviceFutures.get(deviceName);
        if (future == null) {
            final SettableFuture<GatewayDeviceSessionCtx> futureToSet = SettableFuture.create();
            deviceFutures.put(deviceName, futureToSet);
            try {
                transportService.process(GetOrCreateDeviceFromGatewayRequestMsg.newBuilder()
                                .setDeviceName(deviceName)
                                .setDeviceType(deviceType)
                                .setGatewayIdMSB(gateway.getDeviceId().getId().getMostSignificantBits())
                                .setGatewayIdLSB(gateway.getDeviceId().getId().getLeastSignificantBits()).build(),
                        new TransportServiceCallback<GetOrCreateDeviceFromGatewayResponse>() {
                            @Override
                            public void onSuccess(GetOrCreateDeviceFromGatewayResponse msg) {
                                GatewayDeviceSessionCtx deviceSessionCtx = new GatewayDeviceSessionCtx(GatewaySessionHandler.this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap);
                                if (devices.putIfAbsent(deviceName, deviceSessionCtx) == null) {
                                    log.trace("[{}] First got or created device [{}], type [{}] for the gateway session", sessionId, deviceName, deviceType);
                                    SessionInfoProto deviceSessionInfo = deviceSessionCtx.getSessionInfo();
                                    transportService.registerAsyncSession(deviceSessionInfo, deviceSessionCtx);
                                    transportService.process(deviceSessionInfo, DefaultTransportService.getSessionEventMsg(TransportProtos.SessionEvent.OPEN), null);
                                    transportService.process(deviceSessionInfo, TransportProtos.SubscribeToRPCMsg.getDefaultInstance(), null);
                                    transportService.process(deviceSessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.getDefaultInstance(), null);
                                }
                                futureToSet.set(devices.get(deviceName));
                                deviceFutures.remove(deviceName);
                            }

                            @Override
                            public void onError(Throwable e) {
                                log.warn("[{}] Failed to process device connect command: {}", sessionId, deviceName, e);
                                futureToSet.setException(e);
                                deviceFutures.remove(deviceName);
                            }
                        });
                return futureToSet;
            } catch (Throwable e) {
                deviceFutures.remove(deviceName);
                throw e;
            }
        } else {
            return future;
        }
    }

    private ListenableFuture<GatewayDeviceSessionCtx> toCompletedFuture(GatewayDeviceSessionCtx result) {
        SettableFuture<GatewayDeviceSessionCtx> future = SettableFuture.create();
        future.set(result);
        return future;
    }

    private int getMsgId(MqttPublishMessage mqttMsg) {
        return mqttMsg.variableHeader().packetId();
    }

    private void onDeviceConnectJson(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = getJson(mqttMsg);
        String deviceName = checkDeviceName(getDeviceName(json));
        String deviceType = getDeviceType(json);
        processOnConnect(mqttMsg, deviceName, deviceType);
    }

    private void onDeviceConnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            TransportApiProtos.ConnectMsg connectProto = TransportApiProtos.ConnectMsg.parseFrom(getBytes(mqttMsg.payload()));
            String deviceName = checkDeviceName(connectProto.getDeviceName());
            String deviceType = StringUtils.isEmpty(connectProto.getDeviceType()) ? DEFAULT_DEVICE_TYPE : connectProto.getDeviceType();
            processOnConnect(mqttMsg, deviceName, deviceType);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void onDeviceDisconnectJson(MqttPublishMessage msg) throws AdaptorException {
        String deviceName = checkDeviceName(getDeviceName(getJson(msg)));
        processOnDisconnect(msg, deviceName);
    }

    private void onDeviceDisconnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            TransportApiProtos.DisconnectMsg connectProto = TransportApiProtos.DisconnectMsg.parseFrom(getBytes(mqttMsg.payload()));
            String deviceName = checkDeviceName(connectProto.getDeviceName());
            processOnDisconnect(mqttMsg, deviceName);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void processOnDisconnect(MqttPublishMessage msg, String deviceName) {
        deregisterSession(deviceName);
        ack(msg);
    }

    private void onDeviceTelemetryJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Futures.addCallback(checkDeviceConnected(deviceName),
                        new FutureCallback<GatewayDeviceSessionCtx>() {
                            @Override
                            public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                                if (!deviceEntry.getValue().isJsonArray()) {
                                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                                }
                                try {
                                    TransportProtos.PostTelemetryMsg postTelemetryMsg = JsonConverter.convertToTelemetryProto(deviceEntry.getValue().getAsJsonArray());
                                    processPostTelemetryMsg(deviceCtx, postTelemetryMsg, deviceName, msgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}] Failed to convert telemetry: {}", gateway.getDeviceId(), deviceName, deviceEntry.getValue(), e);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}] Failed to process device telemetry command: {}", sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    private void onDeviceTelemetryProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayTelemetryMsg telemetryMsgProto = TransportApiProtos.GatewayTelemetryMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.TelemetryMsg> deviceMsgList = telemetryMsgProto.getMsgList();
            if (!CollectionUtils.isEmpty(deviceMsgList)) {
                deviceMsgList.forEach(telemetryMsg -> {
                    String deviceName = checkDeviceName(telemetryMsg.getDeviceName());
                    Futures.addCallback(checkDeviceConnected(deviceName),
                            new FutureCallback<GatewayDeviceSessionCtx>() {
                                @Override
                                public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                                    TransportProtos.PostTelemetryMsg msg = telemetryMsg.getMsg();
                                    try {
                                        TransportProtos.PostTelemetryMsg postTelemetryMsg = ProtoConverter.validatePostTelemetryMsg(msg.toByteArray());
                                        processPostTelemetryMsg(deviceCtx, postTelemetryMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}] Failed to convert telemetry: {}", gateway.getDeviceId(), deviceName, msg, e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}] Failed to process device telemetry command: {}", sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}] Devices telemetry messages is empty for: [{}]", sessionId, gateway.getDeviceId());
                throw new IllegalArgumentException("[" + sessionId + "] Devices telemetry messages is empty for [" + gateway.getDeviceId() + "]");
            }
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void processPostTelemetryMsg(GatewayDeviceSessionCtx deviceCtx, TransportProtos.PostTelemetryMsg postTelemetryMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), postTelemetryMsg, getPubAckCallback(channel, deviceName, msgId, postTelemetryMsg));
    }

    private void onDeviceClaimJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Futures.addCallback(checkDeviceConnected(deviceName),
                        new FutureCallback<GatewayDeviceSessionCtx>() {
                            @Override
                            public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                                if (!deviceEntry.getValue().isJsonObject()) {
                                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                                }
                                try {
                                    DeviceId deviceId = deviceCtx.getDeviceId();
                                    TransportProtos.ClaimDeviceMsg claimDeviceMsg = JsonConverter.convertToClaimDeviceProto(deviceId, deviceEntry.getValue());
                                    processClaimDeviceMsg(deviceCtx, claimDeviceMsg, deviceName, msgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}] Failed to convert claim message: {}", gateway.getDeviceId(), deviceName, deviceEntry.getValue(), e);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}] Failed to process device claiming command: {}", sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    private void onDeviceClaimProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayClaimMsg claimMsgProto = TransportApiProtos.GatewayClaimMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.ClaimDeviceMsg> claimMsgList = claimMsgProto.getMsgList();
            if (!CollectionUtils.isEmpty(claimMsgList)) {
                claimMsgList.forEach(claimDeviceMsg -> {
                    String deviceName = checkDeviceName(claimDeviceMsg.getDeviceName());
                    Futures.addCallback(checkDeviceConnected(deviceName),
                            new FutureCallback<GatewayDeviceSessionCtx>() {
                                @Override
                                public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                                    TransportApiProtos.ClaimDevice claimRequest = claimDeviceMsg.getClaimRequest();
                                    if (claimRequest == null) {
                                        throw new IllegalArgumentException("Claim request for device: " + deviceName + " is null!");
                                    }
                                    try {
                                        DeviceId deviceId = deviceCtx.getDeviceId();
                                        TransportProtos.ClaimDeviceMsg claimDeviceMsg = ProtoConverter.convertToClaimDeviceProto(deviceId, claimRequest.toByteArray());
                                        processClaimDeviceMsg(deviceCtx, claimDeviceMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}] Failed to convert claim message: {}", gateway.getDeviceId(), deviceName, claimRequest, e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}] Failed to process device claiming command: {}", sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}] Devices claim messages is empty for: [{}]", sessionId, gateway.getDeviceId());
                throw new IllegalArgumentException("[" + sessionId + "] Devices claim messages is empty for [" + gateway.getDeviceId() + "]");
            }
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void processClaimDeviceMsg(GatewayDeviceSessionCtx deviceCtx, TransportProtos.ClaimDeviceMsg claimDeviceMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), claimDeviceMsg, getPubAckCallback(channel, deviceName, msgId, claimDeviceMsg));
    }

    private void onDeviceAttributesJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Futures.addCallback(checkDeviceConnected(deviceName),
                        new FutureCallback<GatewayDeviceSessionCtx>() {
                            @Override
                            public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                                if (!deviceEntry.getValue().isJsonObject()) {
                                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                                }
                                TransportProtos.PostAttributeMsg postAttributeMsg = JsonConverter.convertToAttributesProto(deviceEntry.getValue().getAsJsonObject());
                                processPostAttributesMsg(deviceCtx, postAttributeMsg, deviceName, msgId);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}] Failed to process device attributes command: {}", sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    private void onDeviceAttributesProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayAttributesMsg attributesMsgProto = TransportApiProtos.GatewayAttributesMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.AttributesMsg> attributesMsgList = attributesMsgProto.getMsgList();
            if (!CollectionUtils.isEmpty(attributesMsgList)) {
                attributesMsgList.forEach(attributesMsg -> {
                    String deviceName = checkDeviceName(attributesMsg.getDeviceName());
                    Futures.addCallback(checkDeviceConnected(deviceName),
                            new FutureCallback<GatewayDeviceSessionCtx>() {
                                @Override
                                public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                                    TransportProtos.PostAttributeMsg kvListProto = attributesMsg.getMsg();
                                    if (kvListProto == null) {
                                        throw new IllegalArgumentException("Attributes List for device: " + deviceName + " is empty!");
                                    }
                                    try {
                                        TransportProtos.PostAttributeMsg postAttributeMsg = ProtoConverter.validatePostAttributeMsg(kvListProto.toByteArray());
                                        processPostAttributesMsg(deviceCtx, postAttributeMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}] Failed to process device attributes command: {}", gateway.getDeviceId(), deviceName, kvListProto, e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}] Failed to process device attributes command: {}", sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}] Devices attributes keys list is empty for: [{}]", sessionId, gateway.getDeviceId());
                throw new IllegalArgumentException("[" + sessionId + "] Devices attributes keys list is empty for [" + gateway.getDeviceId() + "]");
            }
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void processPostAttributesMsg(GatewayDeviceSessionCtx deviceCtx, TransportProtos.PostAttributeMsg postAttributeMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), postAttributeMsg, getPubAckCallback(channel, deviceName, msgId, postAttributeMsg));
    }

    private void onDeviceAttributesRequestJson(MqttPublishMessage msg) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, msg.payload());
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            int requestId = jsonObj.get("id").getAsInt();
            String deviceName = jsonObj.get(DEVICE_PROPERTY).getAsString();
            boolean clientScope = jsonObj.get("client").getAsBoolean();
            Set<String> keys;
            if (jsonObj.has("key")) {
                keys = Collections.singleton(jsonObj.get("key").getAsString());
            } else {
                JsonArray keysArray = jsonObj.get("keys").getAsJsonArray();
                keys = new HashSet<>();
                for (JsonElement keyObj : keysArray) {
                    keys.add(keyObj.getAsString());
                }
            }
            TransportProtos.GetAttributeRequestMsg requestMsg = toGetAttributeRequestMsg(requestId, clientScope, keys);
            processGetAttributeRequestMessage(msg, deviceName, requestMsg);
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    private void onDeviceAttributesRequestProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = TransportApiProtos.GatewayAttributesRequestMsg.parseFrom(getBytes(mqttMsg.payload()));
            String deviceName = checkDeviceName(gatewayAttributesRequestMsg.getDeviceName());
            int requestId = gatewayAttributesRequestMsg.getId();
            boolean clientScope = gatewayAttributesRequestMsg.getClient();
            ProtocolStringList keysList = gatewayAttributesRequestMsg.getKeysList();
            Set<String> keys = new HashSet<>(keysList);
            TransportProtos.GetAttributeRequestMsg requestMsg = toGetAttributeRequestMsg(requestId, clientScope, keys);
            processGetAttributeRequestMessage(mqttMsg, deviceName, requestMsg);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void onDeviceRpcResponseJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            String deviceName = jsonObj.get(DEVICE_PROPERTY).getAsString();
            Futures.addCallback(checkDeviceConnected(deviceName),
                    new FutureCallback<GatewayDeviceSessionCtx>() {
                        @Override
                        public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                            Integer requestId = jsonObj.get("id").getAsInt();
                            String data = jsonObj.get("data").toString();
                            TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                                    .setRequestId(requestId).setPayload(data).build();
                            processRpcResponseMsg(deviceCtx, rpcResponseMsg, deviceName, msgId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.debug("[{}] Failed to process device Rpc response command: {}", sessionId, deviceName, t);
                        }
                    }, context.getExecutor());
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    private void onDeviceRpcResponseProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayRpcResponseMsg gatewayRpcResponseMsg = TransportApiProtos.GatewayRpcResponseMsg.parseFrom(getBytes(payload));
            String deviceName = checkDeviceName(gatewayRpcResponseMsg.getDeviceName());
            Futures.addCallback(checkDeviceConnected(deviceName),
                    new FutureCallback<GatewayDeviceSessionCtx>() {
                        @Override
                        public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                            Integer requestId = gatewayRpcResponseMsg.getId();
                            String data = gatewayRpcResponseMsg.getData();
                            TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                                    .setRequestId(requestId).setPayload(data).build();
                            processRpcResponseMsg(deviceCtx, rpcResponseMsg, deviceName, msgId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.debug("[{}] Failed to process device Rpc response command: {}", sessionId, deviceName, t);
                        }
                    }, context.getExecutor());
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void processRpcResponseMsg(GatewayDeviceSessionCtx deviceCtx, TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(channel, deviceName, msgId, rpcResponseMsg));
    }

    private void processGetAttributeRequestMessage(MqttPublishMessage mqttMsg, String deviceName, TransportProtos.GetAttributeRequestMsg requestMsg) {
        int msgId = getMsgId(mqttMsg);
        Futures.addCallback(checkDeviceConnected(deviceName),
                new FutureCallback<GatewayDeviceSessionCtx>() {
                    @Override
                    public void onSuccess(@Nullable GatewayDeviceSessionCtx deviceCtx) {
                        transportService.process(deviceCtx.getSessionInfo(), requestMsg, getPubAckCallback(channel, deviceName, msgId, requestMsg));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ack(mqttMsg);
                        log.debug("[{}] Failed to process device attributes request command: {}", sessionId, deviceName, t);
                    }
                }, context.getExecutor());
    }

    private TransportProtos.GetAttributeRequestMsg toGetAttributeRequestMsg(int requestId, boolean clientScope, Set<String> keys) {
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        result.setRequestId(requestId);

        if (clientScope) {
            result.addAllClientAttributeNames(keys);
        } else {
            result.addAllSharedAttributeNames(keys);
        }
        return result.build();
    }

    private ListenableFuture<GatewayDeviceSessionCtx> checkDeviceConnected(String deviceName) {
        GatewayDeviceSessionCtx ctx = devices.get(deviceName);
        if (ctx == null) {
            log.debug("[{}] Missing device [{}] for the gateway session", sessionId, deviceName);
            return onDeviceConnect(deviceName, DEFAULT_DEVICE_TYPE);
        } else {
            return Futures.immediateFuture(ctx);
        }
    }

    private String checkDeviceName(String deviceName) {
        if (StringUtils.isEmpty(deviceName)) {
            throw new RuntimeException("Device name is empty!");
        } else {
            return deviceName;
        }
    }

    private String getDeviceName(JsonElement json) {
        return json.getAsJsonObject().get(DEVICE_PROPERTY).getAsString();
    }

    private String getDeviceType(JsonElement json) {
        JsonElement type = json.getAsJsonObject().get("type");
        return type == null || type instanceof JsonNull ? DEFAULT_DEVICE_TYPE : type.getAsString();
    }

    private JsonElement getJson(MqttPublishMessage mqttMsg) throws AdaptorException {
        return JsonMqttAdaptor.validateJsonPayload(sessionId, mqttMsg.payload());
    }

    private byte[] getBytes(ByteBuf payload) {
        return ProtoMqttAdaptor.toBytes(payload);
    }

    private void ack(MqttPublishMessage msg) {
        int msgId = getMsgId(msg);
        if (msgId > 0) {
            writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(msgId));
        }
    }

    private void deregisterSession(String deviceName, GatewayDeviceSessionCtx deviceSessionCtx) {
        transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
        transportService.process(deviceSessionCtx.getSessionInfo(), DefaultTransportService.getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
        log.debug("[{}] Removed device [{}] from the gateway session", sessionId, deviceName);
    }

    private <T> TransportServiceCallback<Void> getPubAckCallback(final ChannelHandlerContext ctx, final String deviceName, final int msgId, final T msg) {
        return new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}][{}] Published msg: {}", sessionId, deviceName, msg);
                if (msgId > 0) {
                    ctx.writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(msgId));
                }
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {} for device: {}", sessionId, msg, deviceName, e);
                ctx.close();
            }
        };
    }
}
