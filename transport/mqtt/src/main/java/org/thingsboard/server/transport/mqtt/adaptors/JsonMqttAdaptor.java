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
package org.thingsboard.server.transport.mqtt.adaptors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.kv.AttributesKVMsg;
import org.thingsboard.server.common.msg.session.*;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.transport.mqtt.MqttTopics;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Andrew Shvayka
 */
@Component("JsonMqttAdaptor")
@Slf4j
public class JsonMqttAdaptor implements MqttTransportAdaptor {

    private static final Gson GSON = new Gson();
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    @Override
    public AdaptorToSessionActorMsg convertToActorMsg(DeviceSessionCtx ctx, MsgType type, MqttMessage inbound) throws AdaptorException {
        FromDeviceMsg msg;
        switch (type) {
            case POST_TELEMETRY_REQUEST:
                msg = convertToTelemetryUploadRequest(ctx, (MqttPublishMessage) inbound);
                break;
            case POST_ATTRIBUTES_REQUEST:
                msg = convertToUpdateAttributesRequest(ctx, (MqttPublishMessage) inbound);
                break;
            case SUBSCRIBE_ATTRIBUTES_REQUEST:
                msg = new AttributesSubscribeMsg();
                break;
            case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                msg = new AttributesUnsubscribeMsg();
                break;
            case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                msg = new RpcSubscribeMsg();
                break;
            case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                msg = new RpcUnsubscribeMsg();
                break;
            case GET_ATTRIBUTES_REQUEST:
                msg = convertToGetAttributesRequest(ctx, (MqttPublishMessage) inbound);
                break;
            case TO_DEVICE_RPC_RESPONSE:
                msg = convertToRpcCommandResponse(ctx, (MqttPublishMessage) inbound);
                break;
            case TO_SERVER_RPC_REQUEST:
                msg = convertToServerRpcRequest(ctx, (MqttPublishMessage) inbound);
                break;
            default:
                log.warn("[{}] Unsupported msg type: {}!", ctx.getSessionId(), type);
                throw new AdaptorException(new IllegalArgumentException("Unsupported msg type: " + type + "!"));
        }
        return new BasicAdaptorToSessionActorMsg(ctx, msg);
    }

    @Override
    public Optional<MqttMessage> convertToAdaptorMsg(DeviceSessionCtx ctx, SessionActorToAdaptorMsg sessionMsg) throws AdaptorException {
        MqttMessage result = null;
        ToDeviceMsg msg = sessionMsg.getMsg();
        switch (msg.getMsgType()) {
            case STATUS_CODE_RESPONSE:
            case GET_ATTRIBUTES_RESPONSE:
                ResponseMsg<?> responseMsg = (ResponseMsg) msg;
                if (responseMsg.isSuccess()) {
                    MsgType requestMsgType = responseMsg.getRequestMsgType();
                    Integer requestId = responseMsg.getRequestId();
                    if (requestId >= 0) {
                        if (requestMsgType == MsgType.POST_ATTRIBUTES_REQUEST || requestMsgType == MsgType.POST_TELEMETRY_REQUEST) {
                            result = MqttTransportHandler.createMqttPubAckMsg(requestId);
                        } else if (requestMsgType == MsgType.GET_ATTRIBUTES_REQUEST) {
                            GetAttributesResponse response = (GetAttributesResponse) msg;
                            if (response.isSuccess()) {
                                result = createMqttPublishMsg(ctx,
                                        MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX + requestId,
                                        response.getData().get(), true);
                            } else {
                                throw new AdaptorException(response.getError().get());
                            }
                        }
                    }
                } else {
                    if (responseMsg.getError().isPresent()) {
                        throw new AdaptorException(responseMsg.getError().get());
                    }
                }
                break;
            case ATTRIBUTES_UPDATE_NOTIFICATION:
                AttributesUpdateNotification notification = (AttributesUpdateNotification) msg;
                result = createMqttPublishMsg(ctx, MqttTopics.DEVICE_ATTRIBUTES_TOPIC, notification.getData(), false);
                break;
            case TO_DEVICE_RPC_REQUEST:
                ToDeviceRpcRequestMsg rpcRequest = (ToDeviceRpcRequestMsg) msg;
                result = createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + rpcRequest.getRequestId(),
                        rpcRequest);
                break;
            case TO_SERVER_RPC_RESPONSE:
                ToServerRpcResponseMsg rpcResponse = (ToServerRpcResponseMsg) msg;
                result = createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC + rpcResponse.getRequestId(),
                        rpcResponse);
                break;
            case RULE_ENGINE_ERROR:
                RuleEngineErrorMsg errorMsg = (RuleEngineErrorMsg) msg;
                result = createMqttPublishMsg(ctx, "errors", JsonConverter.toErrorJson(errorMsg.getErrorMsg()));
                break;
        }
        return Optional.ofNullable(result);
    }

    private MqttPublishMessage createMqttPublishMsg(DeviceSessionCtx ctx, String topic, AttributesKVMsg msg, boolean asMap) {
        return createMqttPublishMsg(ctx, topic, JsonConverter.toJson(msg, asMap));
    }

    private MqttPublishMessage createMqttPublishMsg(DeviceSessionCtx ctx, String topic, ToDeviceRpcRequestMsg msg) {
        return createMqttPublishMsg(ctx, topic, JsonConverter.toJson(msg, false));
    }

    private MqttPublishMessage createMqttPublishMsg(DeviceSessionCtx ctx, String topic, ToServerRpcResponseMsg msg) {
        return createMqttPublishMsg(ctx, topic, JsonConverter.toJson(msg));
    }

    private MqttPublishMessage createMqttPublishMsg(DeviceSessionCtx ctx, String topic, JsonElement json) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, ctx.nextMsgId());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(GSON.toJson(json).getBytes(UTF8));
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }

    private FromDeviceMsg convertToGetAttributesRequest(DeviceSessionCtx ctx, MqttPublishMessage inbound) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            Integer requestId = Integer.valueOf(topicName.substring(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX.length()));
            String payload = inbound.payload().toString(UTF8);
            JsonElement requestBody = new JsonParser().parse(payload);
            Set<String> clientKeys = toStringSet(requestBody, "clientKeys");
            Set<String> sharedKeys = toStringSet(requestBody, "sharedKeys");
            if (clientKeys == null && sharedKeys == null) {
                return new BasicGetAttributesRequest(requestId);
            } else {
                return new BasicGetAttributesRequest(requestId, clientKeys, sharedKeys);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    private FromDeviceMsg convertToRpcCommandResponse(DeviceSessionCtx ctx, MqttPublishMessage inbound) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            Integer requestId = Integer.valueOf(topicName.substring(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC.length()));
            String payload = inbound.payload().toString(UTF8);
            return new ToDeviceRpcResponseMsg(
                    requestId,
                    payload);
        } catch (RuntimeException e) {
            log.warn("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    private Set<String> toStringSet(JsonElement requestBody, String name) {
        JsonElement element = requestBody.getAsJsonObject().get(name);
        if (element != null) {
            return new HashSet<>(Arrays.asList(element.getAsString().split(",")));
        } else {
            return null;
        }
    }

    private UpdateAttributesRequest convertToUpdateAttributesRequest(SessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload());
        try {
            return JsonConverter.convertToAttributes(new JsonParser().parse(payload), inbound.variableHeader().messageId());
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    private TelemetryUploadRequest convertToTelemetryUploadRequest(SessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload());
        try {
            return JsonConverter.convertToTelemetry(new JsonParser().parse(payload), inbound.variableHeader().messageId());
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    private FromDeviceMsg convertToServerRpcRequest(DeviceSessionCtx ctx, MqttPublishMessage inbound) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        String payload = validatePayload(ctx.getSessionId(), inbound.payload());
        try {
            Integer requestId = Integer.valueOf(topicName.substring(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC.length()));
            return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), requestId);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    public static JsonElement validateJsonPayload(SessionId sessionId, ByteBuf payloadData) throws AdaptorException {
        String payload = validatePayload(sessionId, payloadData);
        try {
            return new JsonParser().parse(payload);
        } catch (JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    public static String validatePayload(SessionId sessionId, ByteBuf payloadData) throws AdaptorException {
        try {
            String payload = payloadData.toString(UTF8);
            if (payload == null) {
                log.warn("[{}] Payload is empty!", sessionId.toUidStr());
                throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
            }
            return payload;
        } finally {
            payloadData.release();
        }
    }

}
