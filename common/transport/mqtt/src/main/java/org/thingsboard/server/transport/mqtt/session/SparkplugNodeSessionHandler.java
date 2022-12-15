/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromSparkplugResponse;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.parseTopic;

/**
 * Created by nickAS21 on 12.12.22
 */
@Slf4j
public class SparkplugNodeSessionHandler {

    private static final String DEFAULT_DEVICE_TYPE = "default";
    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";
    private static final String DEVICE_PROPERTY = "device";

    private final MqttTransportContext context;
    private final TransportService transportService;
    private final TransportDeviceInfo nodeSparkplugInfo;
    private final UUID sessionId;
    private final ConcurrentMap<String, Lock> deviceCreationLockMap;
    private final ConcurrentMap<String, SparkplugSessionCtx> devices = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ListenableFuture<SparkplugSessionCtx>> deviceFutures = new ConcurrentHashMap<>();
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;
    private final ChannelHandlerContext channel;
    private final DeviceSessionCtx deviceSessionCtx;
    private String nodeTopic;

    public SparkplugNodeSessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId, String nodeTopic) {
        this.context = deviceSessionCtx.getContext();
        this.transportService = context.getTransportService();
        this.deviceSessionCtx = deviceSessionCtx;
        this.nodeSparkplugInfo = deviceSessionCtx.getDeviceInfo();
        this.sessionId = sessionId;
        this.deviceCreationLockMap = createWeakMap();
        this.mqttQoSMap = deviceSessionCtx.getMqttQoSMap();
        this.channel = deviceSessionCtx.getChannel();
        this.nodeTopic = nodeTopic;
    }

    ConcurrentReferenceHashMap<String, Lock> createWeakMap() {
        return new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);
    }

    public String getNodeId() {
        return context.getNodeId();
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String  getNodeTopic() {
        return nodeTopic;
    }

    public int nextMsgId() {
        return deviceSessionCtx.nextMsgId();
    }

    public void deregisterSession(String deviceName) {
        SparkplugSessionCtx deviceSessionCtx = devices.remove(deviceName);
        if (deviceSessionCtx != null) {
            deregisterSession(deviceName, deviceSessionCtx);
        } else {
            log.debug("[{}] Device [{}] was already removed from the gateway session", sessionId, deviceName);
        }
    }

    private void deregisterSession(String deviceName, SparkplugSessionCtx deviceSessionCtx) {
        transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
        transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_CLOSED, null);
        log.debug("[{}] Removed device [{}] from the gateway session", sessionId, deviceName);
    }

    public void onDeviceDeleted(String deviceName) {
        deregisterSession(deviceName);
    }

    private int getMsgId(MqttPublishMessage mqttMsg) {
        return mqttMsg.variableHeader().packetId();
    }

    public void onDeviceConnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            String deviceName = parseTopic(mqttMsg.variableHeader().topicName()).getDeviceId();
            String deviceType = StringUtils.isEmpty(nodeSparkplugInfo.getDeviceType()) ? DEFAULT_DEVICE_TYPE : nodeSparkplugInfo.getDeviceType();
            processOnConnect(mqttMsg, deviceName, deviceType);
        } catch (Exception  e) {
            throw new AdaptorException(e);
        }
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

    ChannelFuture writeAndFlush(MqttMessage mqttMessage) {
        return channel.writeAndFlush(mqttMessage);
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


    private void processOnConnect(MqttPublishMessage msg, String deviceName, String deviceType) {
        log.trace("[{}] onDeviceConnect: {}", sessionId, deviceName);
        Futures.addCallback(onDeviceConnect(deviceName, deviceType), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable SparkplugSessionCtx result) {
                ack(msg);
                log.trace("[{}] onDeviceConnectOk: {}", sessionId, deviceName);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to process device connect command: {}", sessionId, deviceName, t);

            }
        }, context.getExecutor());
    }


    private ListenableFuture<SparkplugSessionCtx> onDeviceConnect(String deviceName, String deviceType) {
        SparkplugSessionCtx result = devices.get(deviceName);
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

    private ListenableFuture<SparkplugSessionCtx> getDeviceCreationFuture(String deviceName, String deviceType) {
        final SettableFuture<SparkplugSessionCtx> futureToSet = SettableFuture.create();
        ListenableFuture<SparkplugSessionCtx> future = deviceFutures.putIfAbsent(deviceName, futureToSet);
        if (future != null) {
            return future;
        }
        try {
            transportService.process(TransportProtos.GetOrCreateDeviceFromSparkplugRequestMsg.newBuilder()
                            .setDeviceName(deviceName)
                            .setDeviceType(deviceType)
                            .setSparkplugIdMSB(nodeSparkplugInfo.getDeviceId().getId().getMostSignificantBits())
                            .setSparkplugIdLSB(nodeSparkplugInfo.getDeviceId().getId().getLeastSignificantBits())
                            .build(),
                    new TransportServiceCallback<>() {
                        @Override
                        public void onSuccess(GetOrCreateDeviceFromSparkplugResponse msg) {
                            if (msg.getDeviceInfo() == null) {
                                System.out.println("DeviceInfo == null");
                            }
                            SparkplugSessionCtx nodeSparkplugSessionCtx = new SparkplugSessionCtx(SparkplugNodeSessionHandler.this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap, transportService);
                            if (devices.putIfAbsent(deviceName, nodeSparkplugSessionCtx) == null) {
                                log.trace("[{}] First got or created device [{}], type [{}] for the gateway session", sessionId, deviceName, deviceType);
                                SessionInfoProto deviceSessionInfo = nodeSparkplugSessionCtx.getSessionInfo();
                                transportService.registerAsyncSession(deviceSessionInfo, nodeSparkplugSessionCtx);
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
    }


}
