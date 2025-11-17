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
package org.thingsboard.server.transport.mqtt.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.adaptor.ProtoConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.gateway.metrics.GatewayMetadata;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;
import org.thingsboard.server.transport.mqtt.gateway.GatewayMetricsService;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import static org.thingsboard.server.common.data.DataConstants.DEFAULT_DEVICE_TYPE;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.OFFLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.STATE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.messageName;

@Slf4j
public abstract class AbstractGatewaySessionHandler<T extends AbstractGatewayDeviceSessionContext> {

    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";
    private static final String DEVICE_PROPERTY = "device";
    public static final String TELEMETRY = "telemetry";
    public static final String CLAIMING = "claiming";
    public static final String ATTRIBUTE = "attribute";
    public static final String RPC_RESPONSE = "Rpc response";
    public static final String ATTRIBUTES_REQUEST = "attributes request";

    protected final MqttTransportContext context;
    protected final TransportService transportService;
    protected final TransportDeviceInfo gateway;
    @Getter
    protected final UUID sessionId;
    private final ConcurrentMap<String, Lock> deviceCreationLockMap;
    @Getter
    private final ConcurrentMap<String, T> devices;
    private final ConcurrentMap<String, ListenableFuture<T>> deviceFutures;
    protected final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;
    @Getter
    protected final ChannelHandlerContext channel;
    protected final DeviceSessionCtx deviceSessionCtx;
    protected final GatewayMetricsService gatewayMetricsService;

    @Getter
    @Setter
    private boolean overwriteDevicesActivity = false;

    public AbstractGatewaySessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId, boolean overwriteDevicesActivity) {
        log.debug("[{}] Gateway connect [{}] session [{}]", deviceSessionCtx.getTenantId(), deviceSessionCtx.getDeviceId(), sessionId);
        this.context = deviceSessionCtx.getContext();
        this.transportService = context.getTransportService();
        this.deviceSessionCtx = deviceSessionCtx;
        this.gateway = deviceSessionCtx.getDeviceInfo();
        this.sessionId = sessionId;
        this.devices = new ConcurrentHashMap<>();
        this.deviceFutures = new ConcurrentHashMap<>();
        this.deviceCreationLockMap = createWeakMap();
        this.mqttQoSMap = deviceSessionCtx.getMqttQoSMap();
        this.channel = deviceSessionCtx.getChannel();
        this.overwriteDevicesActivity = overwriteDevicesActivity;
        this.gatewayMetricsService = deviceSessionCtx.getContext().getGatewayMetricsService();
    }

    ConcurrentReferenceHashMap<String, Lock> createWeakMap() {
        return new ConcurrentReferenceHashMap<>(16, ReferenceType.WEAK);
    }

    public void onDeviceDisconnect(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceDisconnectJson(mqttMsg);
        } else {
            onGatewayDeviceDisconnectProto(mqttMsg);
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

    public void onGatewayPing() {
        if (overwriteDevicesActivity) {
            devices.forEach((deviceName, deviceSessionCtx) -> transportService.recordActivity(deviceSessionCtx.getSessionInfo()));
        }
    }

    public void onDevicesDisconnect() {
        log.debug("[{}] Gateway disconnect [{}]", gateway.getTenantId(), gateway.getDeviceId());
        try {
            deviceFutures.forEach((name, future) -> {
                Futures.addCallback(future, new FutureCallback<T>() {
                    @Override
                    public void onSuccess(T result) {
                        log.debug("[{}] Gateway disconnect [{}] device deregister callback [{}]", gateway.getTenantId(), gateway.getDeviceId(), name);
                        deregisterSession(name, result);
                    }

                    @Override
                    public void onFailure(Throwable t) {

                    }
                }, MoreExecutors.directExecutor());
            });

            devices.forEach(this::deregisterSession);
        } catch (Exception e) {
            log.error("Gateway disconnect failure", e);
        }
    }

    public void onDeviceDeleted(String deviceName) {
        deregisterSession(deviceName);
    }

    public String getNodeId() {
        return context.getNodeId();
    }

    public MqttTransportAdaptor getPayloadAdaptor() {
        return deviceSessionCtx.getPayloadAdaptor();
    }

    void deregisterSession(String deviceName) {
        MqttDeviceAwareSessionContext deviceSessionCtx = devices.remove(deviceName);
        if (deviceSessionCtx != null) {
            deregisterSession(deviceName, deviceSessionCtx);
        } else {
            log.debug("[{}][{}][{}] Device [{}] was already removed from the gateway session", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
        }
    }

    public ChannelFuture writeAndFlush(MqttMessage mqttMessage) {
        return channel.writeAndFlush(mqttMessage);
    }

    int nextMsgId() {
        return deviceSessionCtx.nextMsgId();
    }

    protected boolean isJsonPayloadType() {
        return deviceSessionCtx.isJsonPayloadType();
    }

    protected void processOnConnect(MqttPublishMessage msg, String deviceName, String deviceType) {
        log.trace("[{}][{}][{}] onDeviceConnect: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
        process(onDeviceConnect(deviceName, deviceType),
                result -> {
                    ack(msg, MqttReasonCodes.PubAck.SUCCESS);
                    log.trace("[{}][{}][{}] onDeviceConnectOk: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
                },
                t -> logDeviceCreationError(t, deviceName));
    }

    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        log.trace("[{}][{}] onDeviceUpdate: [{}]", gateway.getTenantId(), gateway.getDeviceId(), device);
        JsonNode deviceAdditionalInfo = device.getAdditionalInfo();
        if (deviceAdditionalInfo.has(DataConstants.GATEWAY_PARAMETER) && deviceAdditionalInfo.has(DataConstants.OVERWRITE_ACTIVITY_TIME_PARAMETER)) {
            overwriteDevicesActivity = deviceAdditionalInfo.get(DataConstants.OVERWRITE_ACTIVITY_TIME_PARAMETER).asBoolean();
        }
    }

    ListenableFuture<T> onDeviceConnect(String deviceName, String deviceType) {
        T result = devices.get(deviceName);
        if (result == null) {
            Lock deviceCreationLock = deviceCreationLockMap.computeIfAbsent(deviceName, s -> new ReentrantLock());
            deviceCreationLock.lock();
            try {
                result = devices.get(deviceName);
                if (result == null) {
                    return getDeviceCreationFuture(deviceName, deviceType);
                } else {
                    return Futures.immediateFuture(result);
                }
            } finally {
                deviceCreationLock.unlock();
            }
        } else {
            return Futures.immediateFuture(result);
        }
    }

    private ListenableFuture<T> getDeviceCreationFuture(String deviceName, String deviceType) {
        final SettableFuture<T> futureToSet = SettableFuture.create();
        ListenableFuture<T> future = deviceFutures.putIfAbsent(deviceName, futureToSet);
        if (future != null) {
            return future;
        }
        try {
            transportService.process(gateway.getTenantId(),
                    GetOrCreateDeviceFromGatewayRequestMsg.newBuilder()
                            .setDeviceName(deviceName)
                            .setDeviceType(deviceType)
                            .setGatewayIdMSB(gateway.getDeviceId().getId().getMostSignificantBits())
                            .setGatewayIdLSB(gateway.getDeviceId().getId().getLeastSignificantBits())
                            .build(),
                    new TransportServiceCallback<>() {
                        @Override
                        public void onSuccess(GetOrCreateDeviceFromGatewayResponse msg) {
                            T deviceSessionCtx = newDeviceSessionCtx(msg);
                            if (devices.putIfAbsent(deviceName, deviceSessionCtx) == null) {
                                log.trace("[{}][{}][{}] First got or created device [{}], type [{}] for the gateway session", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, deviceType);
                                SessionInfoProto deviceSessionInfo = deviceSessionCtx.getSessionInfo();
                                transportService.registerAsyncSession(deviceSessionInfo, deviceSessionCtx);
                                /**
                                 *  3.0.0 Device Session Establishment:
                                 * dcmd-subscribe
                                 * [tck-id-message-flow-device-dcmd-subscribe] If the Device supports writing to outputs, the
                                 * MQTT client associated with the Device MUST subscribe to a topic of the form
                                 * spBv1.0/group_id/DCMD/edge_node_id/device_id where group_id is the Sparkplug Group ID
                                 * the edge_node_id is the Sparkplug Edge Node ID and the device_id is the Sparkplug Device ID
                                 * for this Device. It MUST subscribe on this topic with a QoS of 1
                                 */
                                transportService.process(TransportProtos.TransportToDeviceActorMsg.newBuilder()
                                        .setSessionInfo(deviceSessionInfo)
                                        .setSessionEvent(SESSION_EVENT_MSG_OPEN)
                                        .setSubscribeToAttributes(SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG)
                                        .setSubscribeToRPC(SUBSCRIBE_TO_RPC_ASYNC_MSG)
                                        .build(), null);
                            }
                            futureToSet.set(devices.get(deviceName));
                            deviceFutures.remove(deviceName);
                        }

                        @Override
                        public void onError(Throwable t) {
                            logDeviceCreationError(t, deviceName);
                            futureToSet.setException(t);
                            deviceFutures.remove(deviceName);
                        }
                    });
            return futureToSet;
        } catch (Throwable e) {
            deviceFutures.remove(deviceName);
            throw e;
        }
    }

    private void logDeviceCreationError(Throwable t, String deviceName) {
        if (DataConstants.MAXIMUM_NUMBER_OF_DEVICES_REACHED.equals(t.getMessage())) {
            log.info("[{}][{}][{}] Failed to process device connect command: [{}] due to [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName,
                    DataConstants.MAXIMUM_NUMBER_OF_DEVICES_REACHED);
        } else {
            log.warn("[{}][{}][{}] Failed to process device connect command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
        }
    }

    protected abstract T newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg);

    protected int getMsgId(MqttPublishMessage mqttMsg) {
        return mqttMsg.variableHeader().packetId();
    }

    protected void onDeviceConnectJson(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = getJson(mqttMsg);
        String deviceName = checkDeviceName(getDeviceName(json));
        String deviceType = getDeviceType(json);
        processOnConnect(mqttMsg, deviceName, deviceType);
    }

    protected void onDeviceConnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
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

    protected void onGatewayDeviceDisconnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            TransportApiProtos.DisconnectMsg connectProto = TransportApiProtos.DisconnectMsg.parseFrom(getBytes(mqttMsg.payload()));
            String deviceName = checkDeviceName(connectProto.getDeviceName());
            processOnDisconnect(mqttMsg, deviceName);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    void processOnDisconnect(MqttPublishMessage msg, String deviceName) {
        deregisterSession(deviceName);
        ack(msg, MqttReasonCodes.PubAck.SUCCESS);
    }

    protected void onDeviceTelemetryJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        validateJsonObject(json);

        List<Map.Entry<String, JsonElement>> deviceEntries = json.getAsJsonObject().entrySet().stream()
                .filter(entry -> {
                    final boolean isArray = entry.getValue().isJsonArray();
                    if (!isArray) {
                        log.warn("{} device='{}' value={}", CAN_T_PARSE_VALUE, entry.getKey(), entry.getValue());
                    }
                    return isArray;
                })
                .toList();

        if (deviceEntries.isEmpty()) {
            log.debug("[{}][{}][{}] Devices telemetry message is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
            throw new IllegalArgumentException("[" + sessionId + "] Devices telemetry message is empty for [" + gateway.getDeviceId() + "]");
        }

        AtomicInteger remaining = new AtomicInteger(deviceEntries.size());
        AtomicBoolean ackSent = new AtomicBoolean(false);

        for (Map.Entry<String, JsonElement> deviceEntry : deviceEntries) {
            String deviceName = deviceEntry.getKey();
            process(deviceName, deviceCtx -> processPostTelemetryMsg(deviceCtx, deviceEntry.getValue(), deviceName, msgId,
                            remaining, ackSent),
                    t -> processFailure(msgId, deviceName, TELEMETRY, ackSent, t));
        }
    }

    private void processPostTelemetryMsg(T deviceCtx, JsonElement msg, String deviceName, int msgId, AtomicInteger remaining, AtomicBoolean ackSent) {
        try {
            long systemTs = System.currentTimeMillis();
            TbPair<TransportProtos.PostTelemetryMsg, List<GatewayMetadata>> gatewayPayloadPair = JsonConverter.convertToGatewayTelemetry(msg.getAsJsonArray(), systemTs);
            TransportProtos.PostTelemetryMsg postTelemetryMsg = gatewayPayloadPair.getFirst();
            List<GatewayMetadata> metadata = gatewayPayloadPair.getSecond();
            if (!CollectionUtils.isEmpty(metadata)) {
                gatewayMetricsService.process(deviceSessionCtx.getSessionInfo(), gateway.getDeviceId(), metadata, systemTs);
            }
            transportService.process(deviceCtx.getSessionInfo(), postTelemetryMsg, getAggregatePubAckCallback(channel, msgId, deviceName, postTelemetryMsg, remaining, ackSent));
        } catch (Throwable e) {
            log.warn("[{}][{}][{}] Failed to convert telemetry: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, msg, e);
            ackOrClose(msgId, ackSent);
        }
    }

    protected void onDeviceTelemetryProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayTelemetryMsg telemetryMsgProto = TransportApiProtos.GatewayTelemetryMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.TelemetryMsg> deviceMsgList = telemetryMsgProto.getMsgList();
            if (CollectionUtils.isEmpty(deviceMsgList)) {
                log.debug("[{}][{}][{}] Devices telemetry messages is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
                throw new IllegalArgumentException("[" + sessionId + "] Devices telemetry messages is empty for [" + gateway.getDeviceId() + "]");
            }

            AtomicInteger remaining = new AtomicInteger(deviceMsgList.size());
            AtomicBoolean ackSent = new AtomicBoolean(false);

            deviceMsgList.forEach(telemetryMsg -> {
                String deviceName = checkDeviceName(telemetryMsg.getDeviceName());
                process(deviceName, deviceCtx -> processPostTelemetryMsg(deviceCtx, telemetryMsg.getMsg(), deviceName, msgId,
                                remaining, ackSent),
                        t -> processFailure(msgId, deviceName, TELEMETRY, ackSent, t));
            });
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    protected void processPostTelemetryMsg(MqttDeviceAwareSessionContext deviceCtx, TransportProtos.PostTelemetryMsg msg, String deviceName, int msgId,
                                           AtomicInteger remaining, AtomicBoolean ackSent) {
        try {
            TransportProtos.PostTelemetryMsg postTelemetryMsg = ProtoConverter.validatePostTelemetryMsg(msg.toByteArray());
            transportService.process(deviceCtx.getSessionInfo(), postTelemetryMsg, getAggregatePubAckCallback(channel, msgId, deviceName, postTelemetryMsg, remaining, ackSent));
        } catch (Throwable e) {
            log.warn("[{}][{}][{}] Failed to convert telemetry: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, msg, e);
            ackOrClose(msgId, ackSent);
        }
    }

    public TransportProtos.PostTelemetryMsg postTelemetryMsgCreated(TransportProtos.KeyValueProto keyValueProto, long ts) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        result.add(keyValueProto);
        TransportProtos.PostTelemetryMsg.Builder request = TransportProtos.PostTelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
        builder.setTs(ts);
        builder.addAllKv(result);
        request.addTsKvList(builder.build());
        return request.build();
    }

    private void onDeviceClaimJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        validateJsonObject(json);

        List<Map.Entry<String, JsonElement>> deviceEntries = json.getAsJsonObject().entrySet().stream()
                .filter(entry -> {
                    boolean isJsonObject = entry.getValue().isJsonObject();
                    if (!isJsonObject) {
                        log.warn("{} device='{}' value={}", CAN_T_PARSE_VALUE, entry.getKey(), entry.getValue());
                    }
                    return isJsonObject;
                })
                .toList();

        if (deviceEntries.isEmpty()) {
            log.debug("[{}][{}][{}] Devices claim message is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
            throw new IllegalArgumentException("[" + sessionId + "] Devices claim message is empty for [" + gateway.getDeviceId() + "]");
        }

        AtomicInteger remaining = new AtomicInteger(deviceEntries.size());
        AtomicBoolean ackSent = new AtomicBoolean(false);

        for (Map.Entry<String, JsonElement> deviceEntry : deviceEntries) {
            String deviceName = deviceEntry.getKey();
            process(deviceName, deviceCtx -> processClaimDeviceMsg(deviceCtx, deviceEntry.getValue(), deviceName, msgId,
                            remaining, ackSent),
                    t -> processFailure(msgId, deviceName, CLAIMING, ackSent, t));
        }
    }

    private void processClaimDeviceMsg(MqttDeviceAwareSessionContext deviceCtx, JsonElement claimRequest, String deviceName, int msgId,
                                       AtomicInteger remaining, AtomicBoolean ackSent) {
        try {
            DeviceId deviceId = deviceCtx.getDeviceId();
            TransportProtos.ClaimDeviceMsg claimDeviceMsg = JsonConverter.convertToClaimDeviceProto(deviceId, claimRequest);
            transportService.process(deviceCtx.getSessionInfo(), claimDeviceMsg, getAggregatePubAckCallback(channel, msgId, deviceName, claimDeviceMsg, remaining, ackSent));
        } catch (Throwable e) {
            log.warn("[{}][{}][{}] Failed to convert claim message: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, claimRequest, e);
            ackOrClose(msgId, ackSent);
        }
    }

    private void onDeviceClaimProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayClaimMsg claimMsgProto = TransportApiProtos.GatewayClaimMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.ClaimDeviceMsg> claimMsgList = claimMsgProto.getMsgList();
            if (CollectionUtils.isEmpty(claimMsgList)) {
                log.debug("[{}][{}][{}] Devices claim messages is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
                throw new IllegalArgumentException("[" + sessionId + "] Devices claim messages is empty for [" + gateway.getDeviceId() + "]");
            }

            AtomicInteger remaining = new AtomicInteger(claimMsgList.size());
            AtomicBoolean ackSent = new AtomicBoolean(false);

            claimMsgList.forEach(claimDeviceMsg -> {
                String deviceName = checkDeviceName(claimDeviceMsg.getDeviceName());
                process(deviceName, deviceCtx -> processClaimDeviceMsg(deviceCtx, claimDeviceMsg.getClaimRequest(), deviceName, msgId,
                                remaining, ackSent),
                        t -> processFailure(msgId, deviceName, CLAIMING, ackSent, t));
            });
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void processClaimDeviceMsg(MqttDeviceAwareSessionContext deviceCtx, TransportApiProtos.ClaimDevice claimRequest, String deviceName, int msgId,
                                       AtomicInteger remaining, AtomicBoolean ackSent) {
        try {
            DeviceId deviceId = deviceCtx.getDeviceId();
            TransportProtos.ClaimDeviceMsg claimDeviceMsg = ProtoConverter.convertToClaimDeviceProto(deviceId, claimRequest.toByteArray());
            transportService.process(deviceCtx.getSessionInfo(), claimDeviceMsg, getAggregatePubAckCallback(channel, msgId, deviceName, claimDeviceMsg, remaining, ackSent));
        } catch (Throwable e) {
            log.warn("[{}][{}][{}] Failed to convert claim message: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, claimRequest, e);
            ackOrClose(msgId, ackSent);
        }
    }

    private void onDeviceAttributesJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        validateJsonObject(json);

        List<Map.Entry<String, JsonElement>> deviceEntries = json.getAsJsonObject().entrySet().stream()
                .filter(entry -> {
                    boolean isJsonObject = entry.getValue().isJsonObject();
                    if (!isJsonObject) {
                        log.warn("{} device='{}' value={}", CAN_T_PARSE_VALUE, entry.getKey(), entry.getValue());
                    }
                    return isJsonObject;
                })
                .toList();

        if (deviceEntries.isEmpty()) {
            log.debug("[{}][{}][{}] Devices attribute message is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
            throw new IllegalArgumentException("[" + sessionId + "] Devices attribute message is empty for [" + gateway.getDeviceId() + "]");
        }

        AtomicInteger remaining = new AtomicInteger(deviceEntries.size());
        AtomicBoolean ackSent = new AtomicBoolean(false);

        for (Map.Entry<String, JsonElement> deviceEntry : deviceEntries) {
            String deviceName = deviceEntry.getKey();
            process(deviceName, deviceCtx -> processPostAttributesMsg(deviceCtx, deviceEntry.getValue(), deviceName, msgId,
                            remaining, ackSent),
                    t -> processFailure(msgId, deviceName, ATTRIBUTE, ackSent, t));
        }
    }

    private void processPostAttributesMsg(MqttDeviceAwareSessionContext deviceCtx, JsonElement msg, String deviceName, int msgId,
                                          AtomicInteger remaining, AtomicBoolean ackSent) {
        try {
            TransportProtos.PostAttributeMsg postAttributeMsg = JsonConverter.convertToAttributesProto(msg.getAsJsonObject());
            transportService.process(deviceCtx.getSessionInfo(), postAttributeMsg, getAggregatePubAckCallback(channel, msgId, deviceName, postAttributeMsg, remaining, ackSent));
        } catch (Throwable e) {
            log.warn("[{}][{}][{}] Failed to process device attributes command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, msg, e);
            ackOrClose(msgId, ackSent);
        }
    }

    private void onDeviceAttributesProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayAttributesMsg attributesMsgProto = TransportApiProtos.GatewayAttributesMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.AttributesMsg> attributesMsgList = attributesMsgProto.getMsgList();
            if (CollectionUtils.isEmpty(attributesMsgList)) {
                log.debug("[{}][{}][{}] Devices attributes keys list is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
                throw new IllegalArgumentException("[" + sessionId + "] Devices attributes keys list is empty for [" + gateway.getDeviceId() + "]");
            }

            AtomicInteger remaining = new AtomicInteger(attributesMsgList.size());
            AtomicBoolean ackSent = new AtomicBoolean(false);

            attributesMsgList.forEach(attributesMsg -> {
                String deviceName = checkDeviceName(attributesMsg.getDeviceName());
                process(deviceName, deviceCtx -> processPostAttributesMsg(deviceCtx, attributesMsg.getMsg(), deviceName, msgId,
                                remaining, ackSent),
                        t -> processFailure(msgId, deviceName, ATTRIBUTE, ackSent, t));
            });
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    protected void processPostAttributesMsg(MqttDeviceAwareSessionContext deviceCtx, TransportProtos.PostAttributeMsg kvListProto, String deviceName, int msgId,
                                            AtomicInteger remaining, AtomicBoolean ackSent) {
        try {
            TransportProtos.PostAttributeMsg postAttributeMsg = ProtoConverter.validatePostAttributeMsg(kvListProto);
            transportService.process(deviceCtx.getSessionInfo(), postAttributeMsg, getAggregatePubAckCallback(channel, msgId, deviceName, postAttributeMsg, remaining, ackSent));
        } catch (Throwable e) {
            log.warn("[{}][{}][{}] Failed to process device attributes command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, kvListProto, e);
            ackOrClose(msgId, ackSent);
        }
    }

    private void onDeviceAttributesRequestJson(MqttPublishMessage msg) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, msg.payload());
        validateJsonObject(json);
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
        validateJsonObject(json);
        JsonObject jsonObj = json.getAsJsonObject();
        String deviceName = jsonObj.get(DEVICE_PROPERTY).getAsString();
        Integer requestId = jsonObj.get("id").getAsInt();
        String data = jsonObj.get("data").toString();
        onDeviceRpcResponse(requestId, data, deviceName, msgId);
    }

    private static void validateJsonObject(JsonElement json) {
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    private void onDeviceRpcResponseProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayRpcResponseMsg gatewayRpcResponseMsg = TransportApiProtos.GatewayRpcResponseMsg.parseFrom(getBytes(payload));
            String deviceName = checkDeviceName(gatewayRpcResponseMsg.getDeviceName());
            Integer requestId = gatewayRpcResponseMsg.getId();
            String data = gatewayRpcResponseMsg.getData();
            onDeviceRpcResponse(requestId, data, deviceName, msgId);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    private void onDeviceRpcResponse(Integer requestId, String data, String deviceName, int msgId) {
        AtomicInteger remaining = new AtomicInteger(1);
        AtomicBoolean ackSent = new AtomicBoolean(false);
        process(deviceName, deviceCtx -> processRpcResponseMsg(deviceCtx, requestId, data, deviceName, msgId, remaining, ackSent),
                t -> processFailure(msgId, deviceName, RPC_RESPONSE, ackSent, t));
    }

    private void processRpcResponseMsg(MqttDeviceAwareSessionContext deviceCtx, Integer requestId, String data, String deviceName,
                                       int msgId, AtomicInteger remaining, AtomicBoolean ackSent) {
        TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                .setRequestId(requestId).setPayload(data).build();
        transportService.process(deviceCtx.getSessionInfo(), rpcResponseMsg,
                getAggregatePubAckCallback(channel, msgId, deviceName, rpcResponseMsg, remaining, ackSent));
    }

    private void processGetAttributeRequestMessage(MqttPublishMessage mqttMsg, String deviceName, TransportProtos.GetAttributeRequestMsg requestMsg) {
        int msgId = getMsgId(mqttMsg);
        AtomicInteger remaining = new AtomicInteger(1);
        AtomicBoolean ackSent = new AtomicBoolean(false);
        process(deviceName, deviceCtx -> {
                    processGetAttributeRequestMessage(deviceCtx, requestMsg, deviceName, msgId, remaining, ackSent);
                },
                t -> processFailure(msgId, deviceName, ATTRIBUTES_REQUEST, ackSent, MqttReasonCodes.PubAck.IMPLEMENTATION_SPECIFIC_ERROR, t));
    }

    private void processGetAttributeRequestMessage(T deviceCtx, TransportProtos.GetAttributeRequestMsg requestMsg,
                                                   String deviceName, int msgId, AtomicInteger remaining, AtomicBoolean ackSent) {
        transportService.process(deviceCtx.getSessionInfo(), requestMsg,
                getAggregatePubAckCallback(channel, msgId, deviceName, requestMsg, remaining, ackSent));
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

    protected String checkDeviceName(String deviceName) {
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

    protected byte[] getBytes(ByteBuf payload) {
        return ProtoMqttAdaptor.toBytes(payload);
    }

    protected void ack(MqttPublishMessage msg, MqttReasonCodes.PubAck returnCode) {
        int msgId = getMsgId(msg);
        ack(msgId, returnCode);
    }

    protected void ack(int msgId, MqttReasonCodes.PubAck returnCode) {
        if (msgId > 0) {
            writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(deviceSessionCtx, msgId, returnCode.byteValue()));
        }
    }

    protected void ackOrClose(int msgId, AtomicBoolean ackSent) {
        if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
            if (ackSent.compareAndSet(false, true)) {
                ack(msgId, MqttReasonCodes.PubAck.PAYLOAD_FORMAT_INVALID);
            }
        } else {
            channel.close();
        }
    }

    private void deregisterSession(String deviceName, MqttDeviceAwareSessionContext deviceSessionCtx) {
        if (this.deviceSessionCtx.isSparkplug()) {
            sendSparkplugStateOnTelemetry(deviceSessionCtx.getSessionInfo(),
                    deviceSessionCtx.getDeviceInfo().getDeviceName(), OFFLINE, new Date().getTime());
        }
        transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
        transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_CLOSED, null);
        log.debug("[{}][{}][{}] Removed device [{}] from the gateway session", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
    }

    public void sendSparkplugStateOnTelemetry(TransportProtos.SessionInfoProto sessionInfo, String deviceName, SparkplugConnectionState connectionState, long ts) {
        TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
        keyValueProtoBuilder.setKey(messageName(STATE));
        keyValueProtoBuilder.setType(TransportProtos.KeyValueType.STRING_V);
        keyValueProtoBuilder.setStringV(connectionState.name());
        TransportProtos.PostTelemetryMsg postTelemetryMsg = postTelemetryMsgCreated(keyValueProtoBuilder.build(), ts);
        TransportServiceCallback<Void> pubAckCallback = getAggregatePubAckCallback(channel, -1, deviceName, postTelemetryMsg,
                new AtomicInteger(1), new AtomicBoolean(false));
        transportService.process(sessionInfo, postTelemetryMsg, pubAckCallback);
    }

    protected <T> TransportServiceCallback<Void> getAggregatePubAckCallback(
            final ChannelHandlerContext ctx,
            final int msgId,
            final String deviceName,
            final T msg,
            final AtomicInteger remaining,
            final AtomicBoolean ackSent) {

        return new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}][{}][{}][{}] Published msg: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, msg);
                if (remaining.decrementAndGet() == 0 && ackSent.compareAndSet(false, true)) {
                    if (msgId > 0) {
                        ctx.writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(
                                deviceSessionCtx, msgId, MqttReasonCodes.PubAck.SUCCESS.byteValue()));
                    } else {
                        log.trace("[{}][{}][{}] Wrong msg id: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, msgId);
                        ctx.writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(
                                deviceSessionCtx, msgId, MqttReasonCodes.PubAck.UNSPECIFIED_ERROR.byteValue()));
                    }
                }
                if (msgId <= 0) {
                    closeDeviceSession(deviceName, MqttReasonCodes.Disconnect.MALFORMED_PACKET);
                }
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}][{}][{}] Failed to publish msg: [{}] for device: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, msg, deviceName, e);
                if (e instanceof TbRateLimitsException) {
                    if (ackSent.compareAndSet(false, true)) {
                        ctx.writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(
                                deviceSessionCtx, msgId, MqttReasonCodes.PubAck.QUOTA_EXCEEDED.byteValue()));
                        ctx.close();
                    }
                    closeDeviceSession(deviceName, MqttReasonCodes.Disconnect.MESSAGE_RATE_TOO_HIGH);
                } else {
                    if (ackSent.compareAndSet(false, true)) {
                        ctx.writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(
                                deviceSessionCtx, msgId, MqttReasonCodes.PubAck.UNSPECIFIED_ERROR.byteValue()));
                        ctx.close();
                    }
                    closeDeviceSession(deviceName, MqttReasonCodes.Disconnect.UNSPECIFIED_ERROR);
                }
            }
        };
    }

    protected void process(String deviceName, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        ListenableFuture<T> deviceCtxFuture = onDeviceConnect(deviceName, DEFAULT_DEVICE_TYPE);
        process(deviceCtxFuture, onSuccess, onFailure);
    }

    @SneakyThrows
    protected <T> void process(ListenableFuture<T> deviceCtxFuture, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        if (deviceCtxFuture.isDone()) {
            onSuccess.accept(deviceCtxFuture.get());
        } else {
            DonAsynchron.withCallback(deviceCtxFuture, onSuccess, onFailure, context.getExecutor());
        }
    }

    protected void processFailure(int msgId, String deviceName, String msgType, AtomicBoolean ackSent, Throwable t) {
        if (DataConstants.MAXIMUM_NUMBER_OF_DEVICES_REACHED.equals(t.getMessage())) {
            processFailure(msgId, deviceName, msgType, ackSent, MqttReasonCodes.PubAck.QUOTA_EXCEEDED, t);
        } else {
            processFailure(msgId, deviceName, msgType, ackSent, MqttReasonCodes.PubAck.UNSPECIFIED_ERROR, t);
        }
    }

    protected void processFailure(int msgId, String deviceName, String msgType, AtomicBoolean ackSent, MqttReasonCodes.PubAck pubAck, Throwable t) {
        log.debug("[{}][{}][{}] Failed to process device {} command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, msgType, deviceName, t);
        if (ackSent.compareAndSet(false, true)) {
            ack(msgId, pubAck);
        }
    }

    private void closeDeviceSession(String deviceName, MqttReasonCodes.Disconnect returnCode) {
        try {
            if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
                MqttTransportAdaptor adaptor = deviceSessionCtx.getPayloadAdaptor();
                int returnCodeValue = returnCode.byteValue() & 0xFF;
                Optional<MqttMessage> deviceDisconnectPublishMsg = adaptor.convertToGatewayDeviceDisconnectPublish(deviceSessionCtx, deviceName, returnCodeValue);
                deviceDisconnectPublishMsg.ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
            }
        } catch (Exception e) {
            log.trace("Failed to send device disconnect to gateway session", e);
        }
    }

}
