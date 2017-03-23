/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.*;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.kv.AttributesKVMsg;
import org.thingsboard.server.common.msg.session.*;
import org.thingsboard.server.common.msg.session.ex.SessionException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.transport.mqtt.MqttTopics;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 19.01.17.
 */
public class GatewayDeviceSessionCtx extends DeviceAwareSessionContext {

    private static final Gson GSON = new Gson();
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    private GatewaySessionCtx parent;
    private final MqttSessionId sessionId;
    private volatile boolean closed;
    private AtomicInteger msgIdSeq = new AtomicInteger(0);

    public GatewayDeviceSessionCtx(GatewaySessionCtx parent, Device device) {
        super(parent.getProcessor(), parent.getAuthService(), device);
        this.parent = parent;
        this.sessionId = new MqttSessionId();
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.ASYNC;
    }

    @Override
    public void onMsg(SessionActorToAdaptorMsg sessionMsg) throws SessionException {
        Optional<MqttMessage> message = getToDeviceMsg(sessionMsg);
        message.ifPresent(parent::writeAndFlush);
    }

    private Optional<MqttMessage> getToDeviceMsg(SessionActorToAdaptorMsg sessionMsg) {
        ToDeviceMsg msg = sessionMsg.getMsg();
        switch (msg.getMsgType()) {
            case STATUS_CODE_RESPONSE:
                ResponseMsg<?> responseMsg = (ResponseMsg) msg;
                if (responseMsg.isSuccess()) {
                    MsgType requestMsgType = responseMsg.getRequestMsgType();
                    Integer requestId = responseMsg.getRequestId();
                    if (requestMsgType == MsgType.POST_ATTRIBUTES_REQUEST || requestMsgType == MsgType.POST_TELEMETRY_REQUEST) {
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
        }
        return Optional.empty();
    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {

    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public long getTimeout() {
        return 0;
    }

    private MqttMessage createMqttPublishMsg(String topic, GetAttributesResponse response) {
        JsonObject result = new JsonObject();
        result.addProperty("id", response.getRequestId());
        result.addProperty("device", device.getName());
        if (response.getData().isPresent()) {
            AttributesKVMsg msg = response.getData().get();
            if (msg.getClientAttributes() != null) {
                msg.getClientAttributes().forEach(v -> addValueToJson(result, "value", v));
            }
            if (msg.getSharedAttributes() != null) {
                msg.getSharedAttributes().forEach(v -> addValueToJson(result, "value", v));
            }
        }
        return createMqttPublishMsg(topic, result);
    }

    private void addValueToJson(JsonObject json, String name, KvEntry entry) {
        switch (entry.getDataType()) {
            case BOOLEAN:
                json.addProperty(name, entry.getBooleanValue().get());
                break;
            case STRING:
                json.addProperty(name, entry.getStrValue().get());
                break;
            case DOUBLE:
                json.addProperty(name, entry.getDoubleValue().get());
                break;
            case LONG:
                json.addProperty(name, entry.getLongValue().get());
                break;
        }
    }

    private MqttMessage createMqttPublishMsg(String topic, AttributesKVMsg data) {
        JsonObject result = new JsonObject();
        result.addProperty("device", device.getName());
        result.add("data", JsonConverter.toJson(data, false));
        return createMqttPublishMsg(topic, result);
    }

    private MqttMessage createMqttPublishMsg(String topic, ToDeviceRpcRequestMsg data) {
        JsonObject result = new JsonObject();
        result.addProperty("device", device.getName());
        result.add("data", JsonConverter.toJson(data, true));
        return createMqttPublishMsg(topic, result);
    }

    private MqttPublishMessage createMqttPublishMsg(String topic, JsonElement json) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, msgIdSeq.incrementAndGet());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(GSON.toJson(json).getBytes(UTF8));
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }

}
