/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.mqtt.session;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.xml.internal.bind.v2.TODO;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.core.AttributesUpdateNotification;
import org.thingsboard.server.common.msg.core.GetAttributesResponse;
import org.thingsboard.server.common.msg.core.ResponseMsg;
import org.thingsboard.server.common.msg.core.ToDeviceRpcRequestMsg;
import org.thingsboard.server.common.msg.kv.AttributesKVMsg;
import org.thingsboard.server.common.msg.session.SessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.mqtt.MqttTopics;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewayDeviceSessionCtx extends MqttDeviceAwareSessionContext implements SessionMsgListener {

    private static final Gson GSON = new Gson();
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    private final GatewaySessionCtx parent;
    private final UUID sessionId;
    private final SessionInfoProto sessionInfo;
    private volatile boolean closed;
    private AtomicInteger msgIdSeq = new AtomicInteger(0);

    public GatewayDeviceSessionCtx(GatewaySessionCtx parent, DeviceInfoProto deviceInfo, ConcurrentMap<String, Integer> mqttQoSMap) {
        super(mqttQoSMap);
        this.parent = parent;
        this.sessionId = UUID.randomUUID();
        this.sessionInfo = SessionInfoProto.newBuilder()
                .setNodeId(parent.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(deviceInfo.getDeviceIdMSB())
                .setDeviceIdLSB(deviceInfo.getDeviceIdLSB())
                .setTenantIdMSB(deviceInfo.getTenantIdMSB())
                .setTenantIdLSB(deviceInfo.getTenantIdLSB())
                .build();
        setDeviceInfo(deviceInfo);
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    private Optional<MqttMessage> getToDeviceMsg(SessionActorToAdaptorMsg sessionMsg) {
        ToDeviceMsg msg = sessionMsg.getMsg();
        switch (msg.getSessionMsgType()) {
            case STATUS_CODE_RESPONSE:
                ResponseMsg<?> responseMsg = (ResponseMsg) msg;
                if (responseMsg.isSuccess()) {
                    SessionMsgType requestMsgType = responseMsg.getRequestMsgType();
                    Integer requestId = responseMsg.getRequestId();
                    if (requestId >= 0 && (requestMsgType == SessionMsgType.POST_ATTRIBUTES_REQUEST || requestMsgType == SessionMsgType.POST_TELEMETRY_REQUEST)) {
                        return Optional.of(MqttTransportHandler.createMqttPubAckMsg(requestId));
                    }
                }
                break;
            case GET_ATTRIBUTES_RESPONSE:
                GetAttributesResponse response = (GetAttributesResponse) msg;
                if (response.isSuccess()) {
                    return Optional.of(createMqttPublishMsg(MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, response));
                } else {
                    //TODO: push error handling to the gateway
                }
                break;
            case ATTRIBUTES_UPDATE_NOTIFICATION:
                AttributesUpdateNotification notification = (AttributesUpdateNotification) msg;
                return Optional.of(createMqttPublishMsg(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, notification.getData()));
            case TO_DEVICE_RPC_REQUEST:
                ToDeviceRpcRequestMsg rpcRequest = (ToDeviceRpcRequestMsg) msg;
                return Optional.of(createMqttPublishMsg(MqttTopics.GATEWAY_RPC_TOPIC, rpcRequest));
            default:
                break;
        }
        return Optional.empty();
    }

    private MqttMessage createMqttPublishMsg(String topic, GetAttributesResponse response) {
        JsonObject result = new JsonObject();
        result.addProperty("id", response.getRequestId());
//        result.addProperty(DEVICE_PROPERTY, device.getName());
        Optional<AttributesKVMsg> responseData = response.getData();
        if (responseData.isPresent()) {
            AttributesKVMsg msg = responseData.get();
            if (msg.getClientAttributes() != null) {
                addValues(result, msg.getClientAttributes());
            }
            if (msg.getSharedAttributes() != null) {
                addValues(result, msg.getSharedAttributes());
            }
        }
        return createMqttPublishMsg(topic, result);
    }

    private void addValues(JsonObject result, List<AttributeKvEntry> kvList) {
        if (kvList.size() == 1) {
            addValueToJson(result, "value", kvList.get(0));
        } else {
            JsonObject values;
            if (result.has("values")) {
                values = result.get("values").getAsJsonObject();
            } else {
                values = new JsonObject();
                result.add("values", values);
            }
            kvList.forEach(value -> addValueToJson(values, value.getKey(), value));
        }
    }

    private void addValueToJson(JsonObject json, String name, KvEntry entry) {
        switch (entry.getDataType()) {
            case BOOLEAN:
                entry.getBooleanValue().ifPresent(aBoolean -> json.addProperty(name, aBoolean));
                break;
            case STRING:
                entry.getStrValue().ifPresent(aString -> json.addProperty(name, aString));
                break;
            case DOUBLE:
                entry.getDoubleValue().ifPresent(aDouble -> json.addProperty(name, aDouble));
                break;
            case LONG:
                entry.getLongValue().ifPresent(aLong -> json.addProperty(name, aLong));
                break;
        }
    }

    private MqttMessage createMqttPublishMsg(String topic, AttributesKVMsg data) {
        JsonObject result = new JsonObject();
//        result.addProperty(DEVICE_PROPERTY, device.getName());
        result.add("data", JsonConverter.toJson(data, false));
        return createMqttPublishMsg(topic, result);
    }

    private MqttMessage createMqttPublishMsg(String topic, ToDeviceRpcRequestMsg data) {
        JsonObject result = new JsonObject();
//        result.addProperty(DEVICE_PROPERTY, device.getName());
        result.add("data", JsonConverter.toJson(data, true));
        return createMqttPublishMsg(topic, result);
    }

    private MqttPublishMessage createMqttPublishMsg(String topic, JsonElement json) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, getQoSForTopic(topic), false, 0);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, msgIdSeq.incrementAndGet());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(GSON.toJson(json).getBytes(UTF8));
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }

    SessionInfoProto getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg response) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, response).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg notification) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), notification).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
        parent.deregisterSession(getDeviceInfo().getDeviceName());
    }

    @Override
    public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg toDeviceRequest) {

    }

    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        TODO
    }
}
